package app.swoop.keyboard

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.widget.Button
import app.kestrel.core.input.glide.LearnedExport
import app.kestrel.core.input.glide.LexiconFile
import app.kestrel.core.input.glide.PersonalModel
import app.kestrel.core.input.glide.UserWords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File

/** Owner request 17: what the Kestrel browser's own keyboard learned is handed to Swoop by an explicit intent, shown, and added only after a tap on Import. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class ReceiveLearnedActivityTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private fun clean() { for (n in listOf("glide_personal.txt", "glide_habit.txt", "own_words.txt")) File(app.filesDir, n).delete() }
    private fun saved() = File(app.filesDir, "glide_personal.txt").let { if (it.isFile) it.readText() else "" }
    private fun export(): Uri {
        val text = LearnedExport.write(PersonalModel().also { it.add(null, "kestrelword", 4f, null) }, floatArrayOf(0.1f, 0.2f), UserWords(800).also { it.add("fromkestrel", 3) })
        val f = File(app.cacheDir, "handover.txt"); f.writeText(text); return Uri.fromFile(f)
    }
    private fun start(i: Intent): ReceiveLearnedActivity = Robolectric.buildActivity(ReceiveLearnedActivity::class.java, i).create().start().resume().get()

    @Test fun theFileIsShownAndAddedOnlyAfterAYes() {
        clean()
        val a = start(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_STREAM, export()) })
        Robolectric.flushBackgroundThreadScheduler(); Robolectric.flushForegroundThreadScheduler()
        val d = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
        assertTrue("the person is asked first", d != null && d.isShowing); assertEquals("nothing was added yet", "", saved())
        d.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        Robolectric.flushBackgroundThreadScheduler(); Robolectric.flushForegroundThreadScheduler()
        assertTrue("after the yes the words are in the keyboard's memory: ${saved()}", saved().contains("kestrelword") || File(app.filesDir, "own_words.txt").let { it.isFile && it.readText().contains("fromkestrel") })
        assertTrue("and the screen closes itself", a.isFinishing)
    }

    @Test fun aNoAddsNothingAndAnythingThatIsNotAnExportIsRefused() {
        clean()
        val a = start(Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_STREAM, export()) })
        Robolectric.flushBackgroundThreadScheduler(); Robolectric.flushForegroundThreadScheduler()
        org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        assertEquals("", saved()); assertTrue(a.isFinishing)
        val junk = File(app.cacheDir, "junk.txt"); junk.writeText("this is not an export\nw hello 99\n")
        val b = start(Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_STREAM, Uri.fromFile(junk)) })
        Robolectric.flushBackgroundThreadScheduler(); Robolectric.flushForegroundThreadScheduler()
        assertEquals("nothing was added from a file that is not an export", "", saved()); assertTrue(b.isFinishing)
    }

    @Test fun anIntentWithoutAFileOrWithAnotherActionJustCloses() {
        assertTrue(start(Intent(Intent.ACTION_SEND)).isFinishing)
        assertTrue(start(Intent(Intent.ACTION_VIEW).apply { putExtra(Intent.EXTRA_STREAM, export()) }).isFinishing)
    }
}
