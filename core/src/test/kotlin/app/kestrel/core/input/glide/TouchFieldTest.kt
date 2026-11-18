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
        val z = SimNoise.NORMAL.withZone(12f, 6f, -12f, 6f); val sl = SimNoise.SLOPPY.withZone(12f, 6f, -12f, 6f)
        out.append("pitch ${keys.pitch}\n")
        for (rate in listOf(0f, 0.03f, 0.06f, 0.12f, 0.25f)) out.append("normal zone 12, rate $rate: ${runDetail(z, GlideParams(touchField = rate > 0f, fieldRate = rate))}\n")
        for (rate in listOf(0f, 0.06f, 0.12f, 0.25f)) out.append("sloppy zone 12, rate $rate: ${runDetail(sl, GlideParams(touchField = rate > 0f, fieldRate = rate))}\n")
        println(out); org.junit.Assert.fail(out.toString())
    }
}
