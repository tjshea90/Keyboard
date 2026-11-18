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

    class Item(val word: String, val key: String, val prev: String?, val xs: FloatArray, val ys: FloatArray, val lexId: Int)

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
            out.add(Item(lex.text(id), String(GlideLetters.keyOf(lex.text(id))), prev?.takeIf { lex.idOf(it) >= 0 }, xs, ys, id))
        }
        return out
    }

    private fun distinct(s: String): Int { var m = 0; var p = ' '; for (c in s) if (c != p) { m++; p = c }; return m }

    class Score(val n: Int, val top1: Int, val top3: Int) { fun pct(x: Int) = 100.0 * x / n; override fun toString() = "n=%d top1=%.1f%% top3=%.1f%%".format(n, pct(top1), pct(top3)) }

    fun score(items: List<Item>, p: GlideParams, context: Boolean): Score {
        val dec = GlideDecoder(keys, p)
        var t1 = 0; var t3 = 0
        for (it in items) {
            val c = dec.decode(it.xs, it.ys, it.xs.size, listOf(lex), if (context) it.prev else null, 3)
            if (c.isNotEmpty() && String(GlideLetters.keyOf(c[0].text)) == it.key) t1++
            if (c.any { x -> String(GlideLetters.keyOf(x.text)) == it.key }) t3++
        }
        return Score(items.size, t1, t3)
    }

    @Test fun replay() {
        assumeTrue("set REAL_SWIPES=1", System.getenv("REAL_SWIPES") == "1")
        val f = File(System.getenv("REAL_SWIPES_FILE") ?: "build/real-swipes.jsonl")
        if (!f.exists()) println("fetched " + RealSwipe.fetch(System.getenv("REAL_SWIPES_URL") ?: RealSwipe.DEV_URL, (System.getenv("REAL_SWIPES_MB")?.toLongOrNull() ?: 20L) * 1_000_000, f) + " bytes")
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
        val want = (System.getenv("REAL_VARIANTS") ?: "none").split(',').map { it.trim() }
        val vit = its.take(System.getenv("REAL_VARIANT_N")?.toIntOrNull() ?: 2500)
        if (want != listOf("none")) {
            println("variants on the first ${vit.size} swipes (shipped on the same swipes: no ctx ${score(vit, base, false)} | ctx ${score(vit, base, true)})")
            for ((name, p) in variants()) if (want == listOf("all") || name in want) println("%-34s no ctx %s | ctx %s".format(name, score(vit, p, false), score(vit, p, true)))
        }
    }

    private fun variants(): List<Pair<String, GlideParams>> = listOf(
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
        "bigramMix .6" to GlideParams(bigramMix = 0.6f), "rare off" to GlideParams(rareW = 0f)
    )
}
