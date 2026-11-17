package app.swoop.keyboard.translate

import app.kestrel.core.translate.ErrorCode
import app.kestrel.core.translate.Http
import app.kestrel.core.translate.HttpReply
import app.kestrel.core.translate.Translation
import com.sun.net.httpserver.HttpServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TranslatorTest {
    private fun mm(t: String) = "{\"responseData\":{\"translatedText\":\"$t\"},\"responseStatus\":200}"

    private class Gate(val reply: (String, String?) -> HttpReply) : Http {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        @Volatile var blockFirst = false
        val calls = AtomicInteger()
        @Volatile var aborts = 0
        override fun call(url: String, form: String?): HttpReply {
            if (calls.incrementAndGet() == 1 && blockFirst) { entered.countDown(); release.await(5, TimeUnit.SECONDS) }
            return reply(url, form)
        }
        override fun abort() { aborts++ }
    }

    @Test fun answerArrivesOnTheWorkerThreadNotTheCaller() {
        val t = Translator(Gate { _, _ -> HttpReply(200, null, mm("Hola")) }, null)
        val q = LinkedBlockingQueue<Pair<String, String>>()
        val me = Thread.currentThread().name
        t.translate("Hello") { r -> q.add(Thread.currentThread().name to r.text) }
        val (thread, text) = q.poll(5, TimeUnit.SECONDS)!!
        assertEquals("Hola", text)
        assertEquals("swoop-translate", thread)
        assertNotEquals(me, thread)
    }

    @Test fun cancelDropsTheAnswerAndAbortsTheWire() {
        val g = Gate { _, _ -> HttpReply(200, null, mm("Hola")) }.also { it.blockFirst = true }
        val t = Translator(g, null)
        val q = LinkedBlockingQueue<String>()
        t.translate("Hello") { q.add("first:" + it.text) }
        assertTrue(g.entered.await(5, TimeUnit.SECONDS))
        t.cancel()
        g.release.countDown()
        t.translate("Hello again") { q.add("second:" + it.text) }
        assertEquals("second:Hola", q.poll(5, TimeUnit.SECONDS))
        assertTrue(g.aborts >= 2)
        assertTrue(q.isEmpty())
    }

    @Test fun aNewerRequestMakesTheOlderStale() {
        val g = Gate { _, _ -> HttpReply(200, null, mm("Hola")) }.also { it.blockFirst = true }
        val t = Translator(g, null)
        val q = LinkedBlockingQueue<String>()
        val a = t.translate("one") { q.add("a") }
        assertTrue(g.entered.await(5, TimeUnit.SECONDS))
        val b = t.translate("two") { q.add("b") }
        assertTrue(b > a)
        g.release.countDown()
        assertEquals("b", q.poll(5, TimeUnit.SECONDS))
        Thread.sleep(100)
        assertTrue(q.isEmpty())
    }

    @Test fun aThrowingCallbackDoesNotKillTheWorker() {
        val t = Translator(Gate { _, _ -> HttpReply(200, null, mm("Hola")) }, null)
        t.translate("x y") { throw IllegalStateException("caller bug") }
        val q = LinkedBlockingQueue<String>()
        t.translate("x z") { q.add(it.text) }
        assertEquals("Hola", q.poll(5, TimeUnit.SECONDS))
    }

    @Test fun failuresComeBackAsResultsAndTheLogHasNoWords() {
        val log = ArrayList<String>()
        val t = Translator(Gate { _, _ -> HttpReply(429, null, "") }, { s, ms, r -> synchronized(log) { log.add("$s $r") } })
        val q = LinkedBlockingQueue<ErrorCode>()
        t.translate("My secret sentence") { q.add(it.code) }
        assertEquals(ErrorCode.BUSY, q.poll(5, TimeUnit.SECONDS))
        synchronized(log) {
            assertEquals(2, log.size)
            assertFalse(log.joinToString().contains("secret"))
        }
    }

    @Test fun toEnglishIsAvailable() {
        val g = Gate { u, _ -> HttpReply(200, null, mm("Hello")) }
        val t = Translator(g, null)
        val q = LinkedBlockingQueue<String>()
        t.translate("Hola", Translation.Way.TO_ENGLISH) { q.add(it.text) }
        assertEquals("Hello", q.poll(5, TimeUnit.SECONDS))
    }

    @Test fun endToEndThroughTheRealTransportToAFakeServer() {
        val server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
        val hits = LinkedBlockingQueue<String>()
        server.createContext("/get") { ex ->
            hits.add(ex.requestURI.rawQuery)
            val b = mm("Voy a alquilar un coche").toByteArray()
            ex.sendResponseHeaders(200, b.size.toLong()); ex.responseBody.use { it.write(b) }
        }
        server.start()
        try {
            val port = server.address.port
            val t = Translator(HttpsTransport({ u -> "http://127.0.0.1:$port/" + u.removePrefix("https://").substringAfter('/') }), null)
            val q = LinkedBlockingQueue<String>()
            t.translate("I will rent a car") { q.add(it.text) }
            assertEquals("Voy a rentar un carro", q.poll(5, TimeUnit.SECONDS))
            assertEquals("q=I+will+rent+a+car&langpair=en%7Ces-MX&onlyprivate=1&mt=1", hits.poll())
        } finally {
            server.stop(0)
        }
    }
}
