package app.swoop.keyboard.translate

import app.kestrel.core.translate.Http
import app.kestrel.core.translate.HttpReply
import app.kestrel.core.translate.TransportException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * The ONLY code in the keyboard that opens a connection. HttpURLConnection, HTTPS to the two hosts of [TranslateGuard] only,
 * timeouts for a flaky 5 Mbit phone hotspot, redirects NOT followed (Google's picture-puzzle page is one; a redirect to another host
 * must never be followed), no cache, a bounded answer, a User-Agent that names nothing personal. It never logs and never puts the
 * text (or an exception message, which could hold the address with the text in it) into an error.
 *
 * Blocking: call from a background thread only ([Translator] does).
 */
class HttpsTransport internal constructor(
    /** Tests only: maps the https address to a local fake server AFTER the allowlist check. Null in the app. */
    private val rewrite: ((String) -> String)?,
    private val connectMs: Int = CONNECT_MS,
    private val readMs: Int = READ_MS
) : Http {
    constructor() : this(null)

    @Volatile private var current: HttpURLConnection? = null

    override fun call(url: String, form: String?): HttpReply {
        if (!TranslateGuard.allows(url)) throw TransportException(TransportException.Kind.OTHER, "address not allowed")
        val target = rewrite?.invoke(url) ?: url
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
        } catch (e: UnknownHostException) {
            throw TransportException(TransportException.Kind.NO_INTERNET)
        } catch (e: NoRouteToHostException) {
            throw TransportException(TransportException.Kind.NO_INTERNET)
        } catch (e: ConnectException) {
            throw TransportException(TransportException.Kind.NO_INTERNET)
        } catch (e: SocketTimeoutException) {
            throw TransportException(TransportException.Kind.TIMEOUT)
        } catch (e: IOException) {
            // the class name only: the message of an IOException can hold the address, and the address holds the text
            throw TransportException(TransportException.Kind.OTHER, e.javaClass.simpleName)
        } finally {
            current = null
            try { c?.disconnect() } catch (e: RuntimeException) { }
        }
    }

    override fun abort() {
        try { current?.disconnect() } catch (e: RuntimeException) { }
    }

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
        const val TOTAL_MS = 30_000L
        const val MAX_ANSWER = 256 * 1024
        const val USER_AGENT = "Swoop"
    }
}
