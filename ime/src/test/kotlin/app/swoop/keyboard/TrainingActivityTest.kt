package app.swoop.keyboard

import android.app.Application
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import app.kestrel.core.input.glide.GlideKeys
import app.kestrel.core.input.glide.GlideTraining
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** R36: the Glide training screen: start, paragraphs word by word with the words written so far in the box, a score after every page, the extra page of missed words, stopping, saving, and what is handed back to the keyboard. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class TrainingActivityTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    private fun all(v: View, out: MutableList<View> = ArrayList()): MutableList<View> { out.add(v); if (v is ViewGroup) for (i in 0 until v.childCount) all(v.getChildAt(i), out); return out }
    private fun texts(a: TrainingActivity) = all(a.window.decorView).filterIsInstance<TextView>().filter { it !is EditText }.map { it.text.toString() }
    private fun clickRow(a: TrainingActivity, title: String) {
        var v: View? = all(a.window.decorView).filterIsInstance<TextView>().first { it.text.toString() == title }
        while (v != null && !v.hasOnClickListeners()) v = v.parent as? View
        assertNotNull("a row with a click listener for '$title'", v); v!!.performClick()
    }
    private fun box(a: TrainingActivity) = all(a.window.decorView).filterIsInstance<EditText>().firstOrNull { Practice.isPracticeBox(it.privateImeOptions) }
    private val keys = GlideKeys(FloatArray(26) { 30f + it * 20f }, FloatArray(26) { 60f + (it % 3) * 40f }, 30f, 26f, 36f)
    private fun glideOf(a: TrainingActivity, offered: List<String>, prev: String? = null, prev2: String? = null) =
        a.glide(floatArrayOf(10f, 20f, 30f, 40f), floatArrayOf(5f, 6f, 7f, 8f), floatArrayOf(0f, 16f, 32f, 48f), 4, offered, keys, 1128, 520, prev, prev2)
    /** Glide the word that is asked for now, the way the keyboard would have typed it. */
    private fun glideRight(a: TrainingActivity): String? = glideOf(a, listOf(a.trainingForTest!!.current!!, "x", "y"))
    private fun start(): TrainingActivity { val a = Robolectric.buildActivity(TrainingActivity::class.java).setup().get(); clickText(a, "Start the training"); return a }
    private fun clickText(a: TrainingActivity, t: String) { all(a.window.decorView).filterIsInstance<TextView>().first { it.text.toString() == t }.performClick() }

    @Test fun theScreenOpensOnAStartPageAndOffersTheSwitchAndTheOwnWords() {
        val c = Robolectric.buildActivity(TrainingActivity::class.java).setup()
        val a = c.get()
        assertSame("the screen listens for glides while it is shown", a, Practice.sink)
        val t = texts(a)
        assertTrue(t.contains("The keyboard learns from my glides here")); assertTrue(t.contains("Words that give you trouble")); assertTrue(t.contains("Start the training"))
        assertNull("no box yet: nothing to glide into", box(a)?.takeIf { it.parent != null })
        assertNull("nothing is taught before the training starts", glideOf(a, listOf("the")))
        c.pause(); assertNull(Practice.sink)
    }

    @Test fun startingAsksForTheFirstWordOfTheFirstParagraphInAnEmptyBoxThatCarriesThePrivateOption() {
        val a = start()
        val tr = a.trainingForTest!!
        assertEquals("the first prompt is the first word of two letters or more ('I' is typed by hand)", "was", tr.current)
        assertTrue(texts(a).any { it.startsWith("Page 1 of 11") })
        val b = box(a)!!
        assertTrue("the box is the private one: only its glides are handed over", Practice.isPracticeBox(b.privateImeOptions))
        assertTrue(b.imeOptions and android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0)
        assertTrue("the paragraph is on screen", texts(a).any { it.startsWith("I was thinking we could meet") })
    }

    @Test fun eachGlideAdvancesTheWordAndTheBoxHoldsTheWordsWrittenSoFar() {
        val a = start()
        val tr = a.trainingForTest!!
        val first = tr.current!!
        assertEquals("the keyboard is told to learn: it gets the word that was asked for", first, glideRight(a))
        assertEquals(1, tr.answered)
        assertEquals("the box holds what was written so far (the 'I' too)", "I was ", box(a)!!.text.toString())
        glideRight(a)
        assertEquals("I was thinking ", box(a)!!.text.toString())
        assertEquals("the cursor stands at the end", box(a)!!.text.length, box(a)!!.selectionStart)
    }

    @Test fun withTheSwitchOffTheGlidesAreOnlyMeasuredNothingIsHandedBack() {
        val c = Robolectric.buildActivity(TrainingActivity::class.java).setup(); val a = c.get()
        clickRow(a, "The keyboard learns from my glides here")
        clickText(a, "Start the training")
        assertNull("measure only", glideRight(a))
        assertEquals("but the glide is kept", 1, a.trainingForTest!!.answered)
    }

    @Test fun aFinishedPageShowsItsScoreAndNextPageGoesOn() {
        val a = start()
        val tr = a.trainingForTest!!
        var n = 0
        while (!tr.atBreak) { if (n == 3) glideOf(a, listOf("zzz", tr.current!!, "y")) else glideRight(a); n++ }
        val t = texts(a)
        assertTrue("page 1 is done: $t", t.contains("Page 1 done"))
        assertTrue("one miss in the first three, the rest right: $t", t.any { it.startsWith("The first word was right ${n - 1} of $n times") })
        assertTrue(t.contains("Next page")); assertTrue(t.contains("Save the file now"))
        assertNull("no glide counts while the score is shown", glideOf(a, listOf("x")))
        clickText(a, "Next page")
        assertTrue(texts(a).any { it.startsWith("Page 2 of 11") })
        assertFalse(tr.atBreak)
    }

    @Test fun theOwnProblemWordsComeAsTheirOwnPageAndTheLastPageRepeatsWhatWentWrong() {
        val c = Robolectric.buildActivity(TrainingActivity::class.java).setup(); val a = c.get()
        val et = all(a.window.decorView).filterIsInstance<EditText>().first()
        et.setText("There, access!")
        clickText(a, "Start the training")
        val tr = a.trainingForTest!!
        assertEquals(12, tr.pageCount)
        assertEquals("Your own problem words", tr.pages[2].title)
        // glide through everything; every "our" is typed as "or"
        var guard = 0
        while (!tr.done && guard++ < 3000) {
            if (tr.atBreak) { clickText(a, "Next page"); continue }
            val w = tr.current!!
            glideOf(a, if (w == "our") listOf("or", "our", "out") else listOf(w, "x", "y"))
        }
        assertTrue("the training ended", tr.done)
        val last = tr.pages.last()
        assertEquals("the page of words that went wrong comes last", GlideTraining.Kind.MISSED, last.kind)
        val words = last.tokens.mapNotNull { it.word }.toSet()
        assertEquals("both words of the pair: the one asked for and the one typed", setOf("our", "or"), words)
        val t = texts(a)
        assertTrue("it says it is finished: $t", t.contains("Training finished"))
        assertTrue("it names the pair: $t", t.any { it.contains("our → or") })
        assertTrue(t.contains("Save the file")); assertTrue(t.any { it.startsWith("The keyboard learned from ") })
    }

    @Test fun stoppingEarlyShowsTheResultOfWhatWasGlidedAndNothingWhenNothingWas() {
        val a = start()
        clickText(a, "Stop and show the result")
        var t = texts(a)
        assertTrue(t.contains("Training finished")); assertTrue(t.contains("Nothing was glided yet.")); assertFalse("no file to save", t.contains("Save the file"))
        clickRow(a, "Start again")
        assertTrue(texts(a).contains("Start the training"))
        clickText(a, "Start the training")
        glideRight(a); glideRight(a)
        clickText(a, "Stop and show the result")
        t = texts(a)
        assertTrue("two of two right: $t", t.any { it.startsWith("Altogether the first word was right 2 of 2 times") })
        assertTrue(t.contains("Save the file"))
    }

    @Test fun skippingAWordKeepsNothingForItAndMovesOn() {
        val a = start()
        val tr = a.trainingForTest!!
        val first = tr.current
        clickText(a, "Skip this word")
        assertEquals(0, tr.answered); assertTrue(tr.current != first)
    }

    @Test fun aGlideBeforeTheTrainingStartsOrAfterItEndedIsNotKept() {
        val c = Robolectric.buildActivity(TrainingActivity::class.java).setup(); val a = c.get()
        assertNull(glideOf(a, listOf("the")))
        clickText(a, "Start the training"); clickText(a, "Stop and show the result")
        assertNull(glideOf(a, listOf("the")))
        assertEquals(0, a.trainingForTest!!.answered)
    }
}
