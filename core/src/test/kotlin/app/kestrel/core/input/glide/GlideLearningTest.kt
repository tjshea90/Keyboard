package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executor

/**
 * R40-6: the learning rule that Swoop and the Kestrel browser's keyboard share ([GlideLearning.confirm]). Counts are in the proportions (and the scale, about 3e11) of the real list: "and" is an everyday word, "ave" about
 * 20 000 times rarer (ln -11.5, the rarity at which a kept glide teaches almost nothing).
 */
class GlideLearningTest {
    private val keys = GlideEval.keysFor(564f, 160f)
    private val bytes = LexiconFile.build(
        listOf("the" to 120_000_000_000L, "and" to 60_000_000_000L, "for" to 40_000_000_000L, "ave" to 3_000_000L, "fur" to 3_000_000L).map { LexiconFile.Entry(it.first, it.second) },
        listOf(LexiconFile.Pair("the", "and", 2_000_000_000L))
    )

    private class Env(bytes: ByteArray, keys: GlideKeys) {
        val saved = ArrayList<String>()
        val engine = GlideTyping({ bytes }, Executor { it.run() }, savePersonal = { saved.add(it) }).also { it.prepare(keys, null, null) }
        fun model(): PersonalModel { engine.saveLearned(); return PersonalModel.parse(saved.last()) }
    }
    private fun env() = Env(bytes, keys)

    @Test fun aKeptGlideOfARareLookAlikeIsLearnedOnlyLightly() {
        val e = env()
        GlideLearning.confirm(e.engine, KeptGlide("ave", null, null), true) { }
        val w = e.model().weightOf("ave")
        assertTrue("a kept rare look-alike teaches a light lesson, not 1 use: $w", w > 0f && w < 0.5f)
    }

    @Test fun aKeptGlideOfAnEverydayWordIsLearnedInFull() {
        val e = env()
        GlideLearning.confirm(e.engine, KeptGlide("and", "the", null), true) { }
        assertEquals(1f, e.model().weightOf("and"), 0.01f)
    }

    @Test fun aWordThePersonPickedCountsThreeTimesAndTheWrongFirstGuessIsTakenBack() {
        val e = env()
        e.engine.learnWord(null, "ave", 3f)                                                              // (what the wrong guess had earned before)
        GlideLearning.confirm(e.engine, KeptGlide("for", null, null, weight = 3f, pickedOver = "ave"), true) { }
        val m = e.model()
        assertEquals("the pick counts three times", 3f, m.weightOf("for"), 0.01f)
        assertEquals("what 'ave' had earned is taken back by UNLEARN_WRONG", 3f - GlideLearning.UNLEARN_WRONG, m.weightOf("ave"), 0.01f)
    }

    @Test fun aWordThatCameInsteadOfADeletedGlideCountsThreeTimesAndRemembersTheMistake() {
        val e = env()
        GlideLearning.confirm(e.engine, KeptGlide("for", null, null, afterUndo = true, deleted = "fur"), true) { }
        val m = e.model()
        assertEquals(3f, m.weightOf("for"), 0.01f)
        assertTrue("the correction fur -> for is remembered", m.confusionWeight("fur", "for") > 0f)
    }

    @Test fun nothingIsLearnedWhereTheFieldSaysNoButTheKeptCounterStillCounts() {
        val e = env()
        GlideLearning.confirm(e.engine, KeptGlide("for", null, null, weight = 3f, pickedOver = "ave"), false) { }
        val m = e.model()
        assertEquals(0f, m.weightOf("for"), 0f)
        assertEquals(0f, m.weightOf("ave"), 0f)
    }
}
