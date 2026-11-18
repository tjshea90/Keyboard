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

    // ------------------------------------------------------------------ it is kept, exported and forgotten like the rest of what is learned

    @Test fun theExportCarriesTheMapAndAnOlderFileWithoutOneStillReads() {
        val f = TouchField(); repeat(60) { f.learn(0.1f, 0.2f, 0.25f, -0.1f, 0.08f) }
        val habit = floatArrayOf(0.1f, -0.05f) + f.toArray()
        val text = LearnedExport.write(PersonalModel().also { it.add(null, "hello", 2f) }, habit, null)
        assertTrue(text.lines().any { it.startsWith("touch ") })
        val back = LearnedExport.read(text)!!
        assertEquals(0.1f, back.habit!![0], 0.001f); assertTrue("the map came back", back.touch != null && back.touch!!.size == f.toArray().size)
        val g = TouchField(); assertTrue(g.fromArray(back.touch!!)); assertEquals(f.dx[0], g.dx[0], 0.002f)
        val old = LearnedExport.write(PersonalModel().also { it.add(null, "hello", 2f) }, floatArrayOf(0.1f, -0.05f), null)
        assertTrue("a file without the map has no touch line", old.lines().none { it.startsWith("touch ") }); assertEquals(null, LearnedExport.read(old)!!.touch)
        val broken = text.lines().joinToString("\n") { if (it.startsWith("touch ")) "touch 1 2 3" else it }
        assertEquals("a map of the wrong size is ignored, the rest is read", null, LearnedExport.read(broken)!!.touch)
    }

    @Test fun theEngineRestoresTheMapAfterARestartAndForgetsItWhenAsked() {
        val saved = ArrayList<FloatArray>()
        fun engine(habit: FloatArray?): GlideTyping {
            val e = GlideTyping({ LexiconFile.build(listOf("for" to 900L, "fir" to 300L, "hello" to 500L, "world" to 800L).map { LexiconFile.Entry(it.first, it.second) }, emptyList()) }, java.util.concurrent.Executor { it.run() })
            e.prepare(keys, habit, null); return e
        }
        val e = engine(null); val sim = GestureSim(keys, Random(2)); val noise = SimNoise.NORMAL.withZone(12f, 6f, -12f, 6f)
        repeat(120) { for (w in listOf("hello", "world", "for")) sim.path(w, noise)?.let { p -> e.decode(p.xs, p.ys, p.xs.size, null, null) { }; e.accepted(w) { h -> saved.add(h) } } }
        val last = saved.last(); assertEquals("two habit numbers and the map", 2 + TouchField().toArray().size, last.size); assertTrue("it learned something: ${last.toList()}", last.drop(2).take(18).any { Math.abs(it) > 0.05f })
        val again = engine(last); var restored: FloatArray? = null
        again.exportText(null, null) { t -> restored = LearnedExport.read(t)?.touch }; assertTrue("the restarted engine has the map", restored != null && restored!!.take(18).any { Math.abs(it) > 0.05f })
        again.forgetAll(); var gone = false; again.exportText(null, null) { t -> gone = LearnedExport.read(t)?.touch == null }; assertTrue("forgetting clears it", gone)
    }

    @Test fun probe2() {
        val out = StringBuilder()
        fun avg(noise: SimNoise, p: GlideParams): Double = (21L..26L).map { lastThird(noise, p, 1200, it) }.average()
        val sloppyHabit = SimNoise.SLOPPY.withUserBias(10f); val normalZone = SimNoise.NORMAL.withZone(12f, 6f, -12f, 6f); val sloppyZone = SimNoise.SLOPPY.withZone(10f, 5f, -10f, 5f)
        for ((name, noise) in listOf("sloppy+habit10" to sloppyHabit, "normal+zone12" to normalZone, "sloppy+zone10" to sloppyZone, "normal" to SimNoise.NORMAL, "sloppy" to SimNoise.SLOPPY)) {
            out.append("$name: off %.1f".format(avg(noise, GlideParams(touchField = false))))
            for (rate in listOf(0.005f, 0.01f, 0.02f)) out.append("  rate $rate: %.1f".format(avg(noise, GlideParams(fieldRate = rate))))
            out.append("\n")
        }
        println(out); org.junit.Assert.fail(out.toString())
    }
}
