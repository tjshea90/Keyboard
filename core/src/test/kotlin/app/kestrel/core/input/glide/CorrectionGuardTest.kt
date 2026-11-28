package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * R40: the weight rule on the numbers of the REAL shipped list (ln shares and ln pair strengths printed by ReviseProbeTest on 2026-11-28; the list is 140 000 words and 243 000 pairs).
 * NaN as a pair = not in the list.
 */
class CorrectionGuardTest {
    private val nan = Float.NaN
    private fun ok(oldLn: Float, newLn: Float, oldPair: Float = nan, newPair: Float = nan) = CorrectionGuard.allows(oldLn, newLn, oldPair, newPair)

    @Test fun theAllowanceGrowsAsTheOldWordGetsRarer() {
        assertEquals(1.5f, CorrectionGuard.allowedDrop(-3.0f), 1e-6f)      // the
        assertEquals(1.5f, CorrectionGuard.allowedDrop(-3.56f), 1e-6f)     // to
        assertEquals(1.85f, CorrectionGuard.allowedDrop(-6.7f), 1e-3f)     // make
        assertEquals(2.75f, CorrectionGuard.allowedDrop(-8.5f), 1e-3f)     // age
        assertEquals(4.5f, CorrectionGuard.allowedDrop(-12.4f), 1e-6f)     // yo
    }

    @Test fun theOwnersExamplesAreNeverReplacedByTheirRareLookAlikes() {
        // (old, new, ln old, ln new, pair of old with the next word, pair of new with it): "next" is the word that follows
        assertFalse("to -> yo before 'it'", ok(-3.56f, -12.42f, -6.14f, nan))
        assertFalse("and -> ave before 'the'", ok(-3.71f, -11.16f, -2.70f, nan))
        assertFalse("make -> Maggie before 'it'", ok(-6.70f, -11.38f, -2.27f, nan))
        assertFalse("for -> fur before 'the'", ok(-4.52f, -11.32f, -1.83f, nan))
        assertFalse("and -> access before 'to', even though access to is a strong pair", ok(-3.71f, -8.90f, -4.84f, -0.32f))
        assertFalse("and -> age", ok(-3.71f, -8.49f, -2.70f, nan))
        assertFalse("the -> yo even when only a pair the person made says so (not in the list)", ok(-3.01f, -12.42f, nan, nan))
    }

    @Test fun aWordTheListCannotFollowIsStillMendedWhenTheNextWordProvesIt() {
        assertTrue("the -> there before 'are' (-11.5 against -1.5)", ok(-3.01f, -5.96f, -11.48f, -1.52f))
        assertTrue("the -> there before 'were'", ok(-3.01f, -5.96f, -12.30f, -2.81f))
        assertTrue("he -> grew before 'up'", ok(-5.39f, -10.17f, -10.34f, -0.41f))
        assertTrue("work -> serial before 'number' (no pair for work)", ok(-6.93f, -10.59f, nan, -1.24f))
        assertTrue("back -> bank before 'holidays'", ok(-6.56f, -8.82f, nan, -3.68f))
    }

    @Test fun commonerOrEquallyCommonWordsAlwaysPass() {
        assertTrue("form -> from", ok(-8.56f, -5.49f))
        assertTrue("it's -> its", ok(-6.2f, -7.1f))
        assertTrue("or -> our", ok(-5.63f, -6.30f))
        assertTrue("in -> on", ok(-4.3f, -5.3f))
        assertTrue("a rare slip -> a common word", ok(-12.0f, -4.0f))
    }

    @Test fun aBigDropNeedsAStrongPairNotJustAnyPair() {
        assertFalse("the pair exists but is no stronger than the old one", ok(-3.01f, -5.96f, -3.0f, -2.5f))
        assertFalse("a gain of 4 nats is not enough", ok(-3.01f, -5.96f, -8.0f, -4.0f))
        assertTrue("a gain of 5 nats is", ok(-3.01f, -5.96f, -8.0f, -3.0f))
        assertFalse("never more than BACKED_DROP rarer, whatever the pair says", ok(-3.71f, -12.42f, nan, -0.1f))
    }
}
