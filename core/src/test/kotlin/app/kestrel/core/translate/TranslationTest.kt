package app.kestrel.core.translate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class TranslationTest {
    // ------------------------------------------------------------------------------------------ pieces
    @Test fun shortTextIsOnePiece() {
        assertEquals(listOf("hello there"), Translation.pieces("hello there", 450))
        assertEquals(emptyList<String>(), Translation.pieces("", 450))
        assertEquals(emptyList<String>(), Translation.pieces(null, 450))
    }

    @Test fun piecesPutBackTogetherGiveTheTextAndStayWithinTheLimit() {
        val rnd = Random(7)
        val alphabet = arrayOf("a", "b", "word", "hola", "\u00f1", "\u00e9", " ", " ", "  ", "\n", ". ", "! ", "? ", "\u2026 ", "\uD83D\uDE00", "\uD83D\uDC4D\uD83C\uDFFD", "1", "-")
        for (n in 0 until 3000) {
            val sb = StringBuilder()
            val len = rnd.nextInt(400)
            for (k in 0 until len) sb.append(alphabet[rnd.nextInt(alphabet.size)])
            val max = 20 + rnd.nextInt(60)
            val text = sb.toString()
            val parts = Translation.pieces(text, max)
            assertEquals(text, parts.joinToString(""))
            for (p in parts) {
                assertTrue(Translation.bytes(p) <= max)
                assertTrue(p.isNotEmpty())
                // never cut inside a surrogate pair
                assertFalse(Character.isHighSurrogate(p.last()))
                assertFalse(Character.isLowSurrogate(p.first()))
            }
        }
    }

    @Test fun cutsAtLineBreaksAndSentencesFirst() {
        val a = "x".repeat(30) + ". "
        val text = a + a + a
        val parts = Translation.pieces(text, 70)
        assertEquals(listOf(a + a, a), parts)
        assertEquals(listOf("line one\n", "line two"), Translation.pieces("line one\nline two", 12))
    }

    @Test fun aLongWordIsCutBetweenLetters() {
        val parts = Translation.pieces("y".repeat(1000), 450)
        assertEquals(listOf(450, 450, 100), parts.map { it.length })
    }

    @Test fun hasWordsAndTrimmed() {
        assertTrue(Translation.hasWords("ok"))
        assertFalse(Translation.hasWords("  \uD83D\uDE00 123 \n"))
        assertEquals(listOf("  ", "hi there", " \n"), Translation.trimmed("  hi there \n").toList())
        assertEquals(listOf("", "", ""), Translation.trimmed("").toList())
    }

    // ------------------------------------------------------------------------------------------ URLs
    @Test fun encodingMatchesFormEncoding() {
        assertEquals("a+b%26c%3D%C3%B1%0A%F0%9F%98%80", Translation.enc("a b&c=\u00f1\n\uD83D\uDE00"))
        assertEquals("abc-._*XYZ019", Translation.enc("abc-._*XYZ019"))
    }

    @Test fun everyUrlTheCodeBuildsIsHttpsToOneOfTwoHosts() {
        val urls = ArrayList<String>()
        for (way in Translation.Way.values()) {
            urls.add(Translation.myMemoryUrl("Can you drive? & more=1 \u00f1 \uD83D\uDE00", way))
            urls.add(Translation.googleUrl(way))
        }
        for (u in urls) {
            assertTrue(u, u.startsWith("https://"))
            val host = u.removePrefix("https://").substringBefore('/')
            assertTrue(host, host == Translation.HOST_MYMEMORY || host == Translation.HOST_GOOGLE)
            assertFalse(u.contains(' '))
        }
        assertEquals(
            "https://api.mymemory.translated.net/get?q=Take+it&langpair=en%7Ces-MX&onlyprivate=1&mt=1",
            Translation.myMemoryUrl("Take it", Translation.Way.TO_MEXICAN)
        )
        assertEquals(
            "https://api.mymemory.translated.net/get?q=Hola&langpair=es-MX%7Cen&onlyprivate=1&mt=1",
            Translation.myMemoryUrl("Hola", Translation.Way.TO_ENGLISH)
        )
        assertEquals("https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=es&dt=t", Translation.googleUrl(Translation.Way.TO_MEXICAN))
        assertEquals("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=en&dt=t", Translation.googleUrl(Translation.Way.TO_ENGLISH))
        assertEquals("q=hi+there%21", Translation.googleForm("hi there!"))
    }

    // ------------------------------------------------------------------------------------------ MyMemory answers (hand-written to the documented shape)
    private fun mm(text: String, status: Any = 200, extra: String = "") =
        """{"responseData":{"translatedText":"$text","match":0.85},"quotaFinished":false,"mtLangSupported":null,"responseDetails":"","responseStatus":$status,"responderId":null,"matches":[]$extra}"""

    @Test fun myMemoryGoodAnswer() {
        val a = Translation.parseMyMemory(200, mm("\u00bfSabes conducir?"))
        assertTrue(a.ok)
        assertEquals("\u00bfSabes conducir?", a.text)
        assertEquals("MyMemory", a.by)
        assertEquals(ErrorCode.NONE, a.code)
    }

    @Test fun myMemoryWritesHtmlEntitiesOut() {
        assertEquals("It's \"ok\" & <fine> \u00f1", Translation.parseMyMemory(200, mm("It&#39;s &quot;ok&quot; &amp; &lt;fine&gt; &#xF1;")).text)
        assertEquals("a & b; c &zz; d &", Translation.unescape("a &amp; b; c &zz; d &"))
    }

    @Test fun myMemoryQuotaIsNeverATranslation() {
        val http429 = Translation.parseMyMemory(429, "")
        assertFalse(http429.ok); assertTrue(http429.busy); assertEquals(ErrorCode.BUSY, http429.code)
        val finished = Translation.parseMyMemory(200, """{"responseData":{"translatedText":"x"},"quotaFinished":true,"responseStatus":200}""")
        assertFalse(finished.ok); assertTrue(finished.busy)
        val warning = Translation.parseMyMemory(
            200,
            mm("MYMEMORY WARNING: YOU USED ALL AVAILABLE FREE TRANSLATIONS FOR TODAY. NEXT AVAILABLE IN  03 HOURS 12 MINUTES 5 SECONDS")
        )
        assertFalse(warning.ok); assertTrue(warning.busy); assertEquals(ErrorCode.BUSY, warning.code)
        val inBody = Translation.parseMyMemory(200, mm("hola", "\"429\""))
        assertFalse(inBody.ok); assertTrue(inBody.busy)
    }

    @Test fun myMemoryRefusalsAndBadAnswers() {
        val bad = Translation.parseMyMemory(200, mm("", 403, "").replace("\"responseDetails\":\"\"", "\"responseDetails\":\"INVALID EMAIL PROVIDED\""))
        assertFalse(bad.ok); assertFalse(bad.busy); assertEquals(ErrorCode.SERVICE_ERROR, bad.code)
        assertTrue(bad.why, bad.why.contains("INVALID EMAIL PROVIDED"))
        val srv = Translation.parseMyMemory(200, mm("", 500))
        assertTrue(srv.busy)
        val err = Translation.parseMyMemory(503, "oops")
        assertFalse(err.ok); assertTrue(err.busy); assertEquals(ErrorCode.SERVICE_ERROR, err.code)
        assertFalse(Translation.parseMyMemory(404, "x").busy)
        assertEquals(ErrorCode.BAD_ANSWER, Translation.parseMyMemory(200, "<html>").code)
        assertEquals(ErrorCode.BAD_ANSWER, Translation.parseMyMemory(200, null).code)
        assertEquals(ErrorCode.BAD_ANSWER, Translation.parseMyMemory(200, mm("   ")).code)
        assertEquals(ErrorCode.BAD_ANSWER, Translation.parseMyMemory(200, "{}").code)
        assertEquals(ErrorCode.BAD_ANSWER, Translation.parseMyMemory(200, """{"responseData":null,"responseStatus":200}""").code)
        // an enormous detail text is shortened
        val long = Translation.parseMyMemory(200, mm("", 403).replace("\"responseDetails\":\"\"", "\"responseDetails\":\"" + "d".repeat(500) + "\""))
        assertTrue(long.why.length < 200)
    }

    // ------------------------------------------------------------------------------------------ Google answers
    @Test fun googleGoodAnswerJoinsSentences() {
        val body = """[[["Hola. ","Hello. ",null,null,10],["\u00bfC\u00f3mo est\u00e1s?","How are you?",null,null,10]],null,"en",null,null,null,1,[]]"""
        val a = Translation.parseGoogle(200, null, body)
        assertTrue(a.ok)
        assertEquals("Hola. \u00bfC\u00f3mo est\u00e1s?", a.text)
        assertEquals("Google Translate", a.by)
    }

    @Test fun googleBusyAndBadAnswers() {
        assertTrue(Translation.parseGoogle(429, null, "").busy)
        assertTrue(Translation.parseGoogle(503, null, "").busy)
        val puzzle = Translation.parseGoogle(302, "https://www.google.com/sorry/index?continue=x", "")
        assertFalse(puzzle.ok); assertTrue(puzzle.busy); assertEquals(ErrorCode.BUSY, puzzle.code)
        assertTrue(Translation.parseGoogle(200, null, "<a href=\"https://www.google.com/sorry/index\">").busy)
        assertEquals(ErrorCode.SERVICE_ERROR, Translation.parseGoogle(404, null, "").code)
        assertTrue(Translation.parseGoogle(500, null, "").busy)
        assertEquals(ErrorCode.BAD_ANSWER, Translation.parseGoogle(200, null, "garbage").code)
        assertEquals(ErrorCode.BAD_ANSWER, Translation.parseGoogle(200, null, "[]").code)
        assertEquals(ErrorCode.BAD_ANSWER, Translation.parseGoogle(200, null, "{}").code)
        assertEquals(ErrorCode.BAD_ANSWER, Translation.parseGoogle(200, null, "[[]]").code)
        assertEquals(ErrorCode.BAD_ANSWER, Translation.parseGoogle(200, null, """[[[null,"x"]]]""").code)
        assertEquals(ErrorCode.BAD_ANSWER, Translation.parseGoogle(200, null, """[[["  ","x"]]]""").code)
    }

    // ------------------------------------------------------------------------------------------ shorthand
    @Test fun shorthandIsWrittenOutAsWholeWordsOnly() {
        assertEquals("are you coming tonight? I don't know if i can make it, to be honest", Translation.expandShorthand("r u coming 2nite? idk if i can make it, tbh"))
        assertEquals("You there?", Translation.expandShorthand("U there?"))
        assertEquals("with you, without me", Translation.expandShorthand("w/ u, w/o me"))
        assertEquals("don't", Translation.expandShorthand("don't"))
        assertEquals("I'm u'd", Translation.expandShorthand("I'm u'd"))
        assertEquals("ruler", Translation.expandShorthand("ruler"))
        assertEquals("", Translation.expandShorthand(null))
        assertEquals("see you at 5", Translation.expandShorthand("cya at 5"))
    }

    // ------------------------------------------------------------------------------------------ Mexican
    private fun mx(s: String) = Translation.toMexican(s)

    @Test fun cogerBecomesAgarrarInEveryFormWithTheRightAccent() {
        assertEquals("Quiero agarrar el autob\u00fas", mx("Quiero coger el autob\u00fas"))
        assertEquals("agarra", mx("coge"))
        assertEquals("agarro", mx("cojo"))
        assertEquals("agarr\u00e9", mx("cog\u00ed"))
        assertEquals("agarrando", mx("cogiendo"))
        assertEquals("Ag\u00e1rralo", mx("C\u00f3gelo"))
        assertEquals("agarrarlo", mx("cogerlo"))
        assertEquals("No agarres eso", mx("No cojas eso"))
        assertEquals("agarraremos", mx("cogeremos"))
        assertEquals("agarrar\u00eda", mx("coger\u00eda"))
        assertEquals("AGARRAR", mx("COGER"))
    }

    @Test fun limpingIsLeftAlone() {
        assertEquals("Est\u00e1 cojo", mx("Est\u00e1 cojo"))
        assertEquals("el cojo", mx("el cojo"))
        assertEquals("la coja", mx("la coja"))
    }

    @Test fun clothesAndWordsFromSpain() {
        assertEquals("Trae una chamarra", mx("Trae una chaqueta"))
        assertEquals("unos tenis blancos", mx("unas zapatillas blancas"))
        assertEquals("el refrigerador est\u00e1 vac\u00edo", mx("la nevera est\u00e1 vac\u00eda"))
        assertEquals("del refrigerador", mx("de la nevera"))
        assertEquals("una pluma nueva", mx("un bol\u00edgrafo nuevo"))
        assertEquals("Voy a rentar un carro", mx("Voy a alquilar un coche"))
        assertEquals("rent\u00e9 un departamento", mx("alquil\u00e9 un apartamento"))
        assertEquals("Vamos por jugo y papas", mx("Vamos por zumo y patatas"))
        assertEquals("Quiero manejar", mx("Quiero conducir"))
        assertEquals("estacionar", mx("aparcar"))
        assertEquals("No te enojes", mx("No te enfades"))
        assertEquals("Mi celular", mx("Mi m\u00f3vil"))
        assertEquals("el mesero", mx("el camarero"))
        assertEquals("La alberca", mx("La piscina"))
    }

    @Test fun thingsThatMustNotChange() {
        for (s in listOf(
            "datos m\u00f3viles", "no vale la pena", "\u00bfCu\u00e1nto vale?", "Eso vale mucho", "conduce a la playa",
            "Voy a la playa", "Qu\u00e9 onda", "Mi celular est\u00e1 en la mesa", "Hola, \u00bfc\u00f3mo est\u00e1s?", "", "123 \uD83D\uDE00"
        )) assertEquals(s, mx(s))
    }

    @Test fun valeAsOkAndNoVosotros() {
        assertEquals("Ok, nos vemos", mx("Vale, nos vemos"))
        assertEquals("Ustedes est\u00e1n listos y tienen su carro", mx("Vosotros est\u00e1is listos y ten\u00e9is vuestro coche"))
    }

    @Test fun secondRunChangesNothingAndNoCogerSurvivesRandomText() {
        val rnd = Random(11)
        val bits = arrayOf(
            "coger", "coge", "cojo", "cogiendo", "c\u00f3gelo", "cogimos", "chaqueta", "zapatillas", "la nevera", "est\u00e1", "vac\u00eda",
            "m\u00f3vil", "mi", "vale", ",", ".", " ", " ", "conducir", "conduce", "a", "la", "playa", "alquilar", "coche", "vosotros", "est\u00e1is",
            "\u00bf", "?", "bol\u00edgrafo", "nuevo", "de", "gafas", "tarta", "\n", "\uD83D\uDE00"
        )
        for (n in 0 until 4000) {
            val sb = StringBuilder()
            for (k in 0 until 1 + rnd.nextInt(12)) { sb.append(bits[rnd.nextInt(bits.size)]).append(' ') }
            val once = mx(sb.toString())
            assertEquals(once, mx(once))
            assertFalse(once, Regex("(?i)\\bcog(e|i|\u00ed)").containsMatchIn(once))
        }
    }

    @Test fun likeKeepsCapitalisation() {
        assertEquals("Papa", Translation.like("Patata", "papa"))
        assertEquals("PAPA", Translation.like("PATATA", "papa"))
        assertEquals("papa", Translation.like("patata", "papa"))
        assertNotEquals("PAPA", Translation.like("P", "papa"))
    }
}
