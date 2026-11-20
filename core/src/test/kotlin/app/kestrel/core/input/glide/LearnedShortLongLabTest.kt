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
        val only = System.getenv("LAB_GEOS")?.split(',')
        val variants = (System.getenv("LAB_VARIANTS") ?: "full").split(',')
        for ((name, wh) in geos) for (habit in listOf(0f, 10f)) for (aim in listOf(10f)) for (variant in variants) {
            if (only != null && only.none { name.contains(it) }) continue
            val keys = GlideEval.keysFor(wh.first, wh.second)
            val fp = GlideParams.forPitchDp(keys.pitch)
            val sc = { x: Float -> val r = 45f / Math.max(keys.pitch, 45f); x * r * r }
            fun ov(patGain: Float = 2.5f, patCap: Float = 4.5f, patMax: Float = 2.2f, confMax: Float = 0.9f) = GlideParams(shapeW = fp.shapeW, visitCap = fp.visitCap, lmMin = fp.lmMin, patGain = patGain, patCap = patCap, patMaxGap = patMax, confMaxGap = confMax)
            val params = when (variant) {
                "gap" -> ov(patMax = sc(2.2f), confMax = sc(0.9f)); "pg1" -> ov(patGain = 1.25f, patCap = 2.5f); "gap+pg1" -> ov(patGain = 1.25f, patCap = 2.5f, patMax = sc(2.2f), confMax = sc(0.9f))
                "gap2" -> ov(patMax = sc(2.2f) * 0.5f, confMax = sc(0.9f) * 0.5f); "cap2" -> ov(patCap = 2.0f)
 "nopat" -> GlideParams(shapeW = fp.shapeW, visitCap = fp.visitCap, lmMin = fp.lmMin, patGain = 0f); "noconf" -> GlideParams(shapeW = fp.shapeW, visitCap = fp.visitCap, lmMin = fp.lmMin, confGain = 0f); "nofield" -> GlideParams(shapeW = fp.shapeW, visitCap = fp.visitCap, lmMin = fp.lmMin, touchField = false); "pure" -> GlideParams(shapeW = fp.shapeW, visitCap = fp.visitCap, lmMin = fp.lmMin, patGain = 0f, confGain = 0f, touchField = false); else -> fp }
            val noise = SimNoise.aim(aim)
            val seed = 3L
            val rnd = Random(seed * 31 + 7)
            val hand = GlideHabitEval.Hand(keys, noise, seed, habit)
            val dec = GlideDecoder(keys, params)
            val plain = GlideDecoder(keys, GlideParams.forPitchDp(keys.pitch).let { GlideParams(shapeW = it.shapeW, visitCap = it.visitCap, lmMin = 0.45f, patGain = 0f) })
            val model = PersonalModel()
            dec.personal = if (variant == "nopersonal" || variant == "onlythumb") null else model; plain.personal = null
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
                    if ((first || inThree) && variant != "nothumb" && variant != "pure") { val id = lex.idOf(w); if (id >= 0) dec.learnFrom(lex, id) }
                    prev2 = prev; prev = w; done++
                }
            }
            val t = { a: IntArray -> a.sum() }
            println("LAB %-24s habit %2.0f dp %-10s: learned/plain  all %.1f/%.1f  short %.1f/%.1f  short->long %d/%d  first guess differs %d of %d".format(name, habit, variant, 100.0 * t(hitL) / t(seen), 100.0 * t(hitP) / t(seen), 100.0 * t(sHitL) / t(sSeen), 100.0 * t(sHitP) / t(sSeen), t(s2lL), t(s2lP), t(flip), t(seen)))
        }
    }
}
