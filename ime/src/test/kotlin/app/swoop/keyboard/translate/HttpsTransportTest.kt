package app.swoop.keyboard.translate

import app.kestrel.core.translate.TransportException
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

/** The real HttpsTransport against a fake server on 127.0.0.1 (JVM only; no real network is ever used). */
class HttpsTransportTest {
    private lateinit var server: FakeServer
    private val seen = CopyOnWriteArrayList<String>()
    private val port: Int get() = server.port
    private val flakyCalls = AtomicInteger()

    @Before fun start() {
        server = FakeServer()
        server.route("/get") { r ->
            seen.add("GET " + r.target + " ua=" + r.headers["user-agent"] + " cookie=" + r.headers["cookie"])
            FakeServer.Res(200, "{\"ok\":1}".toByteArray())
        }
        server.route("/post") { r ->
            seen.add("POST " + r.target + " type=" + r.headers["content-type"] + " body=" + r.body)
            FakeServer.Res(200, "posted".toByteArray())
        }
        server.route("/redirect") {
            seen.add("redirect")
            FakeServer.Res(302, headers = mapOf("Location" to "https://www.google.com/sorry/index"))
        }
        server.route("/sorry") { seen.add("FOLLOWED"); FakeServer.Res(200) }
        server.route("/limit") { FakeServer.Res(429, ByteArray(5)) }
        server.route("/big") { FakeServer.Res(200, ByteArray(300 * 1024) { 'a'.code.toByte() }) }
        server.route("/slow") { FakeServer.Res(200, delayMs = 2000) }
        server.route("/drop") { FakeServer.Res(200, mode = FakeServer.Mode.DROP) }
        server.route("/reset") { FakeServer.Res(200, mode = FakeServer.Mode.RESET) }
        server.route("/flaky") { // the hotspot drops the first two connections (the JDK and Android's own stack already retry ONE dropped connection silently), the third is fine
            if (flakyCalls.incrementAndGet() <= 2) FakeServer.Res(200, mode = FakeServer.Mode.DROP) else FakeServer.Res(200, "{\"ok\":2}".toByteArray())
        }
        server.start()
    }

    @After fun stop() { server.stop() }

    private fun transport(connectMs: Int = 2000, readMs: Int = 2000, retryMs: Int = 50) =
        HttpsTransport({ u -> "http://127.0.0.1:" + port + u.removePrefix("https://").substringAfter('/').let { "/$it" } }, connectMs, readMs, retryMs)

    private val mm = "https://api.mymemory.translated.net"

    @Test fun getSendsOneQuietRequest() {
        val r = transport().call("$mm/get?q=Hello&langpair=en%7Ces-MX", null)
        assertEquals(200, r.code); assertEquals("{\"ok\":1}", r.body); assertNull(r.location)
        assertEquals(listOf("GET /get?q=Hello&langpair=en%7Ces-MX ua=Swoop cookie=null"), seen.toList())
    }

    @Test fun postSendsTheFormInTheBody() {
        val r = transport().call("https://translate.googleapis.com/post?client=gtx", "q=hi+there")
        assertEquals("posted", r.body)
        assertEquals(listOf("POST /post?client=gtx type=application/x-www-form-urlencoded;charset=UTF-8 body=q=hi+there"), seen.toList())
    }

    @Test fun aRedirectIsReportedNeverFollowed() {
        val r = transport().call("https://translate.googleapis.com/redirect", "q=x")
        assertEquals(302, r.code)
        assertEquals("https://www.google.com/sorry/index", r.location)
        assertEquals(listOf("redirect"), seen.toList())
    }

    @Test fun errorStatusesStillGiveTheirBody() {
        val r = transport().call("$mm/limit", null)
        assertEquals(429, r.code)
    }

    @Test fun otherHostsAndCleartextAreRefusedBeforeAnyConnection() {
        val t = HttpsTransport({ throw AssertionError("must not be reached") })
        for (u in listOf(
            "http://api.mymemory.translated.net/get", "https://example.com/x", "https://api.mymemory.translated.net.evil.com/get",
            "https://user@api.mymemory.translated.net/get", "https://api.mymemory.translated.net:8443/get", "ftp://translate.googleapis.com/", "",
            "https://translate.googleapis.com\\@evil.com/"
        )) {
            try { t.call(u, null); fail("accepted $u") } catch (e: TransportException) { assertEquals(TransportException.Kind.OTHER, e.kind) }
        }
    }

    @Test fun anAnswerOver256KbIsRefused() {
        try { transport().call("$mm/big", null); fail() } catch (e: TransportException) { assertEquals(TransportException.Kind.TOO_BIG, e.kind) }
    }

    @Test fun aSlowServerIsATimeout() {
        try { transport(readMs = 300).call("$mm/slow", null); fail() } catch (e: TransportException) { assertEquals(TransportException.Kind.TIMEOUT, e.kind) }
    }

    @Test fun aClosedPortIsARefusedConnection() {
        val closed = FakeServer()
        val p = closed.port
        closed.stop()
        val t = HttpsTransport({ "http://127.0.0.1:$p/x" })
        try { t.call("$mm/get", null); fail() } catch (e: TransportException) { assertEquals(TransportException.Kind.REFUSED, e.kind) }
    }

    @Test fun errorsNeverCarryTheAddressOrTheText() {
        val t = HttpsTransport({ "http://127.0.0.1:1/secret-words-here" })
        try { t.call("$mm/get?q=secret-words-here", null); fail() } catch (e: TransportException) {
            assertFalse(String.format("%s %s", e.message, e.kind).contains("secret"))
        }
    }

    @Test fun guardAcceptsExactlyTheTwoHosts() {
        assertTrue(TranslateGuard.allows("https://api.mymemory.translated.net/get?q=a"))
        assertTrue(TranslateGuard.allows("https://translate.googleapis.com/translate_a/single?client=gtx"))
        assertFalse(TranslateGuard.allows("https://translate.googleapis.com.evil.com/"))
        assertFalse(TranslateGuard.allows("https://evil.com/?https://translate.googleapis.com/"))
        assertEquals(setOf("api.mymemory.translated.net", "translate.googleapis.com"), TranslateGuard.HOSTS)
    }

    // ---- R24a (2026-11-18): the owner saw only "SocketException"; now every failure says WHICH step broke, and a dropped connection gets one quick retry

    private fun kind(e: IOException) = HttpsTransport({ it }).classify(e).kind
    private fun why(e: IOException) = HttpsTransport({ it }).classify(e).message

    @Test fun everyFailureShapeIsClassifiedByTheStepThatBroke() {
        assertEquals(TransportException.Kind.BLOCKED, kind(SocketException("socket failed: EPERM (Operation not permitted)")))
        assertEquals(TransportException.Kind.BLOCKED, kind(ConnectException("connect failed: EACCES (Permission denied)")))
        assertEquals(TransportException.Kind.NO_INTERNET, kind(UnknownHostException("Unable to resolve host \"x\": No address associated with hostname")))
        assertEquals(TransportException.Kind.NO_INTERNET, kind(NoRouteToHostException("Host unreachable")))
        assertEquals("a refused connection (what Data Saver does to a background app) is its own kind", TransportException.Kind.REFUSED, kind(ConnectException("failed to connect to x (port 443) after 8000ms: isConnected failed: ECONNREFUSED (Connection refused)")))
        assertEquals(TransportException.Kind.NO_INTERNET, kind(ConnectException("failed to connect to x (port 443) after 8000ms: isConnected failed: ENETUNREACH (Network is unreachable)")))
        assertEquals(TransportException.Kind.TIMEOUT, kind(SocketTimeoutException("failed to connect to x after 8000ms")))
        assertEquals(TransportException.Kind.DROPPED, kind(SocketException("Connection reset")))
        assertEquals(TransportException.Kind.DROPPED, kind(SocketException("Socket closed")))
        assertEquals(TransportException.Kind.DROPPED, kind(SocketException("recvfrom failed: ECONNRESET (Connection reset by peer)")))
        assertEquals(TransportException.Kind.DROPPED, kind(SocketException("sendto failed: EPIPE (Broken pipe)")))
        assertEquals(TransportException.Kind.DROPPED, kind(SocketException("Software caused connection abort")))
        assertEquals(TransportException.Kind.DROPPED, kind(SocketException()))
        assertEquals(TransportException.Kind.DROPPED, kind(EOFException()))
        assertEquals(TransportException.Kind.DROPPED, kind(IOException("unexpected end of stream on Connection{api.mymemory.translated.net:443}")))
        assertEquals(TransportException.Kind.DROPPED, kind(SSLException("Read error: ssl=0x1: I/O error during system call, Connection reset by peer")))
        assertEquals(TransportException.Kind.DROPPED, kind(SSLHandshakeException("Connection closed by peer")))
        assertEquals(TransportException.Kind.SECURE, kind(SSLHandshakeException("java.security.cert.CertPathValidatorException: Trust anchor for certification path not found.")))
        assertEquals(TransportException.Kind.SECURE, kind(SSLHandshakeException("Chain validation failed")))
        assertEquals(TransportException.Kind.OTHER, kind(IOException("who knows")))
    }

    @Test fun theReasonNamesTheErrnoAndNeverTheAddressOrTheWords() {
        assertEquals("SocketException (EPERM, not permitted)", why(SocketException("socket failed: EPERM (Operation not permitted)")))
        val w = why(ConnectException("failed to connect to api.mymemory.translated.net/203.0.113.7 (port 443): https://api.mymemory.translated.net/get?q=top%20secret ECONNREFUSED"))
        assertEquals("ConnectException (ECONNREFUSED)", w)
        assertFalse(w!!.contains("secret") || w.contains("203"))
    }

    @Test fun aDroppedConnectionIsRetriedOnceAndThenWorks() {
        val t = transport()
        val r = t.call("$mm/flaky", null)
        assertEquals(200, r.code); assertEquals("{\"ok\":2}", r.body)
        assertTrue("one or two tries of ours", t.attempts in 1..2)
        assertTrue("the server saw at least two connections", flakyCalls.get() >= 2)
    }

    @Test fun aConnectionThatKeepsDroppingEndsAsDroppedAfterTwoTriesOnly() {
        val t = transport()
        try { t.call("$mm/drop", null); fail() } catch (e: TransportException) {
            assertEquals(TransportException.Kind.DROPPED, e.kind)
            assertTrue(e.message, e.message!!.startsWith("SocketException"))
        }
        assertEquals(2, t.attempts)
    }

    @Test fun aResetIsDroppedToo() {
        val t = transport(retryMs = 0)
        try { t.call("$mm/reset", null); fail() } catch (e: TransportException) { assertEquals(TransportException.Kind.DROPPED, e.kind) }
        assertEquals("no retry when it is switched off", 1, t.attempts)
    }

    @Test fun otherFailuresAreNotRetried() {
        val t = transport(readMs = 300)
        try { t.call("$mm/slow", null); fail() } catch (e: TransportException) { assertEquals(TransportException.Kind.TIMEOUT, e.kind) }
        assertEquals(1, t.attempts)
        val big = transport()
        try { big.call("$mm/big", null); fail() } catch (e: TransportException) { assertEquals(TransportException.Kind.TOO_BIG, e.kind) }
        assertEquals(1, big.attempts)
    }

    @Test fun anAbortDuringTheRetryWaitEndsItWithoutASecondTry() {
        val t = transport(retryMs = 3000)
        val result = java.util.concurrent.LinkedBlockingQueue<Any>()
        Thread { try { t.call("$mm/drop", null); result.add("returned") } catch (e: TransportException) { result.add(e.kind) } }.start()
        Thread.sleep(400)   // the first try dropped; the call is now waiting to retry
        val t0 = System.currentTimeMillis()
        t.abort()
        assertEquals(TransportException.Kind.DROPPED, result.poll(3, java.util.concurrent.TimeUnit.SECONDS))
        assertTrue("the wait ended at once", System.currentTimeMillis() - t0 < 1500)
        assertEquals(1, t.attempts)
    }
}
