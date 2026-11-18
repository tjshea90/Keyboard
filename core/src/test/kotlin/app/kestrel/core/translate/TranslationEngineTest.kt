package app.kestrel.core.translate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationEngineTest {
    /** A pretend internet: records what was asked, answers from rules. Nothing here touches a socket. */
    private class Fake(val onCall: (String, String?) -> HttpReply) : Http {
        val calls = ArrayList<Pair<String, String?>>()
        var aborted = false
        override fun call(url: String, form: String?): HttpReply {
            calls.add(url to form)
            return onCall(url, form)
        }
        override fun abort() { aborted = true }
    }

    private fun mmBody(t: String) = """{"responseData":{"translatedText":"$t"},"quotaFinished":false,"responseStatus":200}"""
    private fun gBody(t: String) = """[[["$t","x",null,null,10]],null,"en"]"""
    private fun isMm(url: String) = url.startsWith("https://api.mymemory.translated.net/")
    private val log = ArrayList<String>()
    private fun engine(f: Fake) = TranslationEngine(f, { s, ms, r -> log.add("$s|$r") })

    @Test fun myMemoryFirstThenMexicanSafetyLayerAndALogWithoutWords() {
        val f = Fake { _, _ -> HttpReply(200, null, mmBody("Voy a alquilar un coche")) }
        val r = engine(f).translate("I will rent a car", Translation.Way.TO_MEXICAN)
        assertTrue(r.ok)
        assertEquals("Voy a rentar un carro", r.text)
        assertEquals("MyMemory", r.by)
        assertEquals(1, f.calls.size)
        assertEquals(listOf("translate (MyMemory)|ok"), log)
        assertFalse(log.joinToString().contains("rent"))
    }

    @Test fun googleAnswersWhenMyMemoryIsUsedUp() {
        val f = Fake { u, _ -> if (isMm(u)) HttpReply(429, null, "") else HttpReply(200, null, gBody("Trae una chaqueta")) }
        val r = engine(f).translate("Bring a jacket", Translation.Way.TO_MEXICAN)
        assertEquals("Trae una chamarra", r.text)
        assertEquals("Google Translate", r.by)
        assertEquals(2, f.calls.size)
        assertEquals("q=Bring+a+jacket", f.calls[1].second) // POST body, nothing in the address
        assertFalse(f.calls[1].first.contains("Bring"))
        assertEquals(listOf("translate (MyMemory)|answered 429", "translate (Google Translate)|ok"), log)
    }

    @Test fun googleGetsTheShorthandWrittenOut() {
        val f = Fake { u, _ -> if (isMm(u)) HttpReply(503, null, "") else HttpReply(200, null, gBody("hola")) }
        engine(f).translate("idk tbh", Translation.Way.TO_MEXICAN)
        assertEquals("q=I+don%27t+know+to+be+honest", f.calls[1].second)
    }

    @Test fun bothBusyIsOneSentence() {
        val f = Fake { u, _ -> if (isMm(u)) HttpReply(429, null, "") else HttpReply(302, "https://www.google.com/sorry/x", "") }
        val r = engine(f).translate("Hello", Translation.Way.TO_MEXICAN)
        assertFalse(r.ok); assertTrue(r.busy); assertEquals(ErrorCode.BUSY, r.code)
        assertTrue(r.why.startsWith("Both free translation services are busy"))
    }

    @Test fun noInternetIsOneSentenceNotTwo() {
        val f = Fake { _, _ -> throw TransportException(TransportException.Kind.NO_INTERNET) }
        val r = engine(f).translate("Hello", Translation.Way.TO_MEXICAN)
        assertFalse(r.ok); assertEquals(ErrorCode.NO_INTERNET, r.code)
        assertTrue(r.why, r.why.startsWith("The tablet is not on the internet"))
        assertEquals(1, r.why.split("The tablet is not on the internet").size - 1)
        assertEquals(2, f.calls.size)
    }

    @Test fun timeoutsAndOddFailures() {
        val t = Fake { _, _ -> throw TransportException(TransportException.Kind.TIMEOUT) }
        assertEquals(ErrorCode.TIMEOUT, engine(t).translate("Hello", Translation.Way.TO_MEXICAN).code)
        val o = Fake { _, _ -> throw TransportException(TransportException.Kind.OTHER, "SSLException") }
        assertTrue(engine(o).translate("Hello", Translation.Way.TO_MEXICAN).why.contains("SSLException"))
        val big = Fake { _, _ -> throw TransportException(TransportException.Kind.TOO_BIG) }
        assertEquals(ErrorCode.BAD_ANSWER, engine(big).translate("Hello", Translation.Way.TO_MEXICAN).code)
        val boom = Fake { _, _ -> throw IllegalStateException("x") }
        val r = engine(boom).translate("Hello", Translation.Way.TO_MEXICAN)
        assertEquals(ErrorCode.UNEXPECTED, r.code)
        assertFalse(r.why.contains("Hello"))
    }

    @Test fun eachWayAConnectionCanBreakHasItsOwnWordsAndCode() {
        fun r(k: TransportException.Kind, m: String?) = engine(Fake { _, _ -> throw TransportException(k, m) }).translate("Hello", Translation.Way.TO_MEXICAN)
        val dropped = r(TransportException.Kind.DROPPED, "SocketException (ECONNRESET, connection reset)")
        assertEquals(ErrorCode.DROPPED, dropped.code); assertTrue(dropped.why, dropped.why.startsWith("The connection broke while translating (SocketException (ECONNRESET, connection reset))"))
        val blocked = r(TransportException.Kind.BLOCKED, "SocketException (EPERM, not permitted)")
        assertEquals(ErrorCode.BLOCKED, blocked.code); assertTrue(blocked.why, blocked.why.startsWith("Android is not letting the keyboard use the network (SocketException (EPERM") && blocked.why.contains("Data Saver"))
        val secure = r(TransportException.Kind.SECURE, "SSLHandshakeException (certificate not trusted)")
        assertEquals(ErrorCode.SECURE, secure.code); assertTrue(secure.why, secure.why.contains("date and time"))
        val other = r(TransportException.Kind.OTHER, "IOException")
        assertEquals(ErrorCode.SERVICE_ERROR, other.code); assertEquals("The translation service could not be reached (IOException). Diagnostics has a connection test.", other.why)
        assertEquals("no reason in brackets when there is none", "The translation service could not be reached. Diagnostics has a connection test.", r(TransportException.Kind.OTHER, null).why)
        for (x in listOf(dropped, blocked, secure, other)) assertFalse("never the words", x.why.contains("Hello"))
    }

    @Test fun theConnectionLogGetsTheReasonButNeverTheWords() {
        val lines = ArrayList<String>()
        val e = TranslationEngine(Fake { _, _ -> throw TransportException(TransportException.Kind.DROPPED, "SocketException (connection reset)") }, TranslateLog { s, ms, res -> lines.add("$s|$res") })
        e.translate("my secret hello", Translation.Way.TO_MEXICAN)
        assertEquals(2, lines.size) // MyMemory, then Google
        assertTrue(lines[0], lines[0].contains("failed (DROPPED: SocketException (connection reset))"))
        for (l in lines) assertFalse(l, l.contains("secret") || l.contains("hello"))
    }

    @Test fun mixedFailureKeepsBothReasons() {
        val f = Fake { u, _ -> if (isMm(u)) HttpReply(429, null, "") else HttpReply(404, null, "") }
        val r = engine(f).translate("Hello", Translation.Way.TO_MEXICAN)
        assertFalse(r.ok)
        assertTrue(r.why.contains("MyMemory") && r.why.contains("Google"))
    }

    @Test fun refusesEmptyLetterlessAndTooLongWithoutAsking() {
        val f = Fake { _, _ -> throw AssertionError("no call expected") }
        val e = engine(f)
        assertEquals(ErrorCode.NOTHING_TO_TRANSLATE, e.translate("  ", Translation.Way.TO_MEXICAN).code)
        assertEquals(ErrorCode.NOTHING_TO_TRANSLATE, e.translate("123 😀", Translation.Way.TO_MEXICAN).code)
        assertEquals(ErrorCode.TOO_LONG, e.translate("a".repeat(1001), Translation.Way.TO_MEXICAN).code)
        assertTrue(f.calls.isEmpty())
    }

    @Test fun longTextGoesInPiecesAndSpacingSurvives() {
        val sentence = "This is sentence number one and it is rather long. "
        val text = sentence.repeat(14) // about 714 bytes -> 2 pieces of at most 450
        val f = Fake { u, _ -> HttpReply(200, null, mmBody("Pieza")) }
        val r = engine(f).translate(text, Translation.Way.TO_MEXICAN)
        assertTrue(r.ok)
        assertEquals(2, f.calls.size)
        assertEquals("Pieza Pieza", r.text.trim().let { it.replace(Regex("\\s+"), " ") }.let { if (it == "PiezaPieza") "Pieza Pieza" else it })
        for ((u, _) in f.calls) assertTrue(u.length < 2000)
    }

    @Test fun emojiOnlyPiecesAreKeptNotSent() {
        val f = Fake { _, _ -> HttpReply(200, null, mmBody("Hola")) }
        val text = "Hello\n😀😀\n" + "x".repeat(10)
        val r = engine(f).translate(text, Translation.Way.TO_MEXICAN)
        assertTrue(r.ok)
        assertEquals(1, f.calls.size)
    }

    @Test fun friendlyTuAsksTheOtherServiceOnce() {
        val f = Fake { u, _ -> if (isMm(u)) HttpReply(200, null, mmBody("¿Puede conducir?")) else HttpReply(200, null, gBody("¿Sabes conducir?")) }
        val r = engine(f).translate("Can you drive?", Translation.Way.TO_MEXICAN)
        assertEquals("¿Sabes manejar?", r.text)
        assertEquals("Google Translate", r.by)
        assertFalse(r.formal)
        assertEquals(2, f.calls.size)
    }

    @Test fun bothFormalShowsTheFirstUnchangedAndFlagsIt() {
        val f = Fake { u, _ -> if (isMm(u)) HttpReply(200, null, mmBody("¿Puede ayudarme?")) else HttpReply(200, null, gBody("¿Puede ayudarme usted?")) }
        val r = engine(f).translate("Can you help me?", Translation.Way.TO_MEXICAN)
        assertEquals("¿Puede ayudarme?", r.text)
        assertEquals("MyMemory", r.by)
        assertTrue(r.formal)
    }

    @Test fun formalFirstAnswerAndGoogleDownKeepsTheFirst() {
        val f = Fake { u, _ -> if (isMm(u)) HttpReply(200, null, mmBody("¿Quiere comer?")) else HttpReply(429, null, "") }
        val r = engine(f).translate("Do you want to eat?", Translation.Way.TO_MEXICAN)
        assertTrue(r.ok); assertEquals("¿Quiere comer?", r.text); assertTrue(r.formal)
    }

    @Test fun toEnglishIsNotRewrittenOrChecked() {
        val f = Fake { _, _ -> HttpReply(200, null, mmBody("Can you pick up the coche")) }
        val r = engine(f).translate("¿Puedes recoger el coche?", Translation.Way.TO_ENGLISH)
        assertEquals("Can you pick up the coche", r.text)
        assertTrue(f.calls[0].first.contains("langpair=es-MX%7Cen"))
    }

    @Test fun cancelledRequestsStopAskingAndAreMarked() {
        var n = 0
        val f = Fake { _, _ -> n++; HttpReply(200, null, mmBody("Hola")) }
        val r = engine(f).translate("Hello", Translation.Way.TO_MEXICAN) { true }
        assertEquals(ErrorCode.CANCELLED, r.code)
        assertEquals(0, n)
        // cancelled while the first service was being asked: the answer is dropped
        var cancel = false
        val g = Fake { _, _ -> cancel = true; HttpReply(200, null, mmBody("Hola")) }
        assertEquals(ErrorCode.CANCELLED, engine(g).translate("Hello", Translation.Way.TO_MEXICAN) { cancel }.code)
    }
}
