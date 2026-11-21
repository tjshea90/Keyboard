package app.kestrel.core.input.glide

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.Random

/**
 * R29: does a wrong look-alike that the person only KEPT feed itself? (owner 2026-11-21: "it still types the word access a lot when i glide the word and ...") A simulated person glides a long stretch of everyday words
 * on the owner's tablet geometry; the keyboard learns as in the real app. After a wrong first guess the person either fixes it (picks it from the strip when it is among the three, else types it) or, with the
 * chance [carelessness], does not notice / bother and KEEPS the wrong word, which the keyboard then learns as if it were right (that is what GlideSession.confirmPrevious does for a kept glide).
 * Variants: "ideal" (fixes every one), "careless" (keeps 40 % of the wrong ones, learning as shipped before R29), "careless+guard" (the same person, LearnGuard in the learning). Reported per quarter of the run:
 * first-choice rate of the learned decoder and of a plain one that learned nothing, how often learning alone spoiled a glide (plain right, learned wrong) and fixed one, and how often the guard acted.
 * Skipped unless REAL_TABLET=1.
 */
class KeptErrorsLabTest {
    private fun keyOf(s: String) = String(GlideLetters.keyOf(s))

    @Test fun keptLookAlikesFeedThemselves() {
        assumeTrue("set REAL_TABLET=1", System.getenv("REAL_TABLET") == "1")
        val lex = GlideEval.loadLexicon()
        val glides = System.getenv("REAL_TABLET_N")?.toIntOrNull() ?: 6000
        val blocks = 4
        val geos = listOf("phone 292x161" to (292f to 161f), "tablet portrait 564x272" to (564f to 272f), "tablet landscape 916x272" to (916f to 272f))
        val only = System.getenv("LAB_GEOS")?.split(',')
        val variants = (System.getenv("LAB_VARIANTS") ?: "ideal,careless,careless+guard").split(',')
        val careless = System.getenv("LAB_CARELESS")?.toFloatOrNull() ?: 0.4f
        for ((name, wh) in geos) for (habit in listOf(10f)) for (variant in variants) {
            if (only != null && only.none { name.contains(it) }) continue
            val keys = GlideEval.keysFor(wh.first, wh.second)
            val fp = GlideParams.forPitchDp(keys.pitch)
            val mod = variant.substringAfter(':', ""); val vname = variant.substringBefore(':')   // "ideal:nopat": the same run with ONE learning part switched off (to find which part costs accuracy)
            val params = GlideParams(shapeW = fp.shapeW, visitCap = fp.visitCap, lmMin = fp.lmMin, patGain = if (mod == "nopat") 0f else 2.5f, confGain = if (mod == "noconf") 0f else 0.9f, touchField = mod != "nofield")
            val noise = SimNoise.aim(10f)
            val seed = 3L
            val rnd = Random(seed * 31 + 7)
            val keepWrong = Random(seed * 101 + 5)
            val hand = GlideHabitEval.Hand(keys, noise, seed, habit)
            val dec = GlideDecoder(keys, params)
            val plain = GlideDecoder(keys, GlideParams(shapeW = fp.shapeW, visitCap = fp.visitCap, lmMin = 0.45f, patGain = 0f))
            val model = PersonalModel()
            dec.personal = if (mod == "nopersonal") null else model; plain.personal = null
            val pk = if (vname == "ideal") 0f else careless
            val guard = vname.endsWith("+guard")
            val base = GlideEval.frequencyPick(lex, 4000, Random(seed * 13), 2).map { lex.text(it.id) }
            val lexicons = listOf<GlideLexicon>(lex)
            val seen = IntArray(blocks); val hitL = IntArray(blocks); val hitP = IntArray(blocks)
            val spoiled = IntArray(blocks); val fixed = IntArray(blocks); val guarded = IntArray(blocks); val kept = IntArray(blocks)
            val wrongPairs = HashMap<String, Int>()
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
                    if (first0 && !first) spoiled[b]++; if (first && !first0) fixed[b]++
                    val print = dec.lastPrint
                    var learnedWord = w; var weight = 1f; var viaStrip = false
                    if (!first && c.isNotEmpty() && keepWrong.nextFloat() < pk) {
                        // not noticed / not bothered: the wrong word stays in the text and the keyboard learns it as a kept glide
                        learnedWord = c[0].text; kept[b]++
                        wrongPairs.merge(c[0].text + "<-" + w, 1) { a, x -> a + x }
                    } else if (!first && inThree) { weight = 3f; viaStrip = true }
                    var shape = true; var habitOk = true
                    if (guard && learnedWord != w) {
                        val p0 = c0.firstOrNull()?.text
                        val gap = if (p0 != null && p0 != learnedWord) plain.staticLn(lex, p0) - plain.staticLn(lex, learnedWord) else 0f
                        val v = LearnGuard.judge(p0, c[0].text, gap, learnedWord, weight, false, false)
                        if (v.circular) { guarded[b]++; weight = v.weight; shape = v.shape; habitOk = v.habit }
                    }
                    model.add(prev, learnedWord, weight, prev2)
                    if (!first && viaStrip && c.isNotEmpty()) model.addConfusion(c[0].text, learnedWord)
                    if (print != null && shape) {
                        if (first || learnedWord != w) { model.patterns.add(learnedWord, print, weight); model.patterns.contradict(learnedWord, print, 0.7f) }
                        else if (inThree) { model.patterns.add(learnedWord, print, 3f); model.patterns.contradict(learnedWord, print, 2f) }
                    }
                    if ((first || inThree || learnedWord != w) && habitOk && mod != "nothumb") { val id = lex.idOf(learnedWord); if (id >= 0) dec.learnFrom(lex, id) }
                    prev2 = prev; prev = learnedWord; done++
                }
            }
            val t = { a: IntArray -> a.sum() }
            val perBlock = (0 until blocks).joinToString(" ") { "%.1f".format(100.0 * hitL[it] / maxOf(1, seen[it])) }
            val top = wrongPairs.entries.sortedByDescending { it.value }.take(4).joinToString(", ") { it.key + " x" + it.value }
            println("KEPT %-24s habit %2.0f dp %-16s: learned %.1f%% (by quarter: %s) plain %.1f%%  learning spoiled %d fixed %d  kept-wrong %d  guard acted %d  most kept: %s".format(
                name, habit, variant, 100.0 * t(hitL) / t(seen), perBlock, 100.0 * t(hitP) / t(seen), t(spoiled), t(fixed), t(kept), t(guarded), top))
        }
    }
}
