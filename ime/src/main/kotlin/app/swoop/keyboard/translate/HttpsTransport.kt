package app.swoop.keyboard.translate

import app.kestrel.core.translate.Http
import app.kestrel.core.translate.HttpReply
import app.kestrel.core.translate.SafeReason
import app.kestrel.core.translate.TransportException
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * The code in the keyboard that opens a connection for a translation ([ConnectionProbe] is the other one, for the Diagnostics test).
 * HttpURLConnection, HTTPS to the two hosts of [TranslateGuard] only, timeouts for a flaky 5 Mbit phone hotspot, redirects NOT followed
 * (Google's picture-puzzle page is one; a redirect to another host must never be followed), no cache, a bounded answer, a User-Agent that
 * names nothing personal. It never logs and never puts the text (or an exception message, which could hold the address with the text in
 * it) into an error: failures carry a [SafeReason] (class name, an errno name, a known phrase) and a [TransportException.Kind] that says
 * WHICH step broke (2026-11-18: the owner saw only "SocketException" and nobody could tell what it meant).
 *
 * One quick automatic retry (after [RETRY_MS]) when a connection was made and then dropped ([TransportException.Kind.DROPPED]): the
 * flaky phone hotspot resets connections now and then, and a second try on a fresh connection usually works.
 *
 * Blocking: call from a background thread only ([Translator] does).
 */
class HttpsTransport internal constructor(
    /** Tests only: maps the https address to a local fake server AFTER the allowlist check. Null in the app. */
    private val rewrite: ((String) -> String)?,
    private val connectMs: Int = CONNECT_MS,
    private val readMs: Int = READ_MS,
    /** Wait before the one automatic retry of a dropped connection; 0 = no retry (tests). */
    private val retryMs: Int = RETRY_MS
) : Http {
    constructor() : this(null)

    @Volatile private var current: HttpURLConnection? = null
    /** Counts aborts: a retry that waited through an [abort] is stale and is not made. */
    @Volatile private var abortGen = 0
    /** How many connections were opened (tests: the retry makes two). */
    @Volatile internal var attempts = 0

    override fun call(url: String, form: String?): HttpReply {
        if (!TranslateGuard.allows(url)) throw TransportException(TransportException.Kind.OTHER, "address not allowed")
        val gen = abortGen
        try {
            return once(url, form)
        } catch (e: TransportException) {
            if (e.kind != TransportException.Kind.DROPPED || retryMs <= 0) throw e
            var waited = 0
            while (waited < retryMs) { // wait in small steps so that an abort (cancel, newer request) ends the wait at once
                try { Thread.sleep(50) } catch (_: InterruptedException) { throw e }
                waited += 50
                if (abortGen != gen) throw e
            }
            if (abortGen != gen) throw e
            return once(url, form)
        }
    }

    private fun once(url: String, form: String?): HttpReply {
        val target = rewrite?.invoke(url) ?: url
        attempts++
        var c: HttpURLConnection? = null
        try {
            c = URL(target).openConnection() as HttpURLConnection
            current = c
            c.connectTimeout = connectMs
            c.readTimeout = readMs
            c.instanceFollowRedirects = false
            c.useCaches = false
            c.defaultUseCaches = false
            c.setRequestProperty("User-Agent", USER_AGENT)
            c.setRequestProperty("Accept", "application/json")
            c.setRequestProperty("Connection", "close")
            if (form != null) {
                val b = form.toByteArray(Charsets.UTF_8)
                c.requestMethod = "POST"
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                c.setFixedLengthStreamingMode(b.size)
                c.outputStream.use { it.write(b) }
            }
            val code = c.responseCode
            val location = c.getHeaderField("Location")
            val stream: InputStream? = if (code >= 400) c.errorStream else c.inputStream
            val body = if (stream == null) "" else read(stream)
            return HttpReply(code, location, body)
        } catch (e: TransportException) {
            throw e
        } catch (e: IOException) {
            throw classify(e)
        } finally {
            current = null
            try { c?.disconnect() } catch (e: RuntimeException) { }
        }
    }

    /**
     * WHICH step broke, from the exception alone. Only a [SafeReason] (class name, an errno name, a known phrase) goes into the error: the
     * message of an IOException can hold the address, and the address holds the text.
     */
    internal fun classify(e: IOException): TransportException {
        val why = SafeReason.of(e.javaClass.simpleName, e.message)
        val low = (e.message ?: "").lowercase()
        val kind = when (e) {
            is UnknownHostException, is NoRouteToHostException -> TransportException.Kind.NO_INTERNET
            // refused / unreachable / timed out while connecting = no internet; EPERM / EACCES = Android refused the socket
            is ConnectException -> if (blocked(e)) TransportException.Kind.BLOCKED else if (SafeReason.errnoOf(e.message) == "ECONNREFUSED" || low.contains("connection refused")) TransportException.Kind.REFUSED else TransportException.Kind.NO_INTERNET
            is SocketTimeoutException -> TransportException.Kind.TIMEOUT
            is SSLException -> if (droppedNotRejected(low)) TransportException.Kind.DROPPED else TransportException.Kind.SECURE
            // a plain SocketException: the socket was refused (EPERM), or a connection that was made got reset / aborted / closed
            is SocketException -> if (blocked(e)) TransportException.Kind.BLOCKED else TransportException.Kind.DROPPED
            is EOFException -> TransportException.Kind.DROPPED
            else -> if (low.contains("unexpected end of stream") || low.contains("end of file from server")) TransportException.Kind.DROPPED else TransportException.Kind.OTHER
        }
        return TransportException(kind, why)
    }

    override fun abort() {
        abortGen++
        try { current?.disconnect() } catch (e: RuntimeException) { }
    }

    private fun blocked(e: IOException): Boolean {
        val n = SafeReason.errnoOf(e.message)
        return n == "EPERM" || n == "EACCES"
    }

    /** An SSL failure that is the connection being cut (reset, aborted, closed by the peer), not a refusal of the certificate or protocol. */
    private fun droppedNotRejected(m: String): Boolean =
        m.contains("connection reset") || m.contains("abort") || m.contains("i/o error during system call") || m.contains("broken pipe") ||
            m.contains("closed by peer") || m.contains("connection closed") || m.contains("unexpected end") || m.contains("socket closed")

    private fun read(stream: InputStream): String {
        val deadline = System.currentTimeMillis() + TOTAL_MS
        stream.use { i ->
            val bo = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            while (true) {
                val n = i.read(buf)
                if (n < 0) break
                if (bo.size() + n > MAX_ANSWER) throw TransportException(TransportException.Kind.TOO_BIG)
                bo.write(buf, 0, n)
                if (System.currentTimeMillis() > deadline) throw TransportException(TransportException.Kind.TIMEOUT)
            }
            return String(bo.toByteArray(), Charsets.UTF_8)
        }
    }

    companion object {
        const val CONNECT_MS = 8000
        const val READ_MS = 15000
        const val RETRY_MS = 600
        const val TOTAL_MS = 30_000L
        const val MAX_ANSWER = 256 * 1024
        const val USER_AGENT = "Swoop"
    }
}
