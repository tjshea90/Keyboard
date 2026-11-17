package app.swoop.keyboard

import android.app.Application
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import app.kestrel.core.clipboard.ClipboardStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** M2: the clipboard panel replaces the keys: paste by tap, pin, long-press menu, clear all, back, off message. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [29], qualifiers = "w412dp-h915dp-xxhdpi")
class ClipboardPanelTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    private var t = 1_000_000L
    private class Calls : ClipboardPanel.Host { val pasted = ArrayList<String>(); var closed = 0; var changed = 0; var turnedOn = 0
        override fun clipPaste(text: String) { pasted.add(text) }; override fun clipClose() { closed++ }; override fun clipChanged() { changed++ }; override fun clipTurnOn() { turnedOn++ } }

    private fun panel(vararg items: String, on: Boolean = true): Triple<ClipboardPanel, ClipboardStore, Calls> {
        val keys = KeysView(app)
        val p = ClipboardPanel(app, keys); val c = Calls(); p.host = c
        val s = ClipboardStore({ t }); for (i in items) { s.add(i); t += 10 }
        p.store = s; p.historyOn = on
        keys.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)); keys.layout(0, 0, keys.measuredWidth, keys.measuredHeight)
        p.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)); p.layout(0, 0, p.measuredWidth, p.measuredHeight)
        p.open()
        return Triple(p, s, c)
    }
    private fun touch(v: View, a: Int, x: Float, y: Float) { val tt = SystemClock.uptimeMillis(); v.dispatchTouchEvent(MotionEvent.obtain(tt, tt, a, x, y, 0)) }
    private fun tap(v: View, p: Pair<Float, Float>) { touch(v, MotionEvent.ACTION_DOWN, p.first, p.second); touch(v, MotionEvent.ACTION_UP, p.first, p.second) }
    private fun hold(v: View, p: Pair<Float, Float>) { touch(v, MotionEvent.ACTION_DOWN, p.first, p.second); org.robolectric.shadows.ShadowLooper.idleMainLooper(ClipboardPanel.LONG_MS + 50, java.util.concurrent.TimeUnit.MILLISECONDS); touch(v, MotionEvent.ACTION_UP, p.first, p.second) }

    @Test fun newestFirstAndATapPastesTheCard() {
        val (p, _, c) = panel("first", "second")
        assertEquals(2, p.cardCountForTest()); tap(p, p.cardCenterForTest(0)!!); assertEquals(listOf("second"), c.pasted)
    }
    @Test fun theBackButtonLeaves() { val (p, _, c) = panel("x"); tap(p, p.backCenterForTest()); assertEquals(1, c.closed) }
    @Test fun thePinKeepsAnItemAndSortsItFirstAndSurvivesClearAll() {
        val (p, s, c) = panel("a", "b", "c")
        tap(p, p.pinCenterForTest(2)!!); assertTrue(s.isPinned("a")); assertEquals("a", s.items.first().text); assertTrue(c.changed > 0)
        tap(p, p.clearCenterForTest()); assertEquals(listOf("a"), s.items.map { it.text }); assertEquals(1, p.cardCountForTest())
    }
    @Test fun aLongPressOpensPinDeleteAndDeleteRemovesTheItem() {
        val (p, s, _) = panel("keep", "drop")
        val (x, y) = p.cardCenterForTest(0)!!; hold(p, x to y)
        tap(p, (x + 40f) to y)                     // the right half of the open menu is Delete
        assertEquals(listOf("keep"), s.items.map { it.text })
    }
    @Test fun withHistoryOffThePanelOffersToTurnItOn() { val (p, _, c) = panel("x", on = false); assertEquals(0, p.cardCountForTest()); tap(p, p.turnOnCenterForTest()); assertEquals(1, c.turnedOn) }
    @Test fun aTapOnTheMenuOutsideAnyCardDoesNotPaste() { val (p, _, c) = panel("x"); tap(p, 5f to 5f); assertFalse(c.pasted.isNotEmpty()) }
}
