package app.kestrel.core.input.glide

/**
 * R30: the trained swipe network ([ShapeNet]) as a SECOND OPINION on the shipped decoder's answer.
 *
 * The shipped decoder gives its best words with a score (higher = better; its geometric cost, the language prior, what this person has taught it). The network reads the SAME touch path and says how
 * likely each of those words is as a path (a CTC log-likelihood). The hybrid is `score + weight * logP`: measured on three disjoint sets of REAL swipes (research/R30-trained-shape-model.md) it lifts the
 * first choice by about 3.5 to 4.5 points and cuts "short word read as a much longer one" by about two thirds, while the network alone is only as good as the shipped decoder: it is the two views together that win.
 *
 * It only runs when the shipped answer is NOT clearly ahead ([gate]: first score minus second score below it): about every second glide, which keeps the whole gain (measured) and halves the cost on a slow tablet.
 * Not thread safe (the network keeps scratch buffers): one engine thread uses one instance.
 *
 * The path is handed to the network on the box round the 26 letter keys (the canvas the network was trained on is a keyboard's letter area, normalised to 0..1 on both axes).
 */
class ShapeRescorer(private val net: ShapeNet, keys: GlideKeys, private val weight: Float = DEFAULT_WEIGHT, private val gate: Float = DEFAULT_GATE) {
    private val left: Float; private val top: Float; private val w: Float; private val h: Float
    private val keyXY = FloatArray(52)

    init {
        left = keys.cx.min() - keys.keyW / 2f; top = keys.cy.min() - keys.keyH / 2f
        w = keys.cx.max() + keys.keyW / 2f - left; h = keys.cy.max() + keys.keyH / 2f - top
        require(w > 0f && h > 0f) { "the letter keys have no area" }
        for (k in 0 until 26) { keyXY[2 * k] = (keys.cx[k] - left) / w; keyXY[2 * k + 1] = (keys.cy[k] - top) / h }
    }

    /** [candidates] in the order the network left them (best first), [ran]: the network was asked at all, [changedFirst]: another word is first now, [millis]: the time it took. */
    class Outcome(val candidates: List<GlideCandidate>, val ran: Boolean, val changedFirst: Boolean, val millis: Long)

    /**
     * Re-rank [cands] (the shipped decoder's list, best first) with the path of [count] touch points [xs] / [ys] in view pixels and [ts] in milliseconds (any origin; only the differences count).
     * Returns [cands] unchanged (ran = false) when the shipped answer is clearly ahead, when there are fewer than two words, or when the path carries no usable timing.
     */
    fun rescore(xs: FloatArray, ys: FloatArray, ts: FloatArray, count: Int, cands: List<GlideCandidate>): Outcome {
        val none = Outcome(cands, false, false, 0L)
        if (cands.size < 2 || count < 4 || xs.size < count || ys.size < count || ts.size < count) return none
        if (cands[0].score - cands[1].score >= gate) return none
        val t0 = System.nanoTime()
        val nx = FloatArray(count) { (xs[it] - left) / w }; val ny = FloatArray(count) { (ys[it] - top) / h }
        val tr = ShapeNet.trajectory(nx, ny, ts, count) ?: return none
        val em = net.emissions(tr, keyXY, 26)
        val scored = cands.map { c ->
            val word = letters(c.text)
            val lp = if (word.isEmpty()) FLOOR else ShapeNet.ctcLogP(em, 26, word).coerceAtLeast(FLOOR)
            GlideCandidate(c.text, c.score + weight * lp, c.cost, c.prior, c.letters)
        }
        val out = scored.sortedByDescending { it.score }                    // (stable: equal scores keep the shipped order)
        return Outcome(out, true, out[0].text != cands[0].text, (System.nanoTime() - t0) / 1_000_000)
    }

    private fun letters(t: String): IntArray {
        var n = 0; for (c in t) if (c in 'a'..'z') n++
        val a = IntArray(n); var i = 0; for (c in t) if (c in 'a'..'z') a[i++] = c - 'a'
        return a
    }

    companion object {
        /** Picked on the first slice of real swipes, confirmed on two others (a flat optimum between 0.35 and 2). */
        const val DEFAULT_WEIGHT = 0.7f
        /** Where the shipped first choice is ahead of the second by at least this (score units, about ln) the network is not asked: the measured median of the margin is 3.4, 25 % of glides are below 1.95. */
        const val DEFAULT_GATE = 3.5f
        /** A word the network finds (almost) impossible is capped here so one number can never swamp the sum. */
        private const val FLOOR = -300f
    }
}
