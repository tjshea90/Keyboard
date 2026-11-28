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
 *  B. the NEXT word proves it: the pair (old, next) is not in the shipped list (the old word cannot be followed by it; "the are") and the pair (new, next) is ("there are"), and new is not more than [BACKED_DROP] below old.
 *     The shipped list holds the 243 000 commonest word pairs of printed English, so a pair in it has real support; a pair the PERSON made once (the personal model) or the class model guessed does not count.
 * Measured on 3 011 real swipes (research/R40-correction-policies.md): rule B keeps every mended word of the unguarded post-correction that rule A alone would lose, and a sparse pair for a rare look-alike ("yo it") passes neither.
 * Pure functions: no state, so the post-correction, the tests and the real-swipe lab apply exactly the same rule.
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
    /** Rule B: the most nats rarer (about 250 times) the new word may be when the next word proves it. */
    const val BACKED_DROP = 5.5f

    /** How many nats rarer than [oldLn] a word may be and still replace it by rule A. */
    fun allowedDrop(oldLn: Float, minDrop: Float = MIN_DROP, slope: Float = SLOPE, maxDrop: Float = MAX_DROP): Float = (slope * (BASE - oldLn)).coerceIn(minDrop, maxDrop)

    /**
     * True when changing a word of weight [oldLn] into one of weight [newLn] is allowed. [newPairInList] / [oldPairInList]: the pair (word, the word that follows) is in the shipped list ([GlideTyping.pairInList]).
     */
    fun allows(oldLn: Float, newLn: Float, newPairInList: Boolean, oldPairInList: Boolean, minDrop: Float = MIN_DROP, backedDrop: Float = BACKED_DROP): Boolean {
        val drop = oldLn - newLn
        if (drop <= allowedDrop(oldLn, minDrop)) return true
        return newPairInList && !oldPairInList && drop <= backedDrop
    }
}
