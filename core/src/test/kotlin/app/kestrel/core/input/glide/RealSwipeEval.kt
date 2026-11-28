package app.kestrel.core.input.glide

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * R24b step 2: REAL swipes through the REAL decoder and word list. Skipped unless REAL_SWIPES=1 (see [RealSwipeLook] for the variables). Prints:
 *  1 the key geometry CALIBRATED from the data (the corpus does not say where its keys are): an axis-aligned fit of the mean start / end point of each letter onto this keyboard's key centres
 *  2 real-thumb statistics (aim scatter at the first / last letter in key pitches, the share of the ideal path length that was drawn) to compare with the simulator's assumptions
 *  3 first / top-3 accuracy of the shipped decoder, with and without the word before as context, and the parameter variants listed in [variants]
 * The data is never written into the repository (build/real-swipes.jsonl is ignored by git).
 */
class RealSwipeEval {
    private val keys = GlideEval.keysFor()
    private val lex = GlideEval.loadLexicon()

    class Fit(val ax: Float, val bx: Float, val ay: Float, val by: Float, val residualPitch: Float)

    /** Least squares ourX = ax * x + bx, ourY = ay * y + by over the mean first-letter start and last-letter end of every letter with at least [minN] swipes. */
    fun calibrate(sw: List<RealSwipe.Swipe>, minN: Int = 15): Fit {
        val sx = HashMap<Char, MutableList<FloatArray>>()
        for (s in sw) { val l = RealSwipe.letters(s.word) ?: continue; sx.getOrPut(l.first()) { ArrayList() }.add(floatArrayOf(s.xs.first(), s.ys.first())); sx.getOrPut(l.last()) { ArrayList() }.add(floatArrayOf(s.xs.last(), s.ys.last())) }
        val px = ArrayList<Double>(); val py = ArrayList<Double>(); val tx = ArrayList<Double>(); val ty = ArrayList<Double>(); val wt = ArrayList<Double>()
        for ((c, l) in sx) { if (l.size < minN) continue; val k = c - 'a'; px.add(l.map { it[0].toDouble() }.average()); py.add(l.map { it[1].toDouble() }.average()); tx.add(keys.cx[k].toDouble()); ty.add(keys.cy[k].toDouble()); wt.add(l.size.toDouble()) }
        fun lsq(a: List<Double>, b: List<Double>): Pair<Double, Double> {
            val sw0 = wt.sum(); val ma = a.indices.sumOf { wt[it] * a[it] } / sw0; val mb = b.indices.sumOf { wt[it] * b[it] } / sw0
            val cov = a.indices.sumOf { wt[it] * (a[it] - ma) * (b[it] - mb) }; val v = a.indices.sumOf { wt[it] * (a[it] - ma) * (a[it] - ma) }
            val slope = cov / v; return slope to (mb - slope * ma)
        }
        val (ax, bx) = lsq(px, tx); val (ay, by) = lsq(py, ty)
        var rss = 0.0
        for (i in px.indices) { val dx = ax * px[i] + bx - tx[i]; val dy = ay * py[i] + by - ty[i]; rss += wt[i] * (dx * dx + dy * dy) }
        return Fit(ax.toFloat(), bx.toFloat(), ay.toFloat(), by.toFloat(), (Math.sqrt(rss / wt.sum()) / keys.pitch).toFloat())
    }

    class Item(val word: String, val key: String, val prev: String?, val xs: FloatArray, val ys: FloatArray, val lexId: Int, val ts: FloatArray = FloatArray(0))

    private fun clean(w: String) = w.lowercase().trim { !it.isLetter() }

    fun items(sw: List<RealSwipe.Swipe>, fit: Fit): List<Item> {
        val out = ArrayList<Item>()
        for (s in sw) {
            val letters = RealSwipe.letters(s.word) ?: continue
            val w = clean(s.word)
            val id = lex.idOf(w).let { if (it >= 0) it else lex.idOf(letters) }
            if (id < 0) continue                                          // not in the word list: a different question (coverage), reported separately
            if (distinct(letters) < 2) continue
            val toks = s.sentence.split(' ')
            val prev = if (s.wordIdx > 0 && s.wordIdx - 1 < toks.size) clean(toks[s.wordIdx - 1]).ifEmpty { null } else null
            val xs = FloatArray(s.xs.size) { fit.ax * s.xs[it] + fit.bx }; val ys = FloatArray(s.xs.size) { fit.ay * s.ys[it] + fit.by }
            out.add(Item(lex.text(id), String(GlideLetters.keyOf(lex.text(id))), prev?.takeIf { lex.idOf(it) >= 0 }, xs, ys, id, s.ts))
        }
        return out
    }

    private fun distinct(s: String): Int { var m = 0; var p = ' '; for (c in s) if (c != p) { m++; p = c }; return m }

    /** [s2l]: a short word (4 letters or fewer) was meant and the first choice is a word 2 or more letters LONGER (the -> there, and -> access). */
    class Score(val n: Int, val top1: Int, val top3: Int, val s2l: Int = 0) { fun pct(x: Int) = 100.0 * x / n; override fun toString() = "n=%d top1=%.1f%% top3=%.1f%% s2l=%d".format(n, pct(top1), pct(top3), s2l) }

    fun score(items: List<Item>, p: GlideParams, context: Boolean, cm: ClassModel? = null): Score {
        val dec = GlideDecoder(keys, p); dec.classModel = cm
        var t1 = 0; var t3 = 0; var s2l = 0
        for (it in items) {
            val c = dec.decode(it.xs, it.ys, it.xs.size, listOf(lex), if (context) it.prev else null, 3)
            if (c.isNotEmpty() && String(GlideLetters.keyOf(c[0].text)) != it.key && it.word.length <= 4 && c[0].text.length - it.word.length >= 2) s2l++
            if (c.isNotEmpty() && String(GlideLetters.keyOf(c[0].text)) == it.key) t1++
            if (c.any { x -> String(GlideLetters.keyOf(x.text)) == it.key }) t3++
        }
        return Score(items.size, t1, t3, s2l)
    }

    @Test fun replay() {
        assumeTrue("set REAL_SWIPES=1", System.getenv("REAL_SWIPES") == "1")
        val f = File(System.getenv("REAL_SWIPES_FILE") ?: "build/real-swipes.jsonl")
        if (!f.exists()) println("fetched " + RealSwipe.fetch(System.getenv("REAL_SWIPES_URL") ?: RealSwipe.DEV_URL, (System.getenv("REAL_SWIPES_MB")?.toLongOrNull() ?: 20L) * 1_000_000, f, (System.getenv("REAL_SWIPES_OFFSET_MB")?.toLongOrNull() ?: 0L) * 1_000_000) + " bytes")
        val limit = System.getenv("REAL_SWIPES_N")?.toIntOrNull() ?: Int.MAX_VALUE
        val sw = RealSwipe.read(f, limit)
        val fit = calibrate(sw)
        println("swipes read: ${sw.size}; keyboard fit: x' = %.2f x + %.2f, y' = %.2f y + %.2f  (residual of letter centres %.2f key pitches)".format(fit.ax, fit.bx, fit.ay, fit.by, fit.residualPitch))
        val its = items(sw, fit)
        println("in the word list with 2+ letters: ${its.size} of ${sw.size}")
        println("with a known previous word: ${its.count { it.prev != null }}")
        val base = GlideParams()
        println("SHIPPED, no context:   " + score(its, base, false))
        println("SHIPPED, with context: " + score(its, base, true))
        // the real finger: scatter of the first / last touch around that letter's key (in key pitches), and the share of the ideal path that was drawn
        val dx = ArrayList<Double>(); val dy = ArrayList<Double>(); val lr = ArrayList<Double>()
        for (it in its) {
            val k0 = it.key.first().code; val k1 = it.key.last().code
            dx.add(((it.xs.first() - keys.cx[k0]) / keys.pitch).toDouble()); dy.add(((it.ys.first() - keys.cy[k0]) / keys.pitch).toDouble())
            dx.add(((it.xs.last() - keys.cx[k1]) / keys.pitch).toDouble()); dy.add(((it.ys.last() - keys.cy[k1]) / keys.pitch).toDouble())
            if (distinct(it.key) >= 3) {
                var ideal = 0.0; var prevK = -1
                for (c in it.key) { val k = c.code; if (k != prevK) { if (prevK >= 0) ideal += Math.hypot((keys.cx[k] - keys.cx[prevK]).toDouble(), (keys.cy[k] - keys.cy[prevK]).toDouble()) / keys.pitch; prevK = k } }
                var drawn = 0.0; for (i in 1 until it.xs.size) drawn += Math.hypot((it.xs[i] - it.xs[i - 1]).toDouble(), (it.ys[i] - it.ys[i - 1]).toDouble()) / keys.pitch
                if (ideal > 0.5 && drawn > 0.3) lr.add(Math.log(drawn / ideal))
            }
        }
        fun sd(l: List<Double>) = Math.sqrt(l.sumOf { (it - l.average()) * (it - l.average()) } / l.size)
        println("real touch at first/last letter (key pitches): mean dx %.2f dy %.2f, sd dx %.2f dy %.2f  -> aim scatter about %.1f dp at pitch %.1f dp (simulator NORMAL: 8 dp)".format(dx.average(), dy.average(), sd(dx), sd(dy), (sd(dx) + sd(dy)) / 2 * keys.pitch, keys.pitch))
        val sorted = lr.sorted()
        println("ln(drawn / ideal length), words of 3+ letters: n=%d mean %.3f (ratio %.2f) sd %.3f  p10 %.3f p50 %.3f p90 %.3f  (decoder: expect ln .92 = -0.083, sigma .16 low / .30 high)".format(lr.size, lr.average(), Math.exp(lr.average()), sd(lr), sorted[sorted.size / 10], sorted[sorted.size / 2], sorted[sorted.size * 9 / 10]))
        // the variants
        if (System.getenv("REAL_DIAG") == "1") diagnose(sw, its)
        if (System.getenv("REAL_POST") == "1") postCorrection(sw, fit)
        if (System.getenv("REAL_DWELL") == "1") dwellStudy(its)
        if (System.getenv("REAL_CLASS") == "1") classStudy(its)
        if (System.getenv("REAL_RARE") == "1") rareStudy(its)
        // the same swipes, only the words people use every day (the 5000 commonest of the list): what texting looks like (the corpus itself is encyclopaedia text full of names)
        val ranked = (0 until lex.size).sortedByDescending { lex.logProb(it) }.take(5000).toHashSet()
        val common = its.filter { it.lexId in ranked }
        println("everyday words only (top 5000): n=${common.size}  no ctx ${score(common, base, false)} | ctx ${score(common, base, true)}")
        val want = (System.getenv("REAL_VARIANTS") ?: "none").split(',').map { it.trim() }
        val vit = its.take(System.getenv("REAL_VARIANT_N")?.toIntOrNull() ?: 2500)
        val cit = common.take(System.getenv("REAL_VARIANT_N")?.toIntOrNull() ?: 2500)
        if (want != listOf("none")) {
            val chosen = variants().filter { want == listOf("all") || it.first in want }
            val pool = java.util.concurrent.Executors.newFixedThreadPool((System.getenv("REAL_THREADS")?.toIntOrNull() ?: Runtime.getRuntime().availableProcessors()).coerceAtLeast(1))
            val jobs = (listOf("SHIPPED" to base) + chosen).map { (name, p) -> pool.submit<String> { "%-34s all(ctx) %s | everyday(ctx) %s".format(name, score(vit, p, true), score(cit, p, true)) } }
            println("variants on the first ${vit.size} swipes / ${cit.size} everyday-word swipes, with the word before as context:")
            for (j in jobs) println(j.get())
            pool.shutdown()
        }
    }

    /**
     * R27a (owner 2026-11-19: "when i try to type a small word like the or and, it types there and access"): who is confused with whom, by LENGTH. Counts the first-choice misses by how much longer / shorter the
     * chosen word is than the one meant, the commonest (meant -> chosen) pairs, and the accuracy of the commonest short words one by one.
     */
    private fun shortLong(its: List<Item>) {
        val dec = GlideDecoder(keys, GlideParams.forPitchDp(keys.pitchDp))
        val pairs = HashMap<String, Int>(); val perWord = HashMap<String, IntArray>()
        var n = 0; var miss = 0; var longer1 = 0; var longer2 = 0; var shorter1 = 0; var shorter2 = 0; var same = 0
        var shortMeant = 0; var shortMiss = 0; var shortToLong = 0; var longMeant = 0; var longToShort = 0
        for (it in its) {
            val c = dec.decode(it.xs, it.ys, it.xs.size, listOf(lex), it.prev, 3)
            n++
            val first = c.firstOrNull() ?: continue
            val hit = String(GlideLetters.keyOf(first.text)) == it.key
            val a = perWord.getOrPut(it.word) { IntArray(2) }; a[0]++; if (!hit) a[1]++
            val isShort = it.word.length <= 4
            if (isShort) shortMeant++ else longMeant++
            if (hit) continue
            miss++; if (isShort) shortMiss++
            val d = first.text.length - it.word.length
            if (d >= 2) longer2++ else if (d == 1) longer1++ else if (d == 0) same++ else if (d == -1) shorter1++ else shorter2++
            if (isShort && d >= 2) shortToLong++
            if (!isShort && d <= -2) longToShort++
            pairs.merge(it.word + " > " + first.text, 1, Int::plus)
        }
        println("SHORTLONG n=$n first-choice misses $miss; chosen word LONGER by 2+ letters: $longer2, by 1: $longer1; same length: $same; SHORTER by 1: $shorter1, by 2+: $shorter2")
        println("SHORTLONG short meant words (4 letters or fewer): $shortMeant, missed $shortMiss (%.1f%%); of those read as a word 2+ letters longer: $shortToLong (%.1f%% of all short words); long words read as 2+ letters shorter: $longToShort of $longMeant (%.1f%%)".format(100.0 * shortMiss / shortMeant, 100.0 * shortToLong / shortMeant, 100.0 * longToShort / longMeant))
        println("SHORTLONG commonest confusions (meant > chosen): " + pairs.entries.sortedByDescending { it.value }.take(40).joinToString("  ") { it.key + " " + it.value })
        val common = listOf("the", "and", "to", "of", "in", "is", "it", "for", "that", "you", "was", "with", "are", "his", "her", "this", "have", "from", "they", "not", "but", "had", "were", "which", "their")
        println("SHORTLONG the commonest short words, n / miss %: " + common.mapNotNull { w -> perWord[w]?.let { a -> "$w ${a[0]}/%.0f".format(100.0 * a[1] / a[0]) } }.joinToString("  "))
    }

    /**
     * R26a part 1: next-word post-correction on REAL consecutive words. Each sentence of the corpus is replayed word by word the way a person types it: a word with two or more different letters is
     * glided (decoded with the word ON SCREEN before it as context: the decoder's own first choice, not the truth), a one-letter word is "tapped" (correct). After word k+1 is on screen, word k's five
     * best candidates are re-scored with the likelihood of the word that now follows them (lm * ln(0.6 p(next) + 0.4 p(next | candidate)), the decoder's own prior formula with the candidate as the word
     * before) and word k is replaced when a candidate beats the first choice by more than [margin] nats. Reported: first-choice accuracy of word k before and after, mended and spoiled words.
     */
    private fun postCorrection(sw: List<RealSwipe.Swipe>, fit: Fit) {
        val dec = GlideDecoder(keys, GlideParams.forPitchDp(keys.pitchDp))
        class Slot(val truthKey: String?, var onScreen: String, val cands: List<GlideCandidate>?, val lm: Float, val tapped: Boolean)
        val margins = (System.getenv("REAL_POST_MARGINS") ?: "0.5,1,2,3,4").split(',').map { it.trim().toFloat() }
        val weights = (System.getenv("REAL_POST_WEIGHTS") ?: "1").split(',').map { it.trim().toFloat() }
        val maxChains = System.getenv("REAL_POST_N")?.toIntOrNull() ?: Int.MAX_VALUE
        // the chains: consecutive swipes of one sentence with word_idx going up by one
        val chains = ArrayList<List<RealSwipe.Swipe>>(); var cur = ArrayList<RealSwipe.Swipe>()
        for (s0 in sw) {
            if (cur.isNotEmpty() && (cur.last().sentence != s0.sentence || s0.wordIdx != cur.last().wordIdx + 1)) { chains.add(cur); cur = ArrayList() }
            cur.add(s0)
        }
        if (cur.isNotEmpty()) chains.add(cur)
        // R40: the weight rule ([CorrectionGuard]) swept on the same replay: "off" is the shipped R26a behaviour, the others refuse a reading that is more than the allowance rarer than the word on screen
        // (name, minDrop of rule A, pair gain of rule B (0 = rule B off); a negative minDrop = no guard at all)
        val guards = listOf(Triple("off", -1f, 0f), Triple("A1.5", 1.5f, 0f), Triple("A1.5+B5", 1.5f, 5f), Triple("A1.5+B3", 1.5f, 3f), Triple("A1.5+B7", 1.5f, 7f), Triple("A1.0+B5", 1.0f, 5f), Triple("A2.5+B5", 2.5f, 5f))
        val n = margins.size * weights.size * guards.size
        val tot = IntArray(n); val before = IntArray(n); val after = IntArray(n)
        val mended = IntArray(n); val spoiled = IntArray(n); val changed = IntArray(n)
        val detail = ArrayList<String>()
        fun ln(text: String): Float { val id = lex.idOf(text); return if (id < 0) -20f else lex.logProb(id) }
        fun pair(a: String, nid: Int): Float { val id = lex.idOf(a); return if (id < 0) Float.NaN else lex.bigramLogProb(id, nid) }
        fun allowed(g: Triple<String, Float, Float>, oldText: String, newText: String, nid: Int): Boolean =
            g.second < 0f || CorrectionGuard.allows(ln(oldText), ln(newText), pair(oldText, nid), pair(newText, nid), minDrop = g.second, pairGain = if (g.third > 0f) g.third else 1000f)
        var words = 0; var glided = 0
        for ((ci, chain) in chains.withIndex()) {
            if (ci >= maxChains) break
            val slots = ArrayList<Slot>()
            var onScreenPrev: String? = null
            for (s0 in chain) {
                val letters = RealSwipe.letters(s0.word); val w = clean(s0.word); val id = if (letters == null) -1 else lex.idOf(w).let { if (it >= 0) it else lex.idOf(letters) }
                words++
                if (letters == null || id < 0 || distinct(letters) < 2) { slots.add(Slot(null, w, null, 0f, true)); onScreenPrev = w.takeIf { lex.idOf(it) >= 0 } ?: onScreenPrev; continue }
                val xs = FloatArray(s0.xs.size) { fit.ax * s0.xs[it] + fit.bx }; val ys = FloatArray(s0.xs.size) { fit.ay * s0.ys[it] + fit.by }
                val c = dec.decode(xs, ys, xs.size, listOf(lex), onScreenPrev?.takeIf { lex.idOf(it) >= 0 }, 5)
                glided++
                val tk = String(GlideLetters.keyOf(lex.text(id)))
                if (c.isEmpty()) { slots.add(Slot(tk, "", null, 0f, false)); continue }
                slots.add(Slot(tk, c[0].text, c, dec.lastLm, false)); onScreenPrev = c[0].text
            }
            for (k in 0 until slots.size - 1) {
                val a = slots[k]; val nx = slots[k + 1]
                val cands = a.cands ?: continue
                val nid = lex.idOf(nx.onScreen)
                if (nid < 0) continue
                val ok0 = String(GlideLetters.keyOf(cands[0].text)) == a.truthKey
                val pnext = Math.exp(lex.logProb(nid).toDouble())
                val oldLn = ln(cands[0].text)
                for ((wi, wt) in weights.withIndex()) {
                    val lnP = DoubleArray(cands.size) { i -> val cid = lex.idOf(cands[i].text); var pb = 0.0; if (cid >= 0) { val b = lex.bigramLogProb(cid, nid); if (!b.isNaN()) pb = Math.exp(b.toDouble()) }; Math.log(0.6 * pnext + 0.4 * pb) }
                    val tot2 = DoubleArray(cands.size) { i -> cands[i].score + wt * a.lm * lnP[i] }
                    for ((mi, m) in margins.withIndex()) for ((gi, g) in guards.withIndex()) {
                        val ix = (wi * margins.size + mi) * guards.size + gi
                        var best = 0; for (i in 1 until cands.size) if (allowed(g, cands[0].text, cands[i].text, nid) && tot2[i] > tot2[best]) best = i
                        tot[ix]++; if (ok0) before[ix]++
                        val pick = if (best != 0 && tot2[best] - tot2[0] > m) best else 0
                        val ok1 = String(GlideLetters.keyOf(cands[pick].text)) == a.truthKey
                        if (ok1) after[ix]++
                        if (pick != 0) {
                            changed[ix]++; if (!ok0 && ok1) mended[ix]++ else if (ok0 && !ok1) spoiled[ix]++
                            if (gi == 0 && mi == margins.indexOf(1f).coerceAtLeast(0) && wi == 0) detail.add("%s -> %s  (next %s)  drop %.1f  ln old %.1f new %.1f  pair old %.1f new %.1f  %s".format(cands[0].text, cands[pick].text, nx.onScreen, oldLn - ln(cands[pick].text), oldLn, ln(cands[pick].text), pair(cands[0].text, nid), pair(cands[pick].text, nid), if (!ok0 && ok1) "MENDED" else if (ok0 && !ok1) "SPOILED" else "neutral"))
                        }
                    }
                }
            }
        }
        println("POST words $words, glided $glided, chains ${chains.size}; judged words (a glided word with a next word on screen): ${tot[0]}")
        for ((wi, wt) in weights.withIndex()) for ((mi, m) in margins.withIndex()) for ((gi, g) in guards.withIndex()) {
            val ix = (wi * margins.size + mi) * guards.size + gi
            println("POST weight %.1f margin %.1f guard %-6s: first-choice before %.2f%% after %.2f%% (%+.2f points); changed %d, mended %d, spoiled %d".format(wt, m, g.first, 100.0 * before[ix] / tot[ix], 100.0 * after[ix] / tot[ix], 100.0 * (after[ix] - before[ix]) / tot[ix], changed[ix], mended[ix], spoiled[ix]))
        }
        for (d in detail) println("POSTCHANGE $d")
    }

    /**
     * R26a part 2: is there a DWELL / SPEED signal in real swipes at all? Per swipe: pause share (time spent moving slower than a fifth of the swipe's own median speed), slow-point count, duration per
     * pitch of path. AUC (Mann-Whitney, 0.5 = no signal) of each feature between words WITH a doubled letter (the finger is expected to dwell or loop) and words without, among words of the same number
     * of distinct letters; and, for the decoder's real ambiguity, among TWIN words (the collapsed key sequence is shared with another word of the list: to/too, of/off, god/good).
     */
    private fun dwellStudy(its: List<Item>) {
        class F(val pause: Double, val slow: Double, val tPerPitch: Double, val doubled: Boolean, val len: Int, val twin: Boolean)
        val byKey = HashMap<String, Int>()
        for (id in 0 until lex.size) { val k = String(GlideLetters.keyOf(lex.text(id))); byKey.merge(k, 1, Int::plus) }
        val feats = ArrayList<F>()
        for (it in its) {
            if (it.ts.size != it.xs.size || it.xs.size < 8) continue
            val n = it.xs.size
            val sp = DoubleArray(n - 1); var tt = 0.0; var dist = 0.0
            for (i in 1 until n) { val dt = (it.ts[i] - it.ts[i - 1]).toDouble().coerceAtLeast(1.0); val d = Math.hypot((it.xs[i] - it.xs[i - 1]).toDouble(), (it.ys[i] - it.ys[i - 1]).toDouble()) / keys.pitch; sp[i - 1] = d / dt; tt += dt; dist += d }
            if (dist < 0.8 || tt <= 0) continue
            val med = sp.sorted()[sp.size / 2]
            if (med <= 0) continue
            var pauseT = 0.0; var slowN = 0
            for (i in 1 until n) { val dt = (it.ts[i] - it.ts[i - 1]).toDouble().coerceAtLeast(1.0); if (sp[i - 1] < med / 5.0) { pauseT += dt; slowN++ } }
            val doubled = it.word.lowercase().filter { c -> c in 'a'..'z' }.let { w -> (1 until w.length).any { i -> w[i] == w[i - 1] } }
            feats.add(F(pauseT / tt, slowN.toDouble() / n, tt / dist, doubled, distinct(it.key), (byKey[it.key] ?: 0) > 1))
        }
        fun auc(pos: List<Double>, neg: List<Double>): Double {
            if (pos.isEmpty() || neg.isEmpty()) return Double.NaN
            val all = (pos.map { it to 1 } + neg.map { it to 0 }).sortedBy { it.first }
            var rankSum = 0.0; var i = 0
            while (i < all.size) { var j = i; while (j + 1 < all.size && all[j + 1].first == all[i].first) j++; val r = (i + j) / 2.0 + 1.0; for (q in i..j) if (all[q].second == 1) rankSum += r; i = j + 1 }
            return (rankSum - pos.size * (pos.size + 1) / 2.0) / (pos.size.toDouble() * neg.size)
        }
        println("DWELL swipes with timing: ${feats.size}; with a doubled letter: ${feats.count { it.doubled }}; twin-ambiguous: ${feats.count { it.twin }} (doubled among them ${feats.count { it.twin && it.doubled }})")
        for ((name, f) in listOf<Pair<String, (F) -> Double>>("pause share" to { it.pause }, "slow points per point" to { it.slow }, "ms per pitch of path" to { it.tPerPitch })) {
            // same number of distinct letters only: compare each length class and average the AUCs weighted by the smaller group
            var wsum = 0.0; var asum = 0.0
            for (l in 3..8) { val g = feats.filter { it.len == l }; val p = g.filter { it.doubled }.map(f); val q = g.filter { !it.doubled }.map(f); if (p.size >= 20 && q.size >= 20) { val a = auc(p, q); val w = Math.min(p.size, q.size).toDouble(); wsum += w; asum += a * w } }
            val tw = feats.filter { it.twin }; val tp = tw.filter { it.doubled }.map(f); val tq = tw.filter { !it.doubled }.map(f)
            println("DWELL %-24s AUC doubled vs not (same distinct-letter count) %.3f; among twin-ambiguous words %.3f (n %d / %d)".format(name, if (wsum > 0) asum / wsum else Double.NaN, auc(tp, tq), tp.size, tq.size))
        }
    }

    /**
     * R27b: does a word-CLASS language model (grammar learned from the shipped pair data) help glide decoding on REAL sentences? Built here from the shipped list ([ClassModelBuilder]); the decoder uses it only where
     * the exact word pair is unknown. Scored on the swipes that have a word before them (the class model needs context), with the phone parameters of R24b.
     */
    private fun classStudy(its: List<Item>) {
        val t0 = System.currentTimeMillis()
        val cm = ClassModelBuilder.build(lex)
        println("CLASS built in ${System.currentTimeMillis() - t0} ms: ${cm.k} classes over ${(0 until lex.size).count { cm.classOfId(it) >= 0 }} words; first classes:\n" + ClassModelBuilder.describe(lex, cm).lines().take(14).joinToString("\n"))
        val base = GlideParams.forPitchDp(30f)
        val ctxItems = its.filter { it.prev != null }
        val common = (0 until lex.size).sortedByDescending { lex.logProb(it) }.take(5000).toHashSet()
        val ev = ctxItems.filter { it.lexId in common }
        println("CLASS swipes with a word before: ${ctxItems.size} (everyday ${ev.size})")
        for (alpha in listOf(0f, 0.5f, 1f, 2f, 4f)) {
            cm.alpha = alpha
            println("CLASS alpha %.1f: all %s | everyday %s".format(alpha, score(ctxItems, base, true, if (alpha == 0f) null else cm), score(ev, base, true, if (alpha == 0f) null else cm)))
        }
    }

    private fun variants(): List<Pair<String, GlideParams>> = listOf(
        "R29 phone (shipped)" to GlideParams.forPitchDp(keys.pitch, letterW = 0f), "R29 phone letterW .25" to GlideParams.forPitchDp(keys.pitch, letterW = 0.25f),
        "R29 phone letterW .5" to GlideParams.forPitchDp(keys.pitch, letterW = 0.5f), "R29 phone letterW 1" to GlideParams.forPitchDp(keys.pitch, letterW = 1f),
        "lmW .25" to GlideParams(lmW = 0.25f), "lmW 1.0" to GlideParams(lmW = 1.0f),
        "lmA 0.75" to GlideParams(lmA = 0.75f), "lmA 3.0" to GlideParams(lmA = 3.0f),
        "shapeW 1.0" to GlideParams(shapeW = 1.0f), "shapeW 4.0" to GlideParams(shapeW = 4.0f),
        "endW 1.5" to GlideParams(endW = 1.5f), "endW 6" to GlideParams(endW = 6f),
        "visitCap 2" to GlideParams(visitCap = 2f), "visitCap 5" to GlideParams(visitCap = 5f),
        "length off" to GlideParams(lengthW = 0f), "length x2" to GlideParams(lengthW = 2f),
        "len 1.09 s.13/.20" to GlideParams(lengthExpect = 1.09f, lengthSigmaLow = 0.13f, lengthSigmaHigh = 0.20f),
        "len 1.09 s.13/.13" to GlideParams(lengthExpect = 1.09f, lengthSigmaLow = 0.13f, lengthSigmaHigh = 0.13f),
        "len 1.09 s.10/.16" to GlideParams(lengthExpect = 1.09f, lengthSigmaLow = 0.10f, lengthSigmaHigh = 0.16f),
        "len 1.09 s.16/.25" to GlideParams(lengthExpect = 1.09f, lengthSigmaLow = 0.16f, lengthSigmaHigh = 0.25f),
        "len 1.09 s.13/.20 W2" to GlideParams(lengthExpect = 1.09f, lengthSigmaLow = 0.13f, lengthSigmaHigh = 0.20f, lengthW = 2f),
        "len 1.09 s.13/.20 W.5" to GlideParams(lengthExpect = 1.09f, lengthSigmaLow = 0.13f, lengthSigmaHigh = 0.20f, lengthW = 0.5f),
        "len 1.0 s.15/.20" to GlideParams(lengthExpect = 1.0f, lengthSigmaLow = 0.15f, lengthSigmaHigh = 0.20f),
        "len 1.09 s.13/.20 cap6" to GlideParams(lengthExpect = 1.09f, lengthSigmaLow = 0.13f, lengthSigmaHigh = 0.20f, lengthCap = 6f),
        "bias off" to GlideParams(biasGain = 0f), "bias 1.0" to GlideParams(biasGain = 1.0f),
        "bigramMix .6" to GlideParams(bigramMix = 0.6f), "rare off" to GlideParams(rareW = 0f),
        "lmMin .2" to GlideParams(lmMin = 0.2f), "lmMin .3" to GlideParams(lmMin = 0.3f), "lmMin .45" to GlideParams(lmMin = 0.45f),
        "lmMin .3 lmMax 1.6" to GlideParams(lmMin = 0.3f, lmMax = 1.6f), "lmMin .6" to GlideParams(lmMin = 0.6f),
        "shape1 visit5 lmMin .45" to GlideParams(shapeW = 1f, visitCap = 5f, lmMin = 0.45f), "shape1 visit5 lmMin .6" to GlideParams(shapeW = 1f, visitCap = 5f, lmMin = 0.6f),
        "shape1 visit5 lmMin .3" to GlideParams(shapeW = 1f, visitCap = 5f, lmMin = 0.3f),
        "shapeW 0" to GlideParams(shapeW = 0f), "shapeW .5" to GlideParams(shapeW = 0.5f), "shapeW .75" to GlideParams(shapeW = 0.75f),
        "shapeW 1.25" to GlideParams(shapeW = 1.25f), "shapeW 1.5" to GlideParams(shapeW = 1.5f),
        "shape1 visit4" to GlideParams(shapeW = 1f, visitCap = 4f), "shape1 visit5" to GlideParams(shapeW = 1f, visitCap = 5f), "shape1 visit6" to GlideParams(shapeW = 1f, visitCap = 6f),
        "shape1 visit5 biasW1.5" to GlideParams(shapeW = 1f, visitCap = 5f, biasW = 1.5f),
        "shape1 end2" to GlideParams(shapeW = 1f, endW = 2f), "shape1 end4" to GlideParams(shapeW = 1f, endW = 4f),
        "shape1 biasW 1.5" to GlideParams(shapeW = 1f, biasW = 1.5f), "shape1 biasW 6" to GlideParams(shapeW = 1f, biasW = 6f),
        "shape1 biasGain .9" to GlideParams(shapeW = 1f, biasGain = 0.9f),
        "shape1 len1.09" to GlideParams(shapeW = 1f, lengthExpect = 1.09f, lengthSigmaLow = 0.13f, lengthSigmaHigh = 0.20f),
        "shape1 visit5 len1.09" to GlideParams(shapeW = 1f, visitCap = 5f, lengthExpect = 1.09f, lengthSigmaLow = 0.13f, lengthSigmaHigh = 0.20f),
        "shape.75 visit5 len1.09" to GlideParams(shapeW = 0.75f, visitCap = 5f, lengthExpect = 1.09f, lengthSigmaLow = 0.13f, lengthSigmaHigh = 0.20f),
        "shape1 visit5 len1.09 end2" to GlideParams(shapeW = 1f, visitCap = 5f, endW = 2f, lengthExpect = 1.09f, lengthSigmaLow = 0.13f, lengthSigmaHigh = 0.20f)
    )

    /**
     * R32e (owner 2026-11-24: "make it better at not predicting rare words such as fur or ave when I'm typing common words like for and and"). What the shipped decoder does with real swipes when its first choice is much RARER (in the
     * contextual prior the decoder itself uses) than another of its three guesses, and what "prefer the common word when the scores are close" would do: for each rule (the first choice is at least G nats less probable than a
     * guess whose score is within M cost units) how many glides flip, how many of them were mended (the common guess is what was meant) and how many spoiled (the rare first choice was right). Also the confusions of the owner's words.
     */
    private fun rareStudy(its: List<Item>) {
        val dec = GlideDecoder(keys, GlideParams.forPitchDp(keys.pitchDp))
        class Row(val word: String, val key: String, val c: List<GlideCandidate>, val everyday: Boolean)
        val top = (0 until lex.size).sortedByDescending { lex.logProb(it) }.take(5000).toHashSet()
        val rows = ArrayList<Row>()
        for (it in its) { val c = dec.decode(it.xs, it.ys, it.xs.size, listOf(lex), it.prev, 3); if (c.isNotEmpty()) rows.add(Row(it.word, it.key, c, it.lexId in top)) }
        fun ok(c: GlideCandidate, r: Row) = String(GlideLetters.keyOf(c.text)) == r.key
        println("RARE rows=${rows.size} everyday=${rows.count { it.everyday }} top1=%.1f%% top3=%.1f%%".format(100.0 * rows.count { ok(it.c[0], it) } / rows.size, 100.0 * rows.count { r -> r.c.any { ok(it, r) } } / rows.size))
        for (scope in listOf("all", "everyday")) {
            val rs = if (scope == "all") rows else rows.filter { it.everyday }
            for (g in listOf(2f, 3f, 4f, 6f)) {
                val line = StringBuilder("RARE $scope G=$g n=${rs.size}:")
                for (m in listOf(0.5f, 1f, 2f, 3f, 5f, 8f)) {
                    var mended = 0; var spoiled = 0; var flips = 0
                    for (r in rs) {
                        val c0 = r.c[0]; var pick = 0
                        for (j in 1 until r.c.size) { val cj = r.c[j]; if (cj.prior - r.c[pick].prior >= g && c0.score - cj.score <= m) pick = j }
                        if (pick != 0) { flips++; val cp = r.c[pick]; if (ok(cp, r)) { if (!ok(c0, r)) mended++ } else if (ok(c0, r)) spoiled++ }
                    }
                    line.append("  M=$m flips=$flips +$mended -$spoiled (net ${mended - spoiled})")
                }
                println(line)
            }
        }
        val mine = listOf("and", "for", "the", "or", "to", "of", "in", "it", "is", "that", "you", "was")
        for (w in mine) {
            val rs = rows.filter { it.word == w }; if (rs.isEmpty()) continue
            val wrong = rs.filter { !ok(it.c[0], it) }.groupingBy { it.c[0].text }.eachCount().entries.sortedByDescending { it.value }.take(6)
            println("RARE word '$w': n=${rs.size} first=%.1f%% top3=%.1f%%; first choice wrong -> ".format(100.0 * rs.count { ok(it.c[0], it) } / rs.size, 100.0 * rs.count { r -> r.c.any { ok(it, r) } } / rs.size) + wrong.joinToString(" ") { it.key + ":" + it.value })
        }
        println("RARE ln prior of owner's words in this list: " + listOf("and", "for", "ave", "fur", "fir", "access", "the", "there", "or", "our", "out", "make", "maggie").joinToString("  ") { w -> w + "=" + lex.idOf(w).let { id -> if (id < 0) "n/a" else "%.1f".format(lex.logProb(id)) } })
    }

    /** Where the real misses come from: words the list lacks, candidate recall, ranking misses (more / less common than the word meant), examples. */
    private fun diagnose(sw: List<RealSwipe.Swipe>, its: List<Item>) {
        val unknown = HashMap<String, Int>(); var unk = 0
        for (s in sw) { val l = RealSwipe.letters(s.word) ?: continue; val w = clean(s.word); if (lex.idOf(w) < 0 && lex.idOf(l) < 0 && distinct(l) >= 2) { unknown.merge(w, 1, Int::plus); unk++ } }
        println("DIAG words not in the list: $unk of ${sw.size} swipes; most common: " + unknown.entries.sortedByDescending { it.value }.take(40).joinToString(" ") { it.key + ":" + it.value })
        val dec = GlideDecoder(keys, GlideParams())
        var n = 0; var miss1 = 0; var notInTop3 = 0; var notSurvived = 0; var moreCommon = 0; var lessCommon = 0; var sameEnds = 0
        val byLen = HashMap<Int, IntArray>(); val shown = ArrayList<String>()
        for (it in its) {
            val c = dec.decode(it.xs, it.ys, it.xs.size, listOf(lex), it.prev, 3)
            n++
            val l = distinct(it.key).coerceAtMost(8); val a = byLen.getOrPut(l) { IntArray(3) }; a[0]++
            val first = c.firstOrNull()
            val hit1 = first != null && String(GlideLetters.keyOf(first.text)) == it.key
            val inTop3 = c.any { x -> String(GlideLetters.keyOf(x.text)) == it.key }
            if (hit1) continue
            miss1++; a[1]++
            val surv = it.word in dec.survivorTexts(listOf(lex))
            if (!inTop3) { notInTop3++; a[2]++ }
            if (!surv) notSurvived++
            if (first != null) {
                val gid = lex.idOf(first.text)
                if (gid >= 0) { val pm = lex.logProb(it.lexId); val pg = lex.logProb(gid); if (pg > pm) moreCommon++ else lessCommon++ }
                if (first.text.first() == it.word.first() && first.text.last() == it.word.last()) sameEnds++
            }
            if (shown.size < 60) shown.add(it.word + " -> " + c.joinToString("/") { x -> x.text } + (if (surv) "" else " (never a candidate)") + (if (it.prev != null) "  after '${it.prev}'" else ""))
        }
        println("DIAG n=$n first-choice misses $miss1; right word NOT in top 3: $notInTop3; not even among the careful-pass candidates: $notSurvived")
        println("DIAG of the first-choice misses: the word chosen is MORE common than the meant one: $moreCommon, LESS common: $lessCommon; same first and last letter as the meant word: $sameEnds")
        println("DIAG by distinct letters (n / first-choice miss % / not in top 3 %): " + byLen.toSortedMap().entries.joinToString("  ") { (l, a) -> "len$l:${a[0]}/%.0f/%.0f".format(100.0 * a[1] / a[0], 100.0 * a[2] / a[0]) })
        for (m in shown) println("DIAG miss: $m")
        shortLong(its)
    }
}
