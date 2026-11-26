package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** R36: the script of the Glide training: paragraphs, drills of look-alike words, the page of words that went wrong, per-page scores and the file. */
class GlideTrainingTest {
    private val lex = GlideEval.loadLexicon()
    private val xs = floatArrayOf(10f, 40f, 90f); private val ys = floatArrayOf(10f, 20f, 30f); private val ts = floatArrayOf(0f, 50f, 100f)

    @Test fun everyPromptedWordIsInTheShippedWordListAndLongEnoughToBeGlided() {
        val t = GlideTraining()
        val missing = t.practice.words.filter { lex.idOf(it) < 0 }
        assertTrue("words the shipped list does not know: $missing", missing.isEmpty())
        assertTrue(t.practice.words.all { it.length >= 2 && it.all { c -> c in 'a'..'z' } })
    }

    @Test fun theDefaultTrainingIsMuchLongerThanTheFortyWordDrill() {
        val t = GlideTraining()
        assertTrue("${t.total} prompts", t.total in 300..520)
        assertEquals("eight paragraphs and three drills, nothing missed yet", 11, t.pageCount)
        assertEquals(listOf(GlideTraining.Kind.TEXT, GlideTraining.Kind.TEXT, GlideTraining.Kind.DRILL), t.pages.take(3).map { it.kind })
    }

    @Test fun aDrillAsksEveryWordSeveralTimesAndNeverTheSameWordTwiceInARow() {
        val t = GlideTraining(repeats = 4)
        for (p in t.pages.filter { it.kind == GlideTraining.Kind.DRILL }) {
            val words = p.tokens.mapNotNull { it.word }
            for (w in words.distinct()) assertEquals("$w in ${p.title}", 4, words.count { it == w })
            for (i in 1 until words.size) assertTrue("'${words[i]}' twice in a row on ${p.title}", words[i] != words[i - 1])
        }
    }

    @Test fun theParagraphsKeepTheirPunctuationAndOneLetterWordsAreNotPrompted() {
        val t = GlideTraining(paragraphs = listOf("I think a cat, or two."), drills = emptyList())
        val toks = t.pages[0].tokens
        assertEquals(listOf("I", "think", "a", "cat,", "or", "two."), toks.map { it.shown })
        assertEquals(listOf(null, "think", null, "cat", "or", "two"), toks.map { it.word })
        assertEquals(listOf("think", "cat", "or", "two"), t.practice.words)
        assertEquals("the box is empty at the start of a paragraph", "", t.boxText())
        t.record(xs, ys, ts, 3, listOf("think"), null, null)
        assertEquals("what the keyboard sees: everything written so far, one letter words and marks included", "I think a ", t.boxText())
        t.record(xs, ys, ts, 3, listOf("cat"), "think", "I")
        assertEquals("I think a cat, ", t.boxText())
    }

    @Test fun aDrillPageShowsNoContext() {
        val t = GlideTraining(paragraphs = emptyList(), drills = listOf("d" to listOf("the", "there")), repeats = 2)
        assertEquals("", t.boxText())
        t.record(xs, ys, ts, 3, listOf("the")); assertEquals("", t.boxText())
    }

    @Test fun recordingMarksTheTokenAndAFinishedPageWaitsForTheNextOne() {
        val t = GlideTraining(paragraphs = listOf("one two three four", "five six"), drills = emptyList())
        assertFalse(t.atBreak)
        assertEquals("one", t.current)
        assertEquals("one", t.record(xs, ys, ts, 3, listOf("one", "won")))
        assertEquals(GlideTraining.RIGHT, t.pages[0].tokens[0].state)
        assertEquals("two", t.record(xs, ys, ts, 3, listOf("too", "two")))
        assertEquals("in the first three counts as near", GlideTraining.NEAR, t.pages[0].tokens[1].state)
        t.record(xs, ys, ts, 3, listOf("tree", "thee"))
        assertEquals(GlideTraining.MISS, t.pages[0].tokens[2].state)
        t.skip()
        assertEquals(GlideTraining.SKIPPED, t.pages[0].tokens[3].state)
        assertTrue("the first page is over", t.atBreak)
        assertEquals(0, t.breakPage)
        assertNull("no glide counts while the page summary is up", t.record(xs, ys, ts, 3, listOf("five")))
        assertEquals(3, t.score(0).answered); assertEquals(1, t.score(0).right); assertEquals(2, t.score(0).inThree)
        t.continueNext()
        assertFalse(t.atBreak)
        assertEquals("five", t.current); assertEquals(1, t.pageIndex)
    }

    @Test fun afterTheLastPageTheWordsThatWentWrongComeBackTogetherWithWhatWasTypedInsteadOfThem() {
        val t = GlideTraining(paragraphs = listOf("we want our house"), drills = emptyList())
        t.record(xs, ys, ts, 3, listOf("we")); t.record(xs, ys, ts, 3, listOf("want"))
        t.record(xs, ys, ts, 3, listOf("or", "our"))                      // missed: typed "or" for "our"
        t.record(xs, ys, ts, 3, listOf("house"))
        assertTrue(t.atBreak)
        assertFalse("there is one more page, the person is not done", t.done)
        assertEquals(2, t.pageCount)
        val extra = t.pages[1]
        assertEquals(GlideTraining.Kind.MISSED, extra.kind)
        val words = extra.tokens.mapNotNull { it.word }
        assertEquals("both words of the pair, three times each", listOf("our", "or"), words.distinct().sortedByDescending { it == "our" })
        assertEquals(6, words.size)
        t.continueNext()
        repeat(6) { t.record(xs, ys, ts, 3, listOf(t.current ?: "")) }
        assertTrue(t.done)
        assertEquals(10, t.answered)
    }

    @Test fun nothingWrongMeansNoExtraPage() {
        val t = GlideTraining(paragraphs = listOf("we want it"), drills = emptyList())
        for (w in listOf("we", "want", "it")) t.record(xs, ys, ts, 3, listOf(w))
        assertTrue(t.done); assertEquals(1, t.pageCount)
        assertEquals(3, t.totalScore().right)
    }

    @Test fun theOwnProblemWordsGetAPageAfterTheSecondParagraph() {
        val t = GlideTraining(extraWords = GlideTraining.cleanWords("Fur, ave; access! x  FOR for"))
        assertEquals(listOf("fur", "ave", "access", "for"), GlideTraining.cleanWords("Fur, ave; access! x  FOR for"))
        assertEquals("Your own problem words", t.pages[2].title)
        assertEquals(GlideTraining.Kind.DRILL, t.pages[2].kind)
        assertEquals(12, t.pageCount)
    }

    @Test fun stoppingEarlyKeepsWhatWasAnswered() {
        val t = GlideTraining(paragraphs = listOf("we want it too"), drills = emptyList())
        t.record(xs, ys, ts, 3, listOf("we"))
        t.finishNow()
        assertTrue(t.done); assertNull(t.current); assertEquals(1, t.answered)
        assertEquals("a stopped training makes no extra page", 1, t.pageCount)
    }

    @Test fun theFileOfATrainingKeepsTheContextAndThePageAndReadsBack() {
        val keys = GlideEval.keysFor(564f, 272f)
        val t = GlideTraining(paragraphs = listOf("we want our house"), drills = emptyList())
        t.record(xs, ys, ts, 3, listOf("we"))
        t.record(xs, ys, ts, 3, listOf("want", "wan"), "we", null)
        t.record(xs, ys, ts, 3, listOf("or", "our"), "want", "we")
        val text = t.serialize(keys, 564, 272, 1f, "test", teaching = true)
        assertTrue(text.startsWith(GlidePractice.MAGIC2))
        val parsed = GlidePractice.parse(text)
        assertNotNull(parsed); assertTrue(parsed!!.training); assertTrue(parsed.teaching)
        assertEquals(3, parsed.entries.size)
        val e = parsed.entries[2]
        assertEquals("our", e.word); assertEquals("want", e.prev); assertEquals("we", e.prev2); assertEquals("p1", e.tag)
        assertNull(parsed.entries[0].prev)
        // the drill's first-format file is still read, with no context
        val old = GlidePractice().also { it.record(xs, ys, ts, 3, listOf("the")) }
        val p1 = GlidePractice.parse(old.serialize(keys, 564, 272, 1f, "test"))
        assertNotNull(p1); assertFalse(p1!!.training); assertNull(p1.entries[0].prev)
    }

    @Test fun aCutOffTrainingFileKeepsItsCompleteLines() {
        val keys = GlideEval.keysFor(564f, 272f)
        val t = GlideTraining(paragraphs = listOf("we want our house"), drills = emptyList())
        t.record(xs, ys, ts, 3, listOf("we")); t.record(xs, ys, ts, 3, listOf("want"), "we", null)
        val text = t.serialize(keys, 564, 272, 1f, "test", teaching = false)
        val parsed = GlidePractice.parse(text.substring(0, text.length - 12))
        assertNotNull(parsed); assertEquals(1, parsed!!.entries.size)
    }
}
