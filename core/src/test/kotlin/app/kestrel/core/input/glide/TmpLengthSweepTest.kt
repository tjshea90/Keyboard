package app.kestrel.core.input.glide

import java.util.Random
import org.junit.Test

class TmpLengthSweepTest {
    private val lex by lazy { GlideEval.loadLexicon() }
    private val keys = GlideEval.keysFor()
    private val pairs = listOf("the" to "there", "he" to "her", "we" to "were", "to" to "top", "an" to "and", "in" to "into", "on" to "one", "is" to "its", "for" to "form", "of" to "off", "or" to "ore", "at" to "ate", "us" to "use", "so" to "some", "me" to "men", "no" to "now", "be" to "bed", "do" to "does", "how" to "show", "all" to "also")
    private fun first(word: String, p: GlideParams, noise: SimNoise, n: Int): Double {
        val id = lex.idOf(word); if (id < 0) return Double.NaN
        val r = GlideEval.run(lex, keys, p, noise, List(n) { GlideEval.Pick(id, -1) }, 5L + word.hashCode()); return r.pct(r.top1)
    }
    @Test fun sweep() {
        val cfgs = listOf(
            "off" to GlideParams(lengthW = 0f),
            "cur 1.0/.92/.16/.30" to GlideParams(),
            "W2" to GlideParams(lengthW = 2f),
            "W3 cap6" to GlideParams(lengthW = 3f, lengthCap = 6f),
            "W2 exp.85" to GlideParams(lengthW = 2f, lengthExpect = 0.85f),
            "W2 lo.12 hi.25" to GlideParams(lengthW = 2f, lengthSigmaLow = 0.12f, lengthSigmaHigh = 0.25f),
        )
        for ((name, p) in cfgs) {
            val sb = StringBuilder("SWEEP %-22s".format(name))
            for (noise in listOf(SimNoise.NORMAL, SimNoise.SLOPPY)) {
                var s = 0.0; var l = 0.0; var k = 0
                for ((a, b) in pairs) { val x = first(a, p, noise, 50); val y = first(b, p, noise, 50); if (!x.isNaN() && !y.isNaN()) { s += x; l += y; k++ } }
                val picks = GlideEval.frequencyPick(lex, 800, Random(21)); val r = GlideEval.run(lex, keys, p, noise, picks, 21L)
                sb.append(" | %s short %.1f long %.1f yard %.1f/%.1f".format(noise.name.substringBefore(' '), s / k, l / k, r.pct(r.top1), r.pct(r.top3)))
            }
            println(sb)
        }
    }
}
