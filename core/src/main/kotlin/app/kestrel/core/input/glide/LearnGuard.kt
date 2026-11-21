package app.kestrel.core.input.glide

/**
 * R29 (owner 2026-11-21: "it still types the word access a lot when i glide the word and ... fur when i glide the word for, Maggie when I try to type make, our or out when i try to type or").
 *
 * A glide that is KEPT (the next thing typed is not Backspace) teaches the keyboard the word, the pair, the glide's shape and where the thumb lands. That is right when the word was right and WRONG when
 * it was a look-alike that nobody noticed or bothered to fix: the wrong word then gets one more use, its shape gets one more confirmation and the thumb habit moves towards it, so the next glide
 * of the real word is a little more likely to give the wrong one again. A loop, and the owner's words ("a lot") are what a loop looks like.
 *
 * The signal that this is the loop and not honest learning is in the shadow decode (R27a: the same glide through a decoder that learned nothing): the plain decoder would have given ANOTHER word first,
 * and that other word is much commoner in the shipped list ([GAP_NATS]: the word learning chose is at least 20 times rarer). A rare word that wins against a common one only because of what was learned is
 * exactly the "and -> access" shape. Such a glide is learned only [SELF_WEIGHT] as much and teaches neither the shape nor the thumb habit. Anything the person DID (picked it in the strip, deleted and
 * retyped, a retry after Backspace) is never touched: those are explicit signals and keep their weight.
 */
object LearnGuard {
    /** ln of how many times commoner the plain decoder's word must be (shipped list) before a kept learned word counts as circular: ln 20. */
    const val GAP_NATS = 3.0f
    /** What a kept circular glide teaches, as a share of a normal one. */
    const val SELF_WEIGHT = 0.25f

    class Verdict(val weight: Float, val shape: Boolean, val habit: Boolean, val circular: Boolean)

    /**
     * [plain]: what a decoder that learned nothing gave first, [learned]: what the keyboard gave first, [gap]: ln P(plain word) - ln P(learned word) in the shipped list, [kept]: the word that stayed in
     * the text, [weight]: 1 = just kept, more = the person chose it, [afterUndo]: it replaced a deleted word, [revised]: the next word's context changed it.
     */
    fun judge(plain: String?, learned: String?, gap: Float, kept: String, weight: Float, afterUndo: Boolean, revised: Boolean): Verdict {
        val circular = plain != null && learned != null && plain != kept && learned == kept && gap >= GAP_NATS && weight <= 1f && !afterUndo && !revised
        return if (circular) Verdict(weight * SELF_WEIGHT, shape = false, habit = false, circular = true) else Verdict(weight, shape = true, habit = true, circular = false)
    }
}
