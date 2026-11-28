package app.kestrel.core.input.glide

/**
 * R40 (owner 2026-11-28: "Sometimes the keyboard corrects a prior word to a rare word that makes no sense in context. There should be weights so that words like and make the are not replaced with words like ave age access
 * Maggie", and "when i typed 'to it' the keyboard retroactively changed it to 'yo it'. That is ridiculous"). The WEIGHT RULE for every change the keyboard makes, without being asked, to a word that is already on the screen
 * (today: the post-correction of R26a, [GlideSession.revisePrevious]). The person SAW the old word and left it: overturning it needs more than a slightly better score, and the commoner the old word is, the more.
 *
 * Weights are ln-probabilities on the scale of the shipped word list (the word's share, plus what this person writes and wrote lately: [GlideTyping.contextLn] with no word before): "the" -3.0, "to" -3.6, "make" -6.7,
 * "age" -8.5, "ave" -11.2, "yo" -12.4. A change old -> new is allowed when
 *  A. new is not more than [allowedDrop] nats below old. The allowance GROWS as the old word gets rarer: an everyday word can only be traded for one almost as common ([MIN_DROP], about 4.5 times), a rare word may be traded
 *     for a rarer one too ([MAX_DROP]); a commoner or equally common new word always passes; or
 *  B. the NEXT word proves it by what PRINTED ENGLISH says: in the shipped list of the 243 000 commonest word pairs, "next after new" is at least [PAIR_GAIN] nats more likely than "next after old" (a pair that is not in the list
 *     counts as [NO_PAIR]: "there are" -1.5 against "the are" -11.5), and new is not more than [BACKED_DROP] below old. Only the shipped list counts here: a pair the PERSON made once, or one the class model guessed, is how a
 *     rare look-alike with one known follower ("yo it") used to beat "to it", and it is never proof.
 * Measured on 3 011 real swipes (research/R40-correction-policies.md): rule B keeps the mended words that rule A alone would lose. Pure functions: no state, so the post-correction, the tests and the real-swipe lab apply
 * exactly the same rule.
 */
object CorrectionGuard {
    /** The smallest allowance: even the commonest word may be traded for one this many nats (about 4.5 times) rarer. */
    const val MIN_DROP = 1.5f
    /** The largest allowance of rule A (about 90 times rarer): the word being replaced is itself rare. */
    const val MAX_DROP = 4.5f
    /** Allowance per nat the old word is rarer than [BASE]: half a nat. */
    const val SLOPE = 0.5f
    /** At and above this ln-share (the commonest few hundred words) the allowance is the minimum. */
    const val BASE = -3f
    /** Rule B: the most nats rarer (about 250 times) the new word may be when the shipped pairs prove it. */
    const val BACKED_DROP = 5.5f
    /** Rule B: how many nats more likely the shipped list must find the next word after the new word than after the old one. */
    const val PAIR_GAIN = 5f
    /** What a pair that is not in the shipped list counts as (the list's weakest pair is about -12.6). */
    const val NO_PAIR = -13f

    /** How many nats rarer than [oldLn] a word may be and still replace it by rule A. */
    fun allowedDrop(oldLn: Float, minDrop: Float = MIN_DROP, slope: Float = SLOPE, maxDrop: Float = MAX_DROP): Float = (slope * (BASE - oldLn)).coerceIn(minDrop, maxDrop)

    /**
     * True when changing a word of weight [oldLn] into one of weight [newLn] is allowed. [oldPairLn] / [newPairLn]: ln P(the word that follows | the word) in the SHIPPED list only ([GlideTyping.pairLn]); NaN = not in the list.
     */
    fun allows(oldLn: Float, newLn: Float, oldPairLn: Float, newPairLn: Float, minDrop: Float = MIN_DROP, backedDrop: Float = BACKED_DROP, pairGain: Float = PAIR_GAIN): Boolean {
        val drop = oldLn - newLn
        if (drop <= allowedDrop(oldLn, minDrop)) return true
        if (newPairLn.isNaN() || drop > backedDrop) return false
        val old = if (oldPairLn.isNaN()) NO_PAIR else oldPairLn
        return newPairLn - old >= pairGain
    }
}
