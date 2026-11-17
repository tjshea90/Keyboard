package app.swoop.keyboard.translate

import app.kestrel.core.translate.TransportException
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
        server.start()
    }

    @After fun stop() { server.stop() }

    private fun transport(connectMs: Int = 2000, readMs: Int = 2000) =
        HttpsTransport({ u -> "http://127.0.0.1:" + port + u.removePrefix("https://").substringAfter('/').let { "/$it" } }, connectMs, readMs)

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

    @Test fun noServerIsNoInternet() {
        val closed = FakeServer()
        val p = closed.port
        closed.stop()
        val t = HttpsTransport({ "http://127.0.0.1:$p/x" })
        try { t.call("$mm/get", null); fail() } catch (e: TransportException) { assertEquals(TransportException.Kind.NO_INTERNET, e.kind) }
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
}
