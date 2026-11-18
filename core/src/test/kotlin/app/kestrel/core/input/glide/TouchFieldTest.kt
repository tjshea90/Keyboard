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

    @Test fun probe() {
        val out = StringBuilder()
        for (mag in listOf(0f, 6f, 9f, 12f)) {
            val noise = SimNoise.NORMAL.withZone(mag, mag * 0.5f, -mag, mag * 0.5f)
            val off = lastThird(noise, GlideParams(touchField = false)); val on = lastThird(noise, GlideParams())
            out.append("zone %.0f dp: single shift only %.1f %%  with the map %.1f %%\n".format(mag, off, on))
        }
        val sloppy = SimNoise.SLOPPY.withZone(9f, 4f, -9f, 4f)
        out.append("sloppy zone 9: %.1f -> %.1f\n".format(lastThird(sloppy, GlideParams(touchField = false)), lastThird(sloppy, GlideParams())))
        println(out); org.junit.Assert.fail(out.toString())
    }
}
