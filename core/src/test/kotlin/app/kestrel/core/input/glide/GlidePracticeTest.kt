package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** R34a2: the practice drill keeps only the prompted words and the finger's paths; the file can be read back exactly as the keyboard saw the glide. */
class GlidePracticeTest {
    private fun keys(): GlideKeys = GlideEval.keysFor(564f, 272f)
    private fun path(n: Int, x0: Float) = FloatArray(n) { x0 + it * 3.5f } to FloatArray(n) { 100f + it * 0.5f }

    @Test fun theWordListIsEverydayLettersOnlyAndHasTheOwnersWords() {
        val w = GlidePractice.WORDS
        assertEquals(40, w.size); assertEquals(w.size, w.toSet().size)
        assertTrue(w.all { it.isNotEmpty() && it.all { c -> c in 'a'..'z' } })
        for (x in listOf("and", "for", "make", "or", "the", "there", "access", "our", "out", "it", "is", "in")) assertTrue("$x is asked", x in w)
    }

    @Test fun eachGlideAnswersTheCurrentWordAndMovesOnSkipsKeepNothing() {
        val p = GlidePractice(listOf("the", "and", "for"))
        assertEquals("the", p.current)
        val (xs, ys) = path(5, 10f); val ts = FloatArray(5) { 1000f + it * 16f }
        p.record(xs, ys, ts, 5, listOf("the", "then", "they"))
        assertEquals("and", p.current); p.skip(); assertEquals("for", p.current)
        p.record(xs, ys, ts, 5, listOf("fur", "for", "fir"))
        assertTrue(p.done); assertNull(p.current); assertEquals(2, p.answered)
        assertEquals(1, p.firstRight()); assertEquals(2, p.inThree()); assertEquals(listOf("for" to "fur"), p.misses())
        assertEquals("times are milliseconds from the first touch", 0f, p.entries[0].ts[0], 0f); assertEquals(64f, p.entries[0].ts[4], 0f)
        p.record(xs, ys, ts, 5, listOf("x")); assertEquals("nothing after the end", 2, p.answered)
    }

    @Test fun theFileReadsBackAsTheKeyboardSawTheGlide() {
        val k = keys(); val p = GlidePractice(listOf("the", "and"))
        val (xs, ys) = path(6, 20f); p.record(xs, ys, FloatArray(6) { 500f + it * 20f }, 6, listOf("the", "then", "they"))
        p.record(xs, ys, FloatArray(6) { it * 10f }, 6, listOf("any", "and", "ant"))
        val text = p.serialize(k, 1128, 520, 2f, "0.7.0-test")
        assertTrue(text.startsWith(GlidePractice.MAGIC + "\n"))
        val back = GlidePractice.parse(text)!!
        assertEquals("0.7.0-test", back.version); assertEquals(1128, back.viewW); assertEquals(520, back.viewH); assertEquals(2f, back.density, 0f)
        assertEquals(2, back.entries.size); assertEquals("the", back.entries[0].word); assertEquals(listOf("any", "and", "ant"), back.entries[1].got)
        for (i in 0 until 6) { assertEquals(xs[i], back.entries[0].xs[i], 0.06f); assertEquals(ys[i], back.entries[0].ys[i], 0.06f); assertEquals(i * 20f, back.entries[0].ts[i], 0.5f) }
        for (i in 0 until 26) { assertEquals(k.cx[i], back.keys.cx[i], 0.01f); assertEquals(k.cy[i], back.keys.cy[i], 0.01f) }
        assertEquals(k.keyW, back.keys.keyW, 0.01f); assertEquals(k.pitch, back.keys.pitch, 0.01f)
        assertEquals("the pitch in dp is rebuilt from the density", k.pitch / 2f, back.keys.pitchDp, 0.01f)
    }

    @Test fun aDamagedFileIsRefusedAndACutOffOneKeepsWhatIsComplete() {
        assertNull(GlidePractice.parse(null)); assertNull(GlidePractice.parse("")); assertNull(GlidePractice.parse("hello")); assertNull(GlidePractice.parse("KESTREL-KEYBOARD-LEARNING 1\nwords"))
        val p = GlidePractice(listOf("the", "and")); val (xs, ys) = path(4, 30f)
        p.record(xs, ys, FloatArray(4), 4, listOf("the")); p.record(xs, ys, FloatArray(4), 4, listOf("and"))
        val text = p.serialize(keys(), 1000, 500, 1f, "v")
        val cut = text.substring(0, text.length - 15)
        val back = GlidePractice.parse(cut)
        assertNotNull(back); assertEquals("the cut-off last line is dropped, the first stays", 1, back!!.entries.size)
        assertNull("no key positions, no file", GlidePractice.parse(text.lines().filter { !it.startsWith("centres") }.joinToString("\n")))
    }

    @Test fun anEmptyPracticeStillWritesAValidHeader() {
        val back = GlidePractice.parse(GlidePractice().serialize(keys(), 800, 400, 1.5f, "v"))
        assertNotNull(back); assertTrue(back!!.entries.isEmpty()); assertFalse(back.version.isEmpty())
    }
}
