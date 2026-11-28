package app.kestrel.core.input.glide

import app.kestrel.core.input.editor.Rig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executor

/**
 * R40-1 / R40-4 (owner 2026-11-28: "Sometimes the keyboard corrects a prior word to a rare word that makes no sense in context. There should be weights so that words like and make the are not replaced with words like
 * ave age access Maggie" and "when i typed 'to it' the keyboard retroactively changed it to 'yo it'. That is ridiculous"). The only thing that rewrites a glided word AFTER the fact is the post-correction of R26a
 * ([GlideSession.revisePrevious]): the next word re-judges the five readings of the word before it. A rare look-alike with ONE known follower (a sparse pair) gets a huge "likelihood of the next word" and used to beat a
 * word thousands of times commoner. These tests pin the weights: a common word is never traded for a far rarer one by an automatic change, while a rare word is still mended to a common one.
 */
class CommonWordGuardTest {
    private val keys = GlideEval.keysFor(564f, 160f)
    // shares in the proportions of the real list: "to" / "and" / "it" / "the" are everyday words, "yo" / "ave" about 10 000 times rarer (ln difference about 9), "form" about 20 times rarer than "from"
    private val bytes = LexiconFile.build(
        listOf("to" to 28_000_000L, "and" to 25_000_000L, "it" to 11_000_000L, "the" to 50_000_000L, "from" to 4_100_000L, "form" to 190_000L, "world" to 550_000L, "hello" to 120_000L, "yo" to 4_000L, "ave" to 14_000L)
            .map { LexiconFile.Entry(it.first, it.second) },
        listOf(LexiconFile.Pair("yo", "it", 3_000L), LexiconFile.Pair("ave", "the", 10_000L), LexiconFile.Pair("from", "the", 900_000L), LexiconFile.Pair("hello", "world", 20_000L))
    )
    private fun cand(text: String, score: Float) = GlideCandidate(text, score, 0.2f, 0f, text.length)

    private inner class Env {
        val rig = Rig("")
        val engine = GlideTyping({ bytes }, Executor { it.run() }, savePersonal = { })
        val s = GlideSession(rig.c, { engine }, { 1_000L }, { })
        init { engine.prepare(keys, null, null); assertTrue(engine.ready); s.ui = object : GlideSession.Ui { override fun showCandidates(words: List<String>) {}; override fun clearCandidates() {}; override fun showNoWord() {} } }
        fun glide(vararg c: GlideCandidate, lm: Float = 1.0f, more: List<GlideCandidate> = emptyList()) {
            val (p2, p) = s.context()
            s.onDecoded(GlideResult(c.toList(), 1, true, lm, more), p, p2); rig.sync(); rig.check()
        }
    }

    @Test fun aCommonWordIsNotTradedForAFarRarerOneBecauseOfASparsePair() {
        val e = Env()
        e.glide(cand("to", 5.0f), cand("yo", 4.9f))                  // the path fits both about equally (t and y are neighbours)
        e.glide(cand("it", 5.0f))
        assertEquals("\"to it\" must stay \"to it\"", "to it ", e.rig.text)
    }

    @Test fun theSameHoldsWhenThePersonHasLearnedTheRarePair() {
        val e = Env()
        repeat(3) { e.engine.learnWord(null, "ave", 1f); e.engine.learnWord("ave", "the", 1f) }          // (what a few kept look-alikes teach)
        e.glide(cand("and", 5.0f), cand("ave", 4.9f))
        e.glide(cand("the", 5.0f))
        assertEquals("and the ", e.rig.text)
    }

    @Test fun aRareWordIsStillMendedToACommonOneWhenTheNextWordProvesIt() {
        val e = Env()
        e.glide(cand("form", 5.0f), cand("from", 4.8f))
        e.glide(cand("the", 5.0f))
        assertEquals("from the ", e.rig.text)
    }

    @Test fun aCommonAlternativeStillWinsWhenARareOneAlsoFitsTheNextWord() {
        val e = Env()
        e.glide(cand("form", 5.0f), cand("yo", 4.95f), cand("from", 4.8f))      // the rare "yo" stands between and must not block the common reading
        e.glide(cand("the", 5.0f))
        assertEquals("from the ", e.rig.text)
    }

    @Test fun theGuardKeepsTheRevisionCounterAtZeroWhenItRefuses() {
        val e = Env()
        e.engine.stats.onDecoded(1, true)
        e.glide(cand("to", 5.0f), cand("yo", 4.9f)); e.glide(cand("it", 5.0f))
        assertEquals("to it ", e.rig.text)
        assertEquals(0, Regex("fixed by the next word (\\d+) times").find(e.engine.stats.line())?.groupValues?.get(1)?.toInt() ?: 0)
    }
}
