package app.kestrel.core.input.glide

/**
 * One glided word that STAYED in the text, as the memory has to hear about it (what [GlideLearning.confirm] needs to know). [weight] is 1 for a word that was only kept and 3 for one the person picked from the strip;
 * [pickedOver] is the first guess a picked word replaced; [afterUndo] / [deleted]: a glided word was deleted and this one came instead; [revised]: the next glide swapped it in (the keyboard's own second guess);
 * [serial] names the decode that gave it (0 = unknown); [shown] is how it stands in the text when the person chose its capitals; [startsSentence]: it began a sentence.
 */
class KeptGlide(
    val word: String,
    val prev: String?,
    val prev2: String?,
    val weight: Float = 1f,
    val pickedOver: String? = null,
    val afterUndo: Boolean = false,
    val deleted: String? = null,
    val revised: Boolean = false,
    val serial: Int = 0,
    val shown: String? = null,
    val startsSentence: Boolean = false,
)

/**
 * R40-6 (owner 2026-11-28: "make the browser app always use the latest version of swoop with everything it learned so that the built in keyboard in the browser is identical at all times to swoop and everything it learned").
 * THE rule for what a glided word that stays in the text teaches the keyboard's memory, in ONE place: Swoop ([GlideSession.confirmPrevious]) and the Kestrel browser's own small keyboard both call it, so the memory
 * of the two can never grow by different rules. (Before this, the browser taught every kept glide at full weight: the loop that let a rare look-alike such as "ave", "access", "yo" or "Maggie" feed itself, which R29 and
 * R33 had closed in Swoop only.)
 *  - R29 [LearnGuard]: a wrong look-alike that was only KEPT must not feed itself: learned lightly, no shape, no thumb habit;
 *  - R33 [GlideTyping.keepTrust]: a glide that was only kept (not picked, not retyped) is weak evidence: the rarer the shipped list finds the word, the lighter the lesson;
 *  - R33: when the person picked another word over the first guess, what that guess had earned is taken back ([UNLEARN_WRONG] uses);
 *  - R40: a word the next glide swapped in counts like a kept one, never as a choice of the person.
 * Nothing is learned where the field says so ([learnAllowed] false: a password, an incognito request, a private tab); the counters still count.
 */
object GlideLearning {
    /** R33: what a wrong first guess had earned is taken back, this many uses, when the person picks another word over it. */
    const val UNLEARN_WRONG = 2f

    /** [habitChanged] gets the new thumb habit when a kept glide moved it (the caller saves it). */
    fun confirm(g: GlideTyping, k: KeptGlide, learnAllowed: Boolean, habitChanged: (FloatArray) -> Unit) {
        val w = k.word
        g.stats.onKept()
        if (!k.revised && k.weight == 1f && !k.afterUndo) g.shadowFor(k.serial)?.let { if (it.plain != it.learned && it.learned == w) g.stats.onShadowLearnedKept() }
        val sh = g.shadowFor(k.serial)
        val verdict = LearnGuard.judge(sh?.plain, sh?.learned, sh?.gap ?: 0f, w, maxOf(k.weight, if (k.afterUndo) 3f else 1f), k.afterUndo, k.revised)
        if (verdict.circular) g.stats.onGuarded()
        val trusted = k.weight > 1f || k.afterUndo
        val trust = if (trusted) 1f else g.keepTrust(w)
        val gentle = trust < 0.5f
        if (gentle) g.stats.onRareKept()
        if (verdict.habit && !gentle && learnAllowed) g.accepted(w) { h -> habitChanged(h) }
        if (learnAllowed) {
            k.shown?.let { g.learnCase(it) }
            if (k.startsSentence) g.learnStart(w)
            // kept: the keyboard hears it (a word the person chose, or one that replaced a deleted one, counts more); a correction remembers what it was picked over (a pick) or put in the place of (a deletion)
            val guessed = k.pickedOver ?: if (k.afterUndo) k.deleted else null
            k.pickedOver?.let { wrong -> if (!wrong.equals(w, ignoreCase = true)) g.unlearnWord(k.prev, wrong, UNLEARN_WRONG, k.prev2) }
            g.learn(k.prev, w, verdict.weight * trust, k.prev2, guessed?.takeIf { it != w }, if (k.pickedOver != null) 1f else 0.5f, fromGlide = verdict.shape && !gentle, afterUndo = k.afterUndo)
        }
    }
}
