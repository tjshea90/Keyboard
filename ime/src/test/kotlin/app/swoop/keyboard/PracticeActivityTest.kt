package app.swoop.keyboard

import android.app.Application
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import app.kestrel.core.input.glide.GlideEval
import app.kestrel.core.input.glide.GlidePractice
import org.junit.Assert.assertEquals
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

/** R34a2: the glide practice screen asks for the words one by one, keeps what the keyboard hands over, shows the score and the misses. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class PracticeActivityTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    private fun all(v: View, out: MutableList<View> = ArrayList()): MutableList<View> { out.add(v); if (v is ViewGroup) for (i in 0 until v.childCount) all(v.getChildAt(i), out); return out }
    private fun texts(a: PracticeActivity) = all(a.window.decorView).filterIsInstance<TextView>().filter { it !is EditText }.map { it.text.toString() }
    private val keys = GlideEval.keysFor(564f, 272f)
    private fun glideOf(a: PracticeActivity, offered: List<String>) = a.glide(floatArrayOf(10f, 20f, 30f, 40f), floatArrayOf(5f, 6f, 7f, 8f), floatArrayOf(0f, 16f, 32f, 48f), 4, offered, keys, 1128, 520)

    @Test fun theScreenOpensTheSinkWhileItIsShownAndClosesItWhenItGoesAway() {
        val c = Robolectric.buildActivity(PracticeActivity::class.java).setup()
        val a = c.get()
        assertSame(a, Practice.sink); assertTrue("the first word is asked for", texts(a).contains(GlidePractice.WORDS[0]))
        assertTrue(texts(a).any { it.startsWith("Word 1 of 40") })
        val box = all(a.window.decorView).filterIsInstance<EditText>().first()
        assertTrue("the box carries the private option, so only its glides are handed over", Practice.isPracticeBox(box.privateImeOptions))
        c.pause(); assertNull(Practice.sink)
    }

    @Test fun eachGlideKeepsTheNextWordComingAndTheEndShowsTheScoreAndTheMisses() {
        val a = Robolectric.buildActivity(PracticeActivity::class.java).setup().get()
        val words = GlidePractice.WORDS
        for ((i, w) in words.withIndex()) {
            assertTrue("word ${i + 1} is asked for", texts(a).contains(w))
            glideOf(a, if (w == "for") listOf("fur", "for", "fir") else listOf(w, "x", "y"))
        }
        val t = texts(a)
        assertTrue("it says it is done: $t", t.contains("Done"))
        assertTrue("39 of 40 first words right: $t", t.any { it.startsWith("The first word was right 39 of 40 times (97%)") })
        assertTrue("the miss is named, asked-for word first: $t", t.any { it.contains("for \u2192 fur") })
        assertTrue(t.contains("Save the file"))
    }

    @Test fun skippingAndFinishingNowKeepOnlyWhatWasGlided() {
        val a = Robolectric.buildActivity(PracticeActivity::class.java).setup().get()
        glideOf(a, listOf("the", "x", "y"))
        all(a.window.decorView).filterIsInstance<TextView>().first { it.text.toString() == "Skip this word" }.performClick()
        assertTrue(texts(a).contains(GlidePractice.WORDS[2]))
        all(a.window.decorView).filterIsInstance<TextView>().first { it.text.toString() == "Finish now" }.performClick()
        val t = texts(a)
        assertTrue("one answered of one: $t", t.any { it.startsWith("The first word was right 1 of 1 times") })
        all(a.window.decorView).filterIsInstance<TextView>().first { it.text.toString() == "Start again" }.performClick()
        assertTrue(texts(a).contains(GlidePractice.WORDS[0]))
        assertNotNull(Practice.sink)
    }

    @Test fun finishingBeforeAnyGlideSaysSoAndOffersNoFile() {
        val a = Robolectric.buildActivity(PracticeActivity::class.java).setup().get()
        all(a.window.decorView).filterIsInstance<TextView>().first { it.text.toString() == "Finish now" }.performClick()
        val t = texts(a)
        assertTrue(t.contains("Nothing was glided yet.")); assertTrue("no file to save", !t.contains("Save the file"))
    }
}
