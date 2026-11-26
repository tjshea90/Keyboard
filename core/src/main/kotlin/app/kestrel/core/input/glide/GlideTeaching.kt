package app.kestrel.core.input.glide

/**
 * R36 (owner 2026-11-26: "make a much longer glide training that I can glide full paragraphs and problematic words several times and the keyboard learns how I glide"): what ONE glide of a KNOWN word teaches the
 * keyboard about this person's hand.
 *
 * A glide that was merely kept in everyday typing is weak evidence (the keyboard chose the word, not the person), so the keyboard learns from it gently and guards against feeding on its own mistakes (LearnGuard,
 * keepTrust). On the Glide training screen the word was ASKED FOR, so the lesson is certain and is given in full - but only the part that is about the HAND:
 *  - where the thumb lands: the single shift and the touch plane ([GlideDecoder.learnFrom], the same move as for a kept glide);
 *  - what this word looks like when this person draws it: the shape memory ([PatternMemory]), with the weights of a CORRECTION (a pick from the strip) when the keyboard put another word first and those of a
 *    kept glide when it was right; every other word that was drawn like this becomes less sure;
 *  - which word the keyboard put in its place ([PersonalModel.addConfusion]).
 * It never touches the word counts, the pairs or the triples: the text of a training paragraph is not what this person writes, so it must not bend the suggestions or the language weight.
 * Pure Kotlin; the same code runs in the keyboard ([GlideTyping.teach]) and in the tuning lab (TrainingLabTest).
 */
object GlideTeaching {
    /** Which lessons a taught glide gives (the lab switches them one by one; the keyboard uses all three). */
    class Parts(val thumb: Boolean = true, val shape: Boolean = true, val confusion: Boolean = true)

    /** What a taught glide changed: the thumb habit moved, a shape was remembered, a confusion was counted, or the glide was too wild to be remembered as this word's shape. */
    class Lesson(val thumb: Boolean, val shape: Boolean, val confusion: Boolean, val wild: Boolean) {
        val any: Boolean get() = thumb || shape || confusion
    }

    /** The shape lesson counts like a kept glide when the keyboard was right, like a correction (a word picked from the strip) when it was not. */
    const val SHAPE_RIGHT = 1f
    const val SHAPE_WRONG = 3f
    const val CONTRADICT_RIGHT = 0.7f
    const val CONTRADICT_WRONG = 2f
    const val CONFUSION_WEIGHT = 1f

    /**
     * [dec] has just decoded the glide ([print] is its shape, [GlideDecoder.lastPrint]); [word] (lower case) is the word that was asked for, found as [id] in [lex] (-1 / null when the list lacks it: the thumb is
     * then not taught); [guessed] is what the keyboard put first (null: nothing). [keys] are the key positions the glide was drawn on, to tell a wild glide (never remembered as the word's shape: about 1.15 key
     * units away from the straight line through its letters is already a different word) from a careless one.
     */
    fun teach(dec: GlideDecoder, lex: GlideLexicon?, id: Int, model: PersonalModel, keys: GlideKeys?, word: String, guessed: String?, print: FloatArray?, parts: Parts = Parts()): Lesson {
        val w = PersonalModel.norm(word) ?: return Lesson(false, false, false, false)
        val right = guessed != null && guessed.equals(w, ignoreCase = true)
        var shape = false; var wild = false; var confusion = false; var thumb = false
        if (parts.confusion && !right && !guessed.isNullOrEmpty() && model.addConfusion(guessed.lowercase(), w, CONFUSION_WEIGHT)) confusion = true
        if (parts.shape && print != null) {
            val ideal = if (keys != null) Fingerprint.ofWord(w, keys) else null
            if (ideal != null && Fingerprint.distance(ideal, print) > GlideTyping.FIT_D) wild = true
            else {
                model.patterns.add(w, print, if (right) SHAPE_RIGHT else SHAPE_WRONG)
                model.patterns.contradict(w, print, if (right) CONTRADICT_RIGHT else CONTRADICT_WRONG)
                shape = true
            }
        }
        if (parts.thumb && lex != null && id >= 0) { dec.learnFrom(lex, id); thumb = true }
        return Lesson(thumb, shape, confusion, wild)
    }
}
