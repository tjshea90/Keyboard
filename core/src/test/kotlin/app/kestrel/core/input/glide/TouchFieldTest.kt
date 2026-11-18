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

    private fun runDetail(noise: SimNoise, params: GlideParams, glides: Int = 1500, seed: Long = 21): String {
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
        val f = dec.field
        return "%.1f %% (bias %.2f,%.2f pitch; field seen %d; dx %s dy %s)".format(100.0 * hit / n, dec.biasX, dec.biasY, f.seen, f.dx.joinToString(",") { "%.2f".format(it) }, f.dy.joinToString(",") { "%.2f".format(it) })
    }

    @Test fun probe() {
        val out = StringBuilder()
        val z = SimNoise.NORMAL.withZone(12f, 6f, -12f, 6f); val z6 = SimNoise.NORMAL.withZone(6f, 3f, -6f, 3f); val sl = SimNoise.SLOPPY.withZone(12f, 6f, -12f, 6f)
        for ((c, r) in listOf(2 to 2, 3 to 2, 3 to 3, 5 to 3, 4 to 4)) {
            val p = GlideParams(fieldCols = c, fieldRows = r, fieldRate = 0.03f)
            out.append("grid ${c}x$r: normal12 %.1f  normal6 %.1f  sloppy12 %.1f\n".format(lastThird(z, p), lastThird(z6, p), lastThird(sl, p)))
        }
        out.append("none: normal12 %.1f  normal6 %.1f  sloppy12 %.1f\n".format(lastThird(z, GlideParams(touchField = false)), lastThird(z6, GlideParams(touchField = false)), lastThird(sl, GlideParams(touchField = false))))
        for (rate in listOf(0.015f, 0.03f)) { val p = GlideParams(fieldRate = rate); out.append("3x3 rate $rate: normal12 %.1f  normal6 %.1f  sloppy12 %.1f  calm-none %.1f\n".format(lastThird(z, p), lastThird(z6, p), lastThird(sl, p), lastThird(SimNoise.NORMAL, p))) }
        out.append("no zone, field off %.1f, on %.1f\n".format(lastThird(SimNoise.NORMAL, GlideParams(touchField = false)), lastThird(SimNoise.NORMAL, GlideParams())))
        println(out); org.junit.Assert.fail(out.toString())
    }
}
