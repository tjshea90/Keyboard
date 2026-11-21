package app.kestrel.core.input.glide

import java.util.Random
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * R29: the price of every letter beyond the second ([GlideParams.letterW]). The focus words ("or" taken for "our"/"out") ask for a price; this is the yardstick that says what the price costs on EVERYDAY words
 * of every length (the frequency-weighted pick of [GlideEval]) with the simulated finger, on the phone and the two tablet shapes, so a value that fixes "or" can not quietly spoil "thing" or "because".
 * Skipped unless REAL_TABLET=1. LAB_LETTERW = comma list (default 0,0.5,1,2).
 */
class LetterWEvalTest {
    private val lex by lazy { GlideEval.loadLexicon() }

    @Test fun yardstickByLetterPrice() {
        assumeTrue("set REAL_TABLET=1", System.getenv("REAL_TABLET") == "1")
        val ws = (System.getenv("LAB_LETTERW") ?: "0,0.5,1,2").split(',').map { it.toFloat() }
        val n = System.getenv("REAL_TABLET_N")?.toIntOrNull() ?: 1500
        val geos = listOf("phone 292x161" to (292f to 161f), "tablet portrait 564x272" to (564f to 272f), "tablet landscape 916x272" to (916f to 272f))
        for ((name, wh) in geos) {
            val keys = GlideEval.keysFor(wh.first, wh.second)
            for (noise in listOf(SimNoise.NORMAL, SimNoise.SLOPPY)) {
                val picks = GlideEval.frequencyPick(lex, n, Random(77))
                val sb = StringBuilder("LW $name ${noise.name}:")
                for (w in ws) {
                    val r = GlideEval.run(lex, keys, GlideParams.forPitchDp(keys.pitch, letterW = w), noise, picks, 77L)
                    sb.append("  w=%.1f top1 %.2f top3 %.2f".format(w, r.pct(r.top1), r.pct(r.top3)))
                }
                println(sb)
            }
        }
    }
}
