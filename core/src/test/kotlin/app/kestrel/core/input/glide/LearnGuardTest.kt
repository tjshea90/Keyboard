package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** R29: when a kept glide is learned lightly (a rare word that only learning put in front of a much commoner one) and when it is never touched. */
class LearnGuardTest {
    @Test fun aRareLookAlikeThatOnlyLearningPutFirstIsLearnedLightly() {
        val v = LearnGuard.judge("and", "access", 5.5f, "access", 1f, afterUndo = false, revised = false)
        assertTrue(v.circular)
        assertEquals(LearnGuard.SELF_WEIGHT, v.weight, 0f)
        assertFalse(v.shape); assertFalse(v.habit)
    }

    @Test fun anHonestLearnedWordKeepsItsWeight() {
        // the plain decoder agrees: nothing circular
        assertFalse(LearnGuard.judge("make", "make", 0f, "make", 1f, false, false).circular)
        // learning changed the word but the plain word is not much commoner (a habit fixing a close call)
        val close = LearnGuard.judge("magic", "maggie", 2.0f, "maggie", 1f, false, false)
        assertFalse(close.circular); assertEquals(1f, close.weight, 0f); assertTrue(close.shape); assertTrue(close.habit)
    }

    @Test fun theGapDecides() {
        assertFalse(LearnGuard.judge("for", "fur", LearnGuard.GAP_NATS - 0.01f, "fur", 1f, false, false).circular)
        assertTrue(LearnGuard.judge("for", "fur", LearnGuard.GAP_NATS, "fur", 1f, false, false).circular)
    }

    @Test fun whatThePersonDidIsNeverGuarded() {
        // picked from the strip / chosen (weight 3), a word that replaced a deleted one, a word the next word's context changed
        assertFalse(LearnGuard.judge("and", "access", 6f, "access", 3f, false, false).circular)
        assertFalse(LearnGuard.judge("and", "access", 6f, "access", 1f, true, false).circular)
        assertFalse(LearnGuard.judge("and", "access", 6f, "access", 1f, false, true).circular)
    }

    @Test fun onlyTheLearnedWordThatStayedCounts() {
        // the word in the text is not the one learning put first (it was swapped): not this rule's business
        assertFalse(LearnGuard.judge("and", "access", 6f, "and", 1f, false, false).circular)
        // no shadow decode (it had not run): nothing is guarded
        assertFalse(LearnGuard.judge(null, null, 0f, "access", 1f, false, false).circular)
    }
}
