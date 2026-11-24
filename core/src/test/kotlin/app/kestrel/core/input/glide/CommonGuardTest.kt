package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** R32e: a rare first guess gives way to a much commoner one that fits almost as well, and to nothing else. */
class CommonGuardTest {
    private fun c(text: String, score: Float, prior: Float) = GlideCandidate(text, score, 0f, prior, text.length)
    private fun texts(l: List<GlideCandidate>) = l.map { it.text }

    @Test fun aRareFirstGuessGivesWayToACommonOneThatFitsAlmostAsWell() {
        val l = listOf(c("ave", -10.0f, -11.2f), c("and", -11.5f, -3.7f), c("ace", -12f, -9f))
        assertEquals(listOf("and", "ave", "ace"), texts(CommonGuard.apply(l)))
        val f = listOf(c("fur", -5f, -11.3f), c("for", -6f, -4.5f))
        assertEquals(listOf("for", "fur"), texts(CommonGuard.apply(f)))
    }

    @Test fun theRareWordStaysAmongTheGuesses() {
        val out = CommonGuard.apply(listOf(c("ave", -10f, -11.2f), c("and", -11f, -3.7f)))
        assertEquals(2, out.size); assertEquals("ave", out[1].text)
    }

    @Test fun nothingChangesWhenTheCommonOneFitsMuchWorse() {
        val l = listOf(c("ave", -10f, -11.2f), c("and", -10f - CommonGuard.MARGIN - 0.5f, -3.7f))
        assertSame(l, CommonGuard.apply(l))
    }

    @Test fun nothingChangesWhenTheDifferenceInCommonnessIsSmall() {
        val l = listOf(c("make", -10f, -6.7f), c("made", -10.5f, -5.5f), c("fur", -10.2f, -11f))
        val two = listOf(l[0], l[1])
        assertSame("a gap of 1.2 nats is no reason: both are ordinary words", two, CommonGuard.apply(two))
        assertEquals("make", CommonGuard.apply(l)[0].text)
    }

    @Test fun aRareWordThePersonUsesIsNotRareBecauseItsPriorCarriesWhatTheyTyped() {
        val l = listOf(c("maggie", -10f, -4.0f), c("make", -10.5f, -6.7f))     // (a name this person writes: its prior is high)
        assertSame(l, CommonGuard.apply(l))
    }

    @Test fun ofSeveralCommonGuessesTheBestFittingOneWins() {
        val l = listOf(c("ave", -10f, -11.2f), c("and", -12f, -3.7f), c("any", -11f, -5f))
        assertEquals("any", CommonGuard.apply(l)[0].text)
    }

    @Test fun aCommonFirstGuessIsLeftAloneAndShortListsAreSafe() {
        val l = listOf(c("and", -10f, -3.7f), c("ave", -9.5f, -11.2f)); assertSame(l, CommonGuard.apply(l))
        val one = listOf(c("ave", -10f, -11.2f)); assertSame(one, CommonGuard.apply(one)); assertEquals(emptyList<GlideCandidate>(), CommonGuard.apply(emptyList()))
    }

    @Test fun onlyTheFirstFiveGuessesAreLookedAt() {
        val l = listOf(c("ave", -10f, -11.2f), c("a1", -10.1f, -11f), c("a2", -10.2f, -11f), c("a3", -10.3f, -11f), c("a4", -10.4f, -11f), c("and", -10.5f, -3.7f))
        assertSame(l, CommonGuard.apply(l))
    }
}
