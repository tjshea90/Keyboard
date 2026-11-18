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
            "cur" to GlideParams(),
            "W1.5 hi.22" to GlideParams(lengthW = 1.5f, lengthSigmaHigh = 0.22f),
            "W1 hi.22" to GlideParams(lengthSigmaHigh = 0.22f),
            "W1.5 exp1.0" to GlideParams(lengthW = 1.5f, lengthExpect = 1.0f),
            "W1.5 lo.22 hi.22" to GlideParams(lengthW = 1.5f, lengthSigmaLow = 0.22f, lengthSigmaHigh = 0.22f),
        )
        for ((name, p) in cfgs) {
            val sb = StringBuilder("SWEEP %-18s".format(name))
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
