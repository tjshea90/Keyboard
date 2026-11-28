package app.kestrel.core.input.glide

/**
 * R40 (owner 2026-11-28: "Sometimes the keyboard corrects a prior word to a rare word that makes no sense in context. There should be weights so that words like and make the are not replaced with words like ave age access
 * Maggie", and "when i typed 'to it' the keyboard retroactively changed it to 'yo it'. That is ridiculous"). The WEIGHT RULE for every change the keyboard makes to a word that is already on the screen without being asked:
 * how common the old word is says how much rarer the new one may be.
 *
 * Weights are ln-probabilities on the scale of the shipped word list (the word's share, plus what this person writes and wrote lately: [GlideTyping.contextLn] with no word before): "the" -3.0, "to" -3.6, "make" -6.7,
 * "age" -8.5, "ave" -11.2, "yo" -12.4. An automatic rewrite old -> new is allowed only when new is not more than [allowedDrop] nats below old. The allowance GROWS as the old word gets rarer: an everyday word can only be
 * traded for one almost as common ([MIN_DROP], about 4.5 times), a rare word may be traded for something much commoner (a drop below zero is always fine) and, if it really is a slip, for a rarer one too ([MAX_DROP]).
 * Pure functions: no state, so the post-correction, the tests and the real-swipe lab apply exactly the same rule.
 */
object CorrectionGuard {
    /** The smallest allowance: even the commonest word may be traded for one this many nats (about 4.5 times) rarer. */
    const val MIN_DROP = 1.5f
    /** The largest allowance (about 90 times rarer): the word being replaced is itself rare. */
    const val MAX_DROP = 4.5f
    /** Allowance per nat the old word is rarer than [BASE]: half a nat. */
    const val SLOPE = 0.5f
    /** Below this ln-share (about the commonest thousand words and up) the allowance is the minimum. */
    const val BASE = -3f

    /** How many nats rarer than [oldLn] a word may be and still replace it without being asked. */
    fun allowedDrop(oldLn: Float): Float = (SLOPE * (-oldLn - BASE)).coerceIn(MIN_DROP, MAX_DROP)

    /** True when a change from a word of weight [oldLn] to a word of weight [newLn] is allowed by weight alone (a commoner or equally common new word always is). */
    fun allows(oldLn: Float, newLn: Float): Boolean = oldLn - newLn <= allowedDrop(oldLn)
}
