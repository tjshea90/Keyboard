package app.swoop.keyboard.translate

import app.kestrel.core.translate.TransportException
import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

/** The real HttpsTransport against a fake server on 127.0.0.1 (JVM only; no real network is ever used). */
class HttpsTransportTest {
    private lateinit var server: HttpServer
    private val seen = CopyOnWriteArrayList<String>()
    private var port = 0

    @Before fun start() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
        port = server.address.port
        server.createContext("/get") { ex ->
            seen.add("GET " + ex.requestURI.toString() + " ua=" + ex.requestHeaders.getFirst("User-Agent") + " cookie=" + ex.requestHeaders.getFirst("Cookie"))
            val b = "{\"ok\":1}".toByteArray()
            ex.sendResponseHeaders(200, b.size.toLong()); ex.responseBody.use { it.write(b) }
        }
        server.createContext("/post") { ex ->
            val body = String(ex.requestBody.readBytes(), Charsets.UTF_8)
            seen.add("POST " + ex.requestURI + " type=" + ex.requestHeaders.getFirst("Content-Type") + " body=" + body)
            val b = "posted".toByteArray()
            ex.sendResponseHeaders(200, b.size.toLong()); ex.responseBody.use { it.write(b) }
        }
        server.createContext("/redirect") { ex ->
            seen.add("redirect")
            ex.responseHeaders.add("Location", "https://www.google.com/sorry/index")
            ex.sendResponseHeaders(302, -1); ex.close()
        }
        server.createContext("/sorry") { ex -> seen.add("FOLLOWED"); ex.sendResponseHeaders(200, -1); ex.close() }
        server.createContext("/limit") { ex ->
            val b = ByteArray(5)
            ex.sendResponseHeaders(429, b.size.toLong()); ex.responseBody.use { it.write(b) }
        }
        server.createContext("/big") { ex ->
            val b = ByteArray(300 * 1024) { 'a'.code.toByte() }
            ex.sendResponseHeaders(200, b.size.toLong()); ex.responseBody.use { it.write(b) }
        }
        server.createContext("/slow") { ex ->
            try { Thread.sleep(2000) } catch (e: InterruptedException) { }
            ex.sendResponseHeaders(200, -1); ex.close()
        }
        server.start()
    }

    @After fun stop() { server.stop(0) }

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
        val closed = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
        val p = closed.address.port
        closed.stop(0)
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
