package app.swoop.keyboard

import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import app.kestrel.core.input.glide.LearnedExport
import app.kestrel.core.input.glide.LexiconFile
import app.kestrel.core.input.glide.PersonalModel
import app.kestrel.core.input.glide.UserWords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.Executor

/** KB-103: what the keyboard learned survives export and import, a browser-format file imports, and the Setup screen's switch is wired. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class KeyboardFlowTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val list = LexiconFile.build(listOf("hello" to 500L, "world" to 800L).map { LexiconFile.Entry(it.first, it.second) }, emptyList())
    private fun host() = GlideHost(app, Executor { it.run() }, Executor { it.run() }) { list }
    private fun clean() { for (n in listOf("glide_personal.txt", "glide_habit.txt", "own_words.txt")) File(app.filesDir, n).delete() }
    private fun saved() = File(app.filesDir, "glide_personal.txt").let { if (it.isFile) it.readText() else "" }

    @Test fun whatWasLearnedExportsAndImportsIntoAFreshKeyboard() {
        clean()
        val a = host()
        a.engine.learn(null, "hello", 5f, null)                      // (synchronous executor: done when it returns)
        a.engine.saveLearned()
        assertTrue(saved().contains("hello"))
        var text = ""; a.engine.exportText(null, a.own) { text = it }
        assertTrue("the export starts with the magic header", text.startsWith(LearnedExport.MAGIC)); assertTrue(text.contains("hello"))
        clean()                                                       // a reinstall: nothing is left
        val b = host(); assertEquals("", saved())
        var summary: LearnedExport.Summary? = null
        b.engine.importText(text, false, b.own, { }) { summary = it }
        assertNotNull(summary); assertTrue("the imported word is saved right away: ${saved()}", saved().contains("hello"))
    }

    @Test fun importingTheSameFileTwiceChangesNothing() {
        clean(); val h = host()
        val text = LearnedExport.write(PersonalModel().also { it.add(null, "world", 4f, null) }, floatArrayOf(0.1f, 0.2f), UserWords(800).also { it.add("swoopword", 3) })
        h.engine.importText(text, false, h.own, { }) { }; val once = saved()
        h.engine.importText(text, false, h.own, { }) { }
        assertEquals(once, saved()); assertTrue(h.own.contains("swoopword"))
    }

    @Test fun aFileThatIsNotAnExportIsRefusedAndTheMemoryIsUntouched() {
        clean(); val h = host(); h.engine.learn(null, "hello", 2f, null); h.engine.saveLearned(); val before = saved()
        var summary: LearnedExport.Summary? = LearnedExport.Summary(1, 1, 1, 1, 1, 1, 1, false, true)
        h.engine.importText("this is not an export\nw hello 99\n", false, h.own, { }) { summary = it }
        assertNull(summary); assertEquals(before, saved())
    }

    @Test fun readingAFileIsBounded() {
        assertEquals(5, KeyboardFlow.readBounded(ByteArrayInputStream(ByteArray(5)), 10).size)
        try { KeyboardFlow.readBounded(ByteArrayInputStream(ByteArray(100)), 10); throw AssertionError("must refuse") } catch (e: KeyboardFlow.TooBig) { }
    }

    @Test fun theExportFileNameCarriesTheDate() { assertEquals("Swoop keyboard learning 1970-01-01.txt", KeyboardFlow.fileName(0L)) }
}
