package app.kestrel.core.input.glide

import org.junit.Assert.fail
import org.junit.Test
import java.util.Random

class TmpNumbersTest {
    private val keys = GlideEval.keysFor(); private val lex by lazy { GlideEval.loadLexicon() }
    private fun lastThird(noise: SimNoise, params: GlideParams, glides: Int, seed: Long): Double {
        val picks = GlideEval.frequencyPick(lex, glides, Random(seed)); val sim = GestureSim(keys, Random(seed)); val dec = GlideDecoder(keys, params)
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
    @Test fun numbers() {
        val sb = StringBuilder()
        fun avg(noise: SimNoise, p: GlideParams) = (21L..26L).map { lastThird(noise, p, 1200, it) }.average()
        for ((name, noise) in listOf("normal, no pattern" to SimNoise.NORMAL, "sloppy, no pattern" to SimNoise.SLOPPY, "sloppy + one habit 10 dp" to SimNoise.SLOPPY.withUserBias(10f),
            "normal, sides differ 12 dp (fan)" to SimNoise.NORMAL.withZone(12f, 6f, -12f, 6f), "normal, sides differ 6 dp" to SimNoise.NORMAL.withZone(6f, 3f, -6f, 3f),
            "sloppy, sides differ 10 dp" to SimNoise.SLOPPY.withZone(10f, 5f, -10f, 5f), "normal, left thumb only drifts (left 10, right 0)" to SimNoise.NORMAL.withZone(10f, 4f, 0f, 0f))) {
            sb.append("%-52s single shift only %.1f %%   with the plane %.1f %%\n".format(name, avg(noise, GlideParams(touchField = false)), avg(noise, GlideParams())))
        }
        fail(sb.toString())
    }
}
