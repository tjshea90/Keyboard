package app.kestrel.core.input.glide

/**
 * R32e (owner 2026-11-24: "make it better at not predicting rare words such as fur or ave when I'm typing common words like for and and"). The last word of the glide pipeline, after the decoder and the trained shape
 * network have ordered their guesses: when the FIRST guess is a word that is hundreds of times rarer than another guess ("ave" at ln -11 against "and" at -4: [GAP] nats) and that other guess fits the finger path almost
 * as well (its score is within [MARGIN] of the first's), the common word goes first. The rare word stays among the guesses, so one tap on the strip still gets it.
 *
 * "Rare" is judged on the contextual prior the decoder already computed ([GlideCandidate.prior]: the word's share in the list, plus what this person has typed and glided of it, mixed with the word before). So a rare word the
 * person really uses (a name, a street) is not rare here, and the guard cannot fight what has been learned. Measured on real swipes (research/R32-punctuation-suggestions.md section 6): on everyday words it mends
 * several glides for each one it spoils; on encyclopaedia text full of names it is about even, which is why the margin is modest.
 */
object CommonGuard {
    /** The first guess must be at least this many nats (ln) less probable than the one that replaces it: about 400 times rarer. */
    const val GAP = 6f
    /** The replacing guess may trail the first by at most this many score units. */
    const val MARGIN = 3f
    /** Only the guesses the keyboard keeps (three shown, two more) are looked at. */
    const val DEPTH = 5

    /** [c] best first; the same list, or the same guesses with one common word moved to the front. */
    fun apply(c: List<GlideCandidate>, gap: Float = GAP, margin: Float = MARGIN): List<GlideCandidate> {
        if (c.size < 2) return c
        val first = c[0]
        var pick = -1
        for (j in 1 until minOf(c.size, DEPTH)) {
            val x = c[j]
            if (x.prior - first.prior >= gap && first.score - x.score <= margin && (pick < 0 || x.score > c[pick].score)) pick = j
        }
        if (pick < 0) return c
        val out = ArrayList<GlideCandidate>(c.size)
        out.add(c[pick])
        for (i in c.indices) if (i != pick) out.add(c[i])
        return out
    }
}
