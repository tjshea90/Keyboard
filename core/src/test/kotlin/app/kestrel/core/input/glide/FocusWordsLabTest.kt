package app.kestrel.core.input.glide

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.Random

/**
 * R29: the words the owner named (2026-11-21): and -> access, for -> fur, make -> Maggie, or -> our / out. A simulated finger glides each of them (and a few other short everyday words) many times on three
 * geometries; the decoder that ships (no personal learning) answers; reported per word: how often the first choice is right and which wrong words win most. Says whether the PLAIN decoder reproduces the owner's slips
 * (then the cure is in the decoder) or not (then it is state the keyboard learned). Skipped unless REAL_TABLET=1.
 */
class FocusWordsLabTest {
    private fun keyOf(s: String) = String(GlideLetters.keyOf(s))

    @Test fun theOwnersWords() {
        assumeTrue("set REAL_TABLET=1", System.getenv("REAL_TABLET") == "1")
        val lex = GlideEval.loadLexicon()
        val n = System.getenv("REAL_TABLET_N")?.toIntOrNull() ?: 150
        val words = listOf("and", "for", "make", "or", "the", "to", "of", "it", "is", "you", "was", "not", "can", "but")
        val geos = listOf("phone 292x161" to (292f to 161f), "tablet portrait 564x272" to (564f to 272f), "tablet landscape 916x272" to (916f to 272f))
        val prevs = listOf<String?>(null, "you", "to")
        for ((name, wh) in geos) for (aim in listOf(8f, 12f)) {
            val keys = GlideEval.keysFor(wh.first, wh.second)
            val dec = GlideDecoder(keys, GlideParams.forPitchDp(keys.pitch))
            val sim = GestureSim(keys, Random(5L + wh.first.toLong() + aim.toLong()))
            val noise = SimNoise.aim(aim)
            val sb = StringBuilder("FOCUS $name aim ${aim.toInt()} dp:")
            var totAll = 0; var hitAll = 0
            for (w in words) {
                if (lex.idOf(w) < 0) continue
                val wrong = HashMap<String, Int>(); var hit = 0; var tot = 0
                for (prev in prevs) for (i in 0 until n) {
                    val path = sim.path(w, noise) ?: continue
                    val c = dec.decode(path.xs, path.ys, path.xs.size, listOf<GlideLexicon>(lex), prev, 3)
                    tot++
                    val f = c.firstOrNull()?.text
                    if (f != null && keyOf(f) == keyOf(w)) hit++ else if (f != null) wrong.merge(f, 1) { a, b -> a + b }
                }
                totAll += tot; hitAll += hit
                val top = wrong.entries.sortedByDescending { it.value }.take(3).joinToString(",") { it.key + " " + it.value }
                sb.append("\n    %-5s right %5.1f%%  wrong: %s".format(w, 100.0 * hit / maxOf(1, tot), top))
            }
            sb.append("\n    ALL   right %5.1f%%".format(100.0 * hitAll / maxOf(1, totAll)))
            println(sb)
        }
    }
}
