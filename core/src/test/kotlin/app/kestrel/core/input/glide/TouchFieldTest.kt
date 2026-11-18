package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/** Owner R20d: where on the keyboard the thumb lands differently is learned (TouchField), measured with the simulated hand (GestureSim zones). */
class TouchFieldTest {
    private val keys = GlideEval.keysFor()
    private val lex by lazy { GlideEval.loadLexicon() }

    /** A person glides [glides] common words; the keyboard learns from every word that was among its guesses (what GlideEval does); the first guess is judged over the LAST third, after it has learned. */
    private fun lastThird(noise: SimNoise, params: GlideParams, glides: Int = 1500, seed: Long = 21): Double {
        val picks = GlideEval.frequencyPick(lex, glides, Random(seed))
        val sim = GestureSim(keys, Random(seed)); val dec = GlideDecoder(keys, params)
        var hit = 0; var n = 0
        for ((k, pk) in picks.withIndex()) {
            val word = lex.text(pk.id); val path = sim.path(word, noise) ?: continue
            val c = dec.decode(path.xs, path.ys, path.xs.size, listOf(lex), if (pk.prev >= 0) lex.text(pk.prev) else null, 3)
            val key = String(GlideLetters.keyOf(word))
            if (k >= glides * 2 / 3) { n++; if (c.isNotEmpty() && String(GlideLetters.keyOf(c[0].text)) == key) hit++ }
            if (c.any { String(GlideLetters.keyOf(it.text)) == key }) dec.learnFrom(lex, pk.id)
        }
        return 100.0 * hit / n
    }

    // ------------------------------------------------------------------ the map itself

    @Test fun anEmptyMapChangesNothingAndTheBilinearBlendIsSmooth() {
        val f = TouchField(); val o = FloatArray(2); assertTrue(f.isEmpty); f.at(0.3f, 0.7f, o); assertEquals(0f, o[0], 0f); assertEquals(0f, o[1], 0f)
        f.dx[0] = 0.4f; f.dx[2] = -0.4f                           // top-left and top-right grid points
        f.at(0f, 0f, o); assertEquals(0.4f, o[0], 1e-5f); f.at(1f, 0f, o); assertEquals(-0.4f, o[0], 1e-5f); f.at(0.5f, 0f, o); assertEquals("half way across the top row", 0f, o[0], 1e-5f)
        f.at(0.25f, 0f, o); assertEquals(0.2f, o[0], 1e-5f); f.at(0f, 1f, o); assertEquals("nothing at the bottom", 0f, o[0], 1e-5f)
    }

    @Test fun learningMovesTheNearestPointsAndIsBoundedAndOutliersAreCut() {
        val f = TouchField(); repeat(500) { f.learn(0f, 0f, 0.3f, -0.2f, 0.05f) }
        assertTrue("the top-left grid point learned the offset: ${f.dx[0]}", f.dx[0] in 0.25f..0.5f); assertTrue(f.dy[0] < -0.15f); assertEquals("far points did not move", 0f, f.dx[8], 1e-3f)
        val g = TouchField(); repeat(2000) { g.learn(0.5f, 0.5f, 9f, 9f, 0.5f) }
        assertTrue("never beyond the limit: ${g.dx[4]}", g.dx[4] <= TouchField.MAX + 1e-6f)
        val h = TouchField(); h.learn(0.5f, 0.5f, 100f, 0f, 1f); assertTrue("one wild observation counts for at most CLIP: ${h.dx[4]}", h.dx[4] <= TouchField.CLIP)
        assertEquals(500, f.seen)
    }

    @Test fun theMapSurvivesTheNumbersForThePrivateFileAndRejectsBadOnes() {
        val f = TouchField(); repeat(40) { f.learn(0.2f, 0.9f, 0.2f, 0.1f, 0.1f) }
        val g = TouchField(); assertTrue(g.fromArray(f.toArray())); for (i in f.dx.indices) { assertEquals(f.dx[i], g.dx[i], 0f); assertEquals(f.dy[i], g.dy[i], 0f) }; assertEquals(f.seen, g.seen)
        val bad = f.toArray(); bad[3] = Float.NaN; assertFalse(TouchField().fromArray(bad)); assertFalse(TouchField().fromArray(FloatArray(5)))
        g.clear(); assertTrue(g.isEmpty); assertEquals(0f, g.dx[0], 0f)
    }

    // ------------------------------------------------------------------ what it is worth (simulated hand; 1500 glides, the last 500 judged)

    @Test fun aThumbThatLandsDifferentlyOnTheTwoSidesIsLearnedAndHelps() {
        val noise = SimNoise.NORMAL.withZone(12f, 6f, -12f, 6f)
        val off = lastThird(noise, GlideParams(touchField = false)); val on = lastThird(noise, GlideParams())
        assertTrue("single shift only $off %, with the map $on %", on >= off + 1.5)
    }

    @Test fun aThumbThatLandsTheSameEverywhereLosesNothing() {
        val off = lastThird(SimNoise.NORMAL, GlideParams(touchField = false)); val on = lastThird(SimNoise.NORMAL, GlideParams())
        assertTrue("single shift only $off %, with the map $on %", on >= off - 1.5)
        val sloppy = SimNoise.SLOPPY.withUserBias(10f)
        val so = lastThird(sloppy, GlideParams(touchField = false)); val sn = lastThird(sloppy, GlideParams())
        assertTrue("sloppy with a habit: $so % vs $sn %", sn >= so - 2.0)
    }

    @Test fun theMapDoesNotCostTheDecoderAnyMeasurableTime() {
        val picks = GlideEval.frequencyPick(lex, 300, Random(4)); val sim = GestureSim(keys, Random(4))
        fun time(p: GlideParams): Long { val d = GlideDecoder(keys, p); repeat(80) { val w = lex.text(picks[it].id); sim.path(w, SimNoise.NORMAL)?.let { q -> d.decode(q.xs, q.ys, q.xs.size, listOf(lex), null, 3); d.learnFrom(lex, picks[it].id) } }
            val t0 = System.nanoTime(); for (k in 80 until 300) { val w = lex.text(picks[k].id); sim.path(w, SimNoise.NORMAL)?.let { q -> d.decode(q.xs, q.ys, q.xs.size, listOf(lex), null, 3) } }; return (System.nanoTime() - t0) / 1_000_000 }
        time(GlideParams()); val without = time(GlideParams(touchField = false)); val with = time(GlideParams())
        assertTrue("220 glides: $with ms with the map, $without ms without", with <= without * 1.5 + 200)
    }
}
