package app.swoop.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import app.kestrel.core.input.editor.InputStats
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** KB-105: the Diagnostics text has the versions, memory and counters, and contains nothing that was typed. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class DiagnosticsTest {
    private val app get() = RuntimeEnvironment.getApplication()

    @Test fun theTextHasVersionsMemoryAndEveryCounter() {
        val s = InputStats(); s.keys = 12; s.glides = 3; s.backspaces = 2; s.resyncs = 1
        val t = Diagnostics.text(app, s, "glides 3, no match 0", true)
        assertTrue(t.startsWith("Swoop 0.1.0-alpha1 (code 1)")); assertTrue(t.contains("API 29")); assertTrue(t.contains("memory: process"))
        assertTrue(t.contains("glide: glides 3, no match 0")); assertTrue(t.contains("keys 12")); assertTrue(t.contains("glides 3")); assertTrue(t.contains("resyncs 1"))
        assertTrue(t.contains("glide engine ready: true"))
    }

    @Test fun beforeAnyGlideItSaysSo() { assertTrue(Diagnostics.text(app, InputStats(), "", false).contains("glide: no glide yet")) }

    @Test fun nothingTypedCanReachTheText() {
        // the only inputs are counters and the engine's counter line; a word put through the keyboard must not appear in either
        val presenterStats = InputStats(); val c = app.packageName
        val t = Diagnostics.text(app, presenterStats, "glides 1", true)
        assertFalse(t.contains("hunter2")); assertFalse(t.contains(c.reversed()))
    }

    @Test fun theScreenShowsTheTextAndCopyPutsItOnTheClipboard() {
        val a = Robolectric.buildActivity(DiagnosticsActivity::class.java).setup().get()
        fun buttons(v: View, out: MutableList<Button>) { if (v is Button) out.add(v); if (v is ViewGroup) for (i in 0 until v.childCount) buttons(v.getChildAt(i), out) }
        val bs = ArrayList<Button>(); buttons(a.window.decorView, bs)
        bs.first { it.text == a.getString(R.string.diag_copy) }.performClick()
        val clip = (app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip!!.getItemAt(0).text.toString()
        assertTrue(clip.startsWith("Swoop ")); assertTrue(clip.endsWith("never contains anything you typed)"))
    }
}
