package app.kestrel.core.input.glide

/**
 * R33: are two words spelled so alike that one may be the other with a slip of the finger (ave / and, fur / for, tho / the)? Used to tell a CORRECTION of the word just glided from a NEXT word when the person taps a word on
 * the strip. Both words have at least 3 letters, at most one letter apart in length, at most two letters apart in spelling (edit distance), and they begin or end with the same letter (a glide that goes wrong keeps one end).
 */
object SpellingNear {
    fun near(a0: String, b0: String): Boolean {
        val a = a0.lowercase(); val b = b0.lowercase()
        if (a == b || a.length < 3 || b.length < 3 || Math.abs(a.length - b.length) > 1) return false
        if (a.first() != b.first() && a.last() != b.last()) return false
        return TypingLearner.distance(a, b) <= 2
    }
}
