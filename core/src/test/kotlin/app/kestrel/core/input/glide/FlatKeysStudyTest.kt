package app.kestrel.core.input.glide

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.Random

/**
 * R28a: the study of R27a drew the tablet keys as tall as the phone's (the height was 0.55 of the width, a row 1.4 key pitches apart). The keyboard really caps a row at 56 dp (ROW_H_MAX_DP), so on the owner's tablet
 * (600x973 at 170 dpi = about 564x916 dp) a row is only 1.23 pitches apart in portrait and 0.76 in landscape (a phone: 1.56). Skipped unless REAL_TABLET=1. The same simulated finger (absolute dp) is run over
 * the phone, the tablet in portrait and in landscape with the REAL row heights, and the first choice is judged for the 24 commonest short words and for 700 words picked by how often people use them.
 */
class FlatKeysStudyTest {
    private fun rowRatio(k: GlideKeys): Float = (k.cy['a' - 'a'] - k.cy['q' - 'a']) / k.pitch

    @Test fun flatKeys() {
        assumeTrue("set REAL_TABLET=1", System.getenv("REAL_TABLET") == "1")
        val lex = GlideEval.loadLexicon()
        val shorts = listOf("the", "and", "to", "of", "in", "is", "it", "for", "you", "was", "are", "his", "but", "not", "can", "all", "her", "one", "out", "had", "with", "that", "this", "have")
        val n = System.getenv("REAL_TABLET_N")?.toIntOrNull() ?: 40
        val geos = listOf("phone 292x161" to (292f to 161f), "tablet portrait old study 564x310" to (564f to 310f), "tablet portrait real 564x272" to (564f to 272f), "tablet landscape real 916x272" to (916f to 272f), "tablet landscape old study 916x504" to (916f to 504f))
        for ((name, wh) in geos) {
            val keys = GlideEval.keysFor(wh.first, wh.second)
            val p = GlideParams.forPitchDp(keys.pitch)
            for (aim in listOf(8f, 12f)) {
                val noise = SimNoise.aim(aim)
                val dec = GlideDecoder(keys, p); val sim = GestureSim(keys, Random(5L + wh.first.toLong() + aim.toLong()))
                var tot = 0; var hit = 0; var s2l = 0; var l2s = 0
                for (w in shorts) {
                    if (lex.idOf(w) < 0) continue
                    val key = String(GlideLetters.keyOf(w))
                    for (i in 0 until n) {
                        val path = sim.path(w, noise) ?: continue
                        val c = dec.decode(path.xs, path.ys, path.xs.size, listOf(lex), null, 3)
                        tot++
                        val f = c.firstOrNull() ?: continue
                        if (String(GlideLetters.keyOf(f.text)) == key) hit++ else if (f.text.length - w.length >= 2) s2l++
                    }
                }
                val rnd = Random(99L); val picks = GlideEval.frequencyPick(lex, 700, rnd, 2)
                var t2 = 0; var h2 = 0
                val sim2 = GestureSim(keys, Random(7L + wh.first.toLong() + aim.toLong())); val dec2 = GlideDecoder(keys, p)
                for (pk in picks) {
                    val w = lex.text(pk.id); val key = String(GlideLetters.keyOf(w))
                    val path = sim2.path(w, noise) ?: continue
                    val c = dec2.decode(path.xs, path.ys, path.xs.size, listOf(lex), null, 3)
                    t2++
                    val f = c.firstOrNull() ?: continue
                    if (String(GlideLetters.keyOf(f.text)) == key) h2++ else if (w.length - f.text.length >= 2) l2s++
                }
                println("FLAT %-36s pitch %.0f dp row/pitch %.2f aim %.0f dp (%.2f pitch): short words first %.1f%% short->long %.1f%% | frequency words first %.1f%% long->short %d".format(name, keys.pitch, rowRatio(keys), aim, aim / keys.pitch, 100.0 * hit / tot, 100.0 * s2l / tot, 100.0 * h2 / t2, l2s))
            }
        }
    }
}
