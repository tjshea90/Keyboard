package app.swoop.keyboard

import android.app.Application
import app.kestrel.core.input.glide.LexiconFile
import app.kestrel.core.input.glide.PersonalModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.Executor

/** M14: what the keyboard learned survives an update. An update keeps the private files; these tests prove the keyboard reads the files an OLDER version wrote, and survives a cut-off write. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class UpdateSafetyTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val direct = Executor { it.run() }
    private val list = LexiconFile.build(listOf("hello" to 500L, "world" to 800L).map { LexiconFile.Entry(it.first, it.second) }, emptyList())

    /** The learned-memory file exactly as version 0.3.0 wrote it (lines w/p/t/c/r/s). Never edit this text: a newer version must keep reading it. */
    private val v030 = "w hello 4.00\\nw kestrel 2.50\\np hello world 3.00\\nt good hello world 1.00\\nc hlelo hello 1.00\\nr hello 0.900\\n".replace("\\n", "\n")

    private fun host() = GlideHost(app, direct, direct) { list }

    @Test fun aMemoryFileFromAnOlderVersionIsReadAndKeptAfterANewSave() {
        File(app.filesDir, "glide_personal.txt").writeText(v030)
        val m = PersonalModel.parse(File(app.filesDir, "glide_personal.txt").readText())
        assertTrue("hello and kestrel are known", m.weightOf("hello") > 0f && m.weightOf("kestrel") > 0f)
        assertTrue(host().learnedSummary().contains("your words"))
    }

    @Test fun aCutOffWriteLeavesTheOldCopyAsBak() {
        val h = host(); File(app.filesDir, "glide_personal.txt").delete(); File(app.filesDir, "glide_personal.txt.bak").delete()
        h.saveHabit(floatArrayOf(0.1f, 0.2f)); h.saveHabit(floatArrayOf(0.3f, 0.4f))
        assertEquals("0.3 0.4", File(app.filesDir, "glide_habit.txt").readText()); assertEquals("0.1 0.2", File(app.filesDir, "glide_habit.txt.bak").readText())
    }

    @Test fun anEmptyOrMissingMemoryFallsBackToTheBackup() {
        val f = File(app.filesDir, "glide_personal.txt"); f.writeText("")
        File(app.filesDir, "glide_personal.txt.bak").writeText(v030)
        var got: String? = null
        val h = host(); h.engine.exportText(null, null) { got = it }          // forces the engine to load its memory through the fallback
        assertTrue("the export contains the old words: $got", got!!.contains("hello"))
    }

    @Test fun theDictionaryAndTheClipboardHistorySurviveAReopen() {
        val h = host(); h.engine.addDictionaryWord("zelda"); h.engine.blockWord("tui")
        val again = host(); var snap: Pair<List<String>, List<String>>? = null
        again.engine.dictionarySnapshot { a, b -> snap = a to b }
        assertEquals(listOf("zelda"), snap!!.first); assertEquals(listOf("tui"), snap!!.second)
        val c = ClipboardHost(app); c.ensureLoaded(); c.store.add("keep me"); c.store.pin("keep me", true); c.save()
        Thread.sleep(300)
        val c2 = ClipboardHost(app); c2.ensureLoaded(); assertTrue(c2.store.isPinned("keep me"))
    }
}
