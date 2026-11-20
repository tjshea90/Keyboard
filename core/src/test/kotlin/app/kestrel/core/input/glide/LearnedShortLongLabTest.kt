package app.kestrel.core.input.glide

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.Random

/**
 * R28a: can WHAT THE KEYBOARD LEARNED make a short word come out as a longer one (the -> there, and -> access)? A simulated person (a finger with its own habit per word, absolute-dp scatter) glides a long stretch of
 * everyday words on the owner's tablet geometry (real row heights), the keyboard learns exactly as the lab of AX1 does (words, pairs, corrections, glide shapes) plus the thumb habit; every glide is also decoded by a decoder that has
 * learned nothing ("plain"). Reported per stretch of use: first-choice rate of both, and for the short words (4 letters or fewer) how often the first choice was a word 2+ letters LONGER. Skipped unless REAL_TABLET=1.
 */
class LearnedShortLongLabTest {
    private fun keyOf(s: String) = String(GlideLetters.keyOf(s))

    @Test fun learnedStateAndShortWords() {
        assumeTrue("set REAL_TABLET=1", System.getenv("REAL_TABLET") == "1")
        val lex = GlideEval.loadLexicon()
        val glides = System.getenv("REAL_TABLET_N")?.toIntOrNull() ?: 4000
        val blocks = 4
        val geos = listOf("phone 292x161" to (292f to 161f), "tablet portrait 564x272" to (564f to 272f), "tablet landscape 916x272" to (916f to 272f))
        for ((name, wh) in geos) for (habit in listOf(0f, 10f)) for (aim in listOf(10f)) {
            val keys = GlideEval.keysFor(wh.first, wh.second)
            val params = GlideParams.forPitchDp(keys.pitch)
            val noise = SimNoise.aim(aim)
            val seed = 3L
            val rnd = Random(seed * 31 + 7)
            val hand = GlideHabitEval.Hand(keys, noise, seed, habit)
            val dec = GlideDecoder(keys, params)
            val plain = GlideDecoder(keys, GlideParams.forPitchDp(keys.pitch).let { GlideParams(shapeW = it.shapeW, visitCap = it.visitCap, lmMin = 0.45f, patGain = 0f) })
            val model = PersonalModel()
            dec.personal = model; plain.personal = null
            val base = GlideEval.frequencyPick(lex, 4000, Random(seed * 13), 2).map { lex.text(it.id) }
            val lexicons = listOf<GlideLexicon>(lex)
            val seen = IntArray(blocks); val hitL = IntArray(blocks); val hitP = IntArray(blocks)
            val sSeen = IntArray(blocks); val s2lL = IntArray(blocks); val s2lP = IntArray(blocks); val sHitL = IntArray(blocks); val sHitP = IntArray(blocks)
            val flip = IntArray(blocks)
            var done = 0
            while (done < glides) {
                val q = List(1 + rnd.nextInt(3)) { base[rnd.nextInt(base.size)] }
                var prev: String? = null; var prev2: String? = null
                for (w in q) {
                    if (done >= glides) break
                    val path = hand.path(w) ?: continue
                    val key = keyOf(w)
                    val c = dec.decode(path.xs, path.ys, path.xs.size, lexicons, prev, 3, prev2)
                    val c0 = plain.decode(path.xs, path.ys, path.xs.size, lexicons, prev, 3, prev2)
                    val first = c.isNotEmpty() && keyOf(c[0].text) == key
                    val first0 = c0.isNotEmpty() && keyOf(c0[0].text) == key
                    val inThree = c.any { keyOf(it.text) == key }
                    val b = (done * blocks / glides).coerceAtMost(blocks - 1)
                    seen[b]++; if (first) hitL[b]++; if (first0) hitP[b]++
                    if (c.isNotEmpty() && c0.isNotEmpty() && c[0].text != c0[0].text) flip[b]++
                    if (w.length <= 4) {
                        sSeen[b]++; if (first) sHitL[b]++; if (first0) sHitP[b]++
                        if (!first && c.isNotEmpty() && c[0].text.length - w.length >= 2) s2lL[b]++
                        if (!first0 && c0.isNotEmpty() && c0[0].text.length - w.length >= 2) s2lP[b]++
                    }
                    // what the person does and the keyboard learns (as habitCurve): keep, pick from the strip, or type it out
                    val print = dec.lastPrint
                    model.add(prev, w, if (first) 1f else if (inThree) 3f else 1f, prev2)
                    if (!first && inThree && c.isNotEmpty()) model.addConfusion(c[0].text, w)
                    if (print != null) {
                        if (first) { model.patterns.add(w, print, 1f); model.patterns.contradict(w, print, 0.7f) }
                        else if (inThree) { model.patterns.add(w, print, 3f); model.patterns.contradict(w, print, 2f) }
                    }
                    if (first || inThree) { val id = lex.idOf(w); if (id >= 0) dec.learnFrom(lex, id) }
                    prev2 = prev; prev = w; done++
                }
            }
            val sb = StringBuilder("LAB %-26s habit %2.0f dp aim %.0f dp: ".format(name, habit, aim))
            for (b in 0 until blocks) sb.append("| all %.1f/%.1f short %.1f/%.1f s2l %d/%d flips %d ".format(100.0 * hitL[b] / seen[b].coerceAtLeast(1), 100.0 * hitP[b] / seen[b].coerceAtLeast(1), 100.0 * sHitL[b] / sSeen[b].coerceAtLeast(1), 100.0 * sHitP[b] / sSeen[b].coerceAtLeast(1), s2lL[b], s2lP[b], flip[b]))
            println(sb)
        }
    }
}
