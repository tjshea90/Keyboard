package app.kestrel.core.input.glide

/**
 * Owner R20d (2026-11-18): "I want it to be very good at predicting my glide typing." The decoder already learns ONE shift of the whole path (where the thumb lands beside the key it aims at). A thumb does not land the same
 * way everywhere: the left thumb of a two-thumb typist and the right thumb land differently, and a one-handed thumb falls short at the far side of the keyboard. This is a small map of those differences: a grid of
 * [cols] x [rows] offsets (in key pitches) over the letter keys, blended smoothly (bilinear) between the grid points, learned a little at a time from the words the person keeps, on top of the single shift.
 * 18 numbers, nothing allocated while decoding, pure Kotlin. Coordinates are normalised to the letter keys ([u] across, [v] down, both 0..1), so the map survives a change of the keyboard's size or shape.
 */
class TouchField(val cols: Int = COLS, val rows: Int = ROWS) {
    val dx = FloatArray(cols * rows)
    val dy = FloatArray(cols * rows)
    /** How many letters it has learned from since it was created or cleared (it only starts to correct once it has a few). */
    var seen = 0; private set

    val isEmpty: Boolean get() = seen == 0

    /** The offset at ([u], [v]) in [out] (x, y), in pitches. */
    fun at(u: Float, v: Float, out: FloatArray) {
        val gu = u.coerceIn(0f, 1f) * (cols - 1); val gv = v.coerceIn(0f, 1f) * (rows - 1)
        val i0 = Math.min(gu.toInt(), cols - 2); val j0 = Math.min(gv.toInt(), rows - 2)
        val fu = gu - i0; val fv = gv - j0
        val a = j0 * cols + i0; val b = a + 1; val c = a + cols; val d = c + 1
        val wa = (1f - fu) * (1f - fv); val wb = fu * (1f - fv); val wc = (1f - fu) * fv; val wd = fu * fv
        out[0] = dx[a] * wa + dx[b] * wb + dx[c] * wc + dx[d] * wd
        out[1] = dy[a] * wa + dy[b] * wb + dy[c] * wc + dy[d] * wd
    }

    /**
     * One observation: at ([u], [v]) the path (already corrected by this map) was ([ex], [ey]) pitches off the key it was matched to, after the whole word's mean was taken away (the single shift learns the mean). The four grid
     * points around it move [rate] of the way, in proportion to how close each is. A wild observation (more than [CLIP] pitches off: a letter the matching put in the wrong place) is cut down, not trusted.
     */
    fun learn(u: Float, v: Float, ex: Float, ey: Float, rate: Float) {
        val gu = u.coerceIn(0f, 1f) * (cols - 1); val gv = v.coerceIn(0f, 1f) * (rows - 1)
        val i0 = Math.min(gu.toInt(), cols - 2); val j0 = Math.min(gv.toInt(), rows - 2)
        val fu = gu - i0; val fv = gv - j0
        val cx = ex.coerceIn(-CLIP, CLIP); val cy = ey.coerceIn(-CLIP, CLIP)
        val a = j0 * cols + i0
        step(a, (1f - fu) * (1f - fv), cx, cy, rate); step(a + 1, fu * (1f - fv), cx, cy, rate)
        step(a + cols, (1f - fu) * fv, cx, cy, rate); step(a + cols + 1, fu * fv, cx, cy, rate)
        seen++
    }
    private fun step(i: Int, w: Float, ex: Float, ey: Float, rate: Float) {
        dx[i] = (dx[i] * (1f - LEAK * w) + rate * w * ex).coerceIn(-MAX, MAX)
        dy[i] = (dy[i] * (1f - LEAK * w) + rate * w * ey).coerceIn(-MAX, MAX)
    }

    fun copyFrom(o: TouchField) { if (o.cols == cols && o.rows == rows) { System.arraycopy(o.dx, 0, dx, 0, dx.size); System.arraycopy(o.dy, 0, dy, 0, dy.size); seen = o.seen } }
    fun clear() { dx.fill(0f); dy.fill(0f); seen = 0 }

    /** The map as numbers for the private file: dx then dy, and the count of letters it learned from last. */
    fun toArray(): FloatArray { val a = FloatArray(2 * dx.size + 1); System.arraycopy(dx, 0, a, 0, dx.size); System.arraycopy(dy, 0, a, dx.size, dy.size); a[a.size - 1] = seen.toFloat(); return a }
    /** True when [a] had the right length and was taken. */
    fun fromArray(a: FloatArray): Boolean {
        if (a.size != 2 * dx.size + 1 || a.any { it.isNaN() || it.isInfinite() }) return false
        for (i in dx.indices) { dx[i] = a[i].coerceIn(-MAX, MAX); dy[i] = a[dx.size + i].coerceIn(-MAX, MAX) }
        seen = a[a.size - 1].toInt().coerceAtLeast(0); return true
    }

    companion object {
        const val COLS = 3
        const val ROWS = 3
        /** No grid point moves further than this from zero (pitches). */
        const val MAX = 0.5f
        /** One observation counts for at most this much (pitches). */
        const val CLIP = 0.8f
        /** A tiny pull back to zero per observation, so a habit the person gave up is forgotten in a few hundred words. */
        const val LEAK = 0.002f
    }
}
