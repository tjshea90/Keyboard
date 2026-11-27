package app.swoop.keyboard

import android.app.Application
import android.graphics.Rect
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import app.kestrel.core.input.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/** N3: the neon bar, detaching with a long press, dragging, resizing by the corners, docking, what the window tells Android. Real touches on the real views (API 29, a 412 x 915 dp phone). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [29], qualifiers = "w412dp-h915dp-xxhdpi")
class KeyboardRootTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val dp get() = app.resources.displayMetrics.density
    private val sw get() = app.resources.displayMetrics.widthPixels
    private val sh get() = app.resources.displayMetrics.heightPixels

    private class Calls : KeyboardRoot.Host {
        val modes = ArrayList<Boolean>(); var geometry = 0; val tall = ArrayList<Boolean>()
        override fun modeChanged(floating: Boolean) { modes.add(floating) }
        override fun windowTall(tall: Boolean) { this.tall.add(tall) }
        override fun geometryChanged() { geometry++ }
    }

    private fun root(prefs: (android.content.SharedPreferences.Editor) -> Unit = {}): Pair<KeyboardRoot, Calls> {
        app.getSharedPreferences(Prefs.FILE, 0).edit().clear().also(prefs).commit()
        val r = KeyboardRoot(app); val c = Calls(); r.host = c
        r.applySettings(Settings(app))
        layout(r)
        return r to c
    }
    /** What the input method window does: the window is as tall as the screen while floating (or while a docked height drag needs room to grow), as the keyboard while docked. */
    private fun layout(r: KeyboardRoot) {
        val floating = r.windowTall
        r.measure(View.MeasureSpec.makeMeasureSpec(sw, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(sh, if (floating) View.MeasureSpec.EXACTLY else View.MeasureSpec.AT_MOST))
        r.layout(0, 0, r.measuredWidth, r.measuredHeight)
    }
    private fun advance(ms: Long) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms)) }
    /** One touch event; the window lays the keyboard out again before the next one, as it does on a device. */
    private fun touch(v: View, a: Int, x: Float, y: Float) { val t = SystemClock.uptimeMillis(); v.dispatchTouchEvent(MotionEvent.obtain(t, t, a, x, y, 0)); if (v is KeyboardRoot) layout(v) }
    private fun gripCentre(r: KeyboardRoot): Pair<Float, Float> { val p = r.panelRect(); return (p.centerX().toFloat()) to (p.top + r.grip.top + r.grip.height / 2f) }

    /** Long press on the bar with the finger staying put, then [moves] as MOVE events, then lift; the root is laid out again after each step like the window would. */
    private fun detach(r: KeyboardRoot, x: Float, y: Float) { touch(r, MotionEvent.ACTION_DOWN, x, y); advance(GripBar.LONG_PRESS_MS + 30); layout(r) }

    @Test fun dockedItIsTheNormalKeyboardWithANeonBarOnTop() {
        val (r, c) = root()
        val p = r.panelRect()
        assertFalse(r.floating); assertEquals(0, p.left); assertEquals(sw, p.right); assertEquals(r.height, p.bottom)
        assertEquals(View.VISIBLE, r.grip.visibility); assertTrue((r.grip.height) >= (GripBar.HEIGHT_DP * dp).toInt() - 1)
        assertTrue("no mode change reported", c.modes.isEmpty())
    }

    /** The bug that this test exists for: the keyboard window is measured "at most the screen"; the overlay layer used to fill all of it, so the keyboard covered the whole app. */
    @Test fun theDockedKeyboardIsOnlyAsTallAsItsKeysNotTheWholeScreen() {
        val (r, _) = root()
        val keysH = r.keys.measuredHeight; val gripH = r.grip.measuredHeight
        assertEquals("the window is the keys plus the bar", keysH + gripH, r.height); assertTrue("${r.height} of $sh px", r.height < sh / 2)
        assertEquals(keysH, r.overlay.measuredHeight); assertEquals(r.keys.measuredWidth, r.overlay.measuredWidth)
    }

    @Test fun theOldSingleViewKeyboardWasAlsoNotTheWholeScreen() {
        val v = KeysView(app); v.measure(View.MeasureSpec.makeMeasureSpec(sw, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(sh, View.MeasureSpec.AT_MOST)); assertTrue(v.measuredHeight < sh / 2)
    }

    @Test fun aLongPressOnTheBarLiftsTheKeyboardOffAndItFollowsTheFinger() {
        val (r, c) = root()
        val (gx, gy) = gripCentre(r)
        detach(r, gx, gy)
        assertTrue(r.floating); assertEquals(listOf(true), c.modes)
        val p = r.panelRect()
        assertEquals("floats at its own width, not full width", Math.round(360 * dp), p.width()); assertTrue(p.width() < sw)
        assertTrue("the bar is under the finger (screen position)", Math.abs((p.left + p.right) / 2f - gx) <= 2f * dp)
        assertEquals("the window is the whole screen now", sh, r.height)
        // drag: the keyboard follows
        val before = r.panelRect()
        touch(r, MotionEvent.ACTION_MOVE, gx - 50f, gy + 300f); layout(r)
        val after = r.panelRect()
        assertTrue("${after.left - before.left}", Math.abs(after.left - before.left + 50) <= 2); assertTrue("${after.top - before.top}", Math.abs(after.top - before.top - 300) <= 2)
        touch(r, MotionEvent.ACTION_UP, gx - 50f, gy + 300f); layout(r)
        assertTrue("a drop in the middle keeps it floating", r.floating); assertEquals(1, c.geometry)
        assertTrue(Settings(app).floating)
    }

    @Test fun theKeyboardCanNeverBeDraggedOffTheScreen() {
        val (r, _) = root(); val (gx, gy) = gripCentre(r); detach(r, gx, gy)
        for ((dx, dy) in listOf(-5000f to -5000f, 5000f to -5000f, -5000f to 100f, 5000f to 100f)) {
            touch(r, MotionEvent.ACTION_MOVE, gx + dx, gy + dy); layout(r)
            val p = r.panelRect(); assertTrue("$p", p.left >= 0 && p.top >= 0 && p.right <= sw && p.bottom <= sh)
        }
    }

    @Test fun anUpwardDragOnTheBarDetachesWithoutWaiting() {
        val (r, c) = root(); val (gx, gy) = gripCentre(r)
        touch(r, MotionEvent.ACTION_DOWN, gx, gy)
        touch(r, MotionEvent.ACTION_MOVE, gx, gy - 12 * dp); layout(r); assertFalse("12 dp is not enough", r.floating)
        touch(r, MotionEvent.ACTION_MOVE, gx, gy - 40 * dp); layout(r)
        assertTrue(r.floating); assertEquals(listOf(true), c.modes)
    }

    @Test fun aSidewaysSlideOnTheDockedBarDoesNotDetach() {
        val (r, _) = root(); val (gx, gy) = gripCentre(r)
        touch(r, MotionEvent.ACTION_DOWN, gx, gy); touch(r, MotionEvent.ACTION_MOVE, gx + 60 * dp, gy); advance(GripBar.LONG_PRESS_MS + 50); touch(r, MotionEvent.ACTION_UP, gx + 60 * dp, gy)
        assertFalse(r.floating)
    }

    @Test fun aLongPressReleasedAtOnceLeavesItFloatingEvenAtTheBottom() {
        val (r, _) = root(); val (gx, gy) = gripCentre(r); detach(r, gx, gy); touch(r, MotionEvent.ACTION_UP, gx, gy)
        assertTrue(r.floating)
    }

    /** Found on the emulator: a finger held still after the lift-off made the panel creep up a pixel at a time (rounding in the stored position, an estimated height). */
    @Test fun aFingerHeldStillNeverMovesTheFloatingKeyboard() {
        val (r, _) = root(); val (gx, gy) = gripCentre(r); detach(r, gx, gy)
        val before = r.panelRect()
        repeat(60) { touch(r, MotionEvent.ACTION_MOVE, gx, gy) }
        assertEquals(before, r.panelRect())
        repeat(60) { layout(r) }
        assertEquals(before, r.panelRect())
        touch(r, MotionEvent.ACTION_UP, gx, gy); assertEquals(before, r.panelRect())
    }

    @Test fun aTapOrAShortHoldDoesNothing() {
        val (r, _) = root(); val (gx, gy) = gripCentre(r)
        touch(r, MotionEvent.ACTION_DOWN, gx, gy); advance(120); touch(r, MotionEvent.ACTION_UP, gx, gy); assertFalse(r.floating)
    }

    @Test fun aDoubleTapOnTheBarDocksAgainAndAFarDropAtTheBottomToo() {
        val (r, c) = root(); val (gx, gy) = gripCentre(r); detach(r, gx, gy); touch(r, MotionEvent.ACTION_UP, gx, gy); layout(r)
        assertTrue(r.floating)
        val (x, y) = gripCentre(r)
        touch(r, MotionEvent.ACTION_DOWN, x, y); touch(r, MotionEvent.ACTION_UP, x, y); touch(r, MotionEvent.ACTION_DOWN, x, y); touch(r, MotionEvent.ACTION_UP, x, y); layout(r)
        assertFalse("two quick taps dock it", r.floating); assertEquals(listOf(true, false), c.modes); assertFalse(Settings(app).floating)
        // and: lift off, drag all the way down, let go
        val (a, b) = gripCentre(r); detach(r, a, b)
        touch(r, MotionEvent.ACTION_MOVE, a, sh.toFloat() + 400f); layout(r); touch(r, MotionEvent.ACTION_UP, a, sh.toFloat()); layout(r)
        assertFalse("dropped on the bottom edge: docked", r.floating)
    }

    private fun corner(r: KeyboardRoot, n: Int): Pair<Float, Float> {
        val p = r.panelRect(); val o = 6 * dp
        return (if (n == 0 || n == 2) p.left - o else p.right + o) to (if (n == 0 || n == 1) p.top - o else p.bottom + o)
    }

    @Test fun aCornerDragResizesAndTheOppositeCornerStaysStill() {
        val (r, c) = root(); val (gx, gy) = gripCentre(r); detach(r, gx, gy)
        touch(r, MotionEvent.ACTION_MOVE, gx, gy - 400f); touch(r, MotionEvent.ACTION_UP, gx, gy - 400f)
        val (cx, cy) = corner(r, 3)
        assertEquals(3, r.cornerAt(cx, cy))
        touch(r, MotionEvent.ACTION_DOWN, cx, cy); touch(r, MotionEvent.ACTION_MOVE, cx - 200f, cy - 100f); touch(r, MotionEvent.ACTION_UP, cx - 200f, cy - 100f)
        val before = r.panelRect(); assertTrue("narrower: ${Math.round(360 * dp)} -> ${before.width()}", Math.abs(Math.round(360 * dp) - 200 - before.width()) <= 3)
        val (dx, dy) = corner(r, 3)
        touch(r, MotionEvent.ACTION_DOWN, dx, dy); touch(r, MotionEvent.ACTION_MOVE, dx + 120f, dy + 90f); touch(r, MotionEvent.ACTION_UP, dx + 120f, dy + 90f)
        val after = r.panelRect()
        assertTrue("bottom right keeps the top left still: $before $after", Math.abs(before.left - after.left) <= 2 && Math.abs(before.top - after.top) <= 2)
        assertTrue("wider: ${before.width()} -> ${after.width()}", after.width() > before.width()); assertTrue("taller: ${before.height()} -> ${after.height()}", after.height() > before.height())
        assertTrue(c.geometry >= 2); assertTrue(Settings(app).floatWDp > 100)
        // top left: the bottom right stays
        val b2 = r.panelRect(); val (tx, ty) = corner(r, 0)
        touch(r, MotionEvent.ACTION_DOWN, tx, ty); touch(r, MotionEvent.ACTION_MOVE, tx + 60f, ty + 40f); layout(r); touch(r, MotionEvent.ACTION_UP, tx + 60f, ty + 40f); layout(r)
        val a2 = r.panelRect(); assertTrue("${b2.right} ${a2.right}", Math.abs(b2.right - a2.right) <= 2); assertTrue(a2.width() < b2.width())
    }

    @Test fun theSizeStopsAtItsLimits() {
        val (r, _) = root(); val (gx, gy) = gripCentre(r); detach(r, gx, gy); touch(r, MotionEvent.ACTION_UP, gx, gy); layout(r)
        var (cx, cy) = corner(r, 3)
        touch(r, MotionEvent.ACTION_DOWN, cx, cy); touch(r, MotionEvent.ACTION_MOVE, cx - 3000f, cy - 3000f); layout(r); touch(r, MotionEvent.ACTION_UP, cx, cy); layout(r)
        assertEquals("narrowest", Math.round(232 * dp), r.panelRect().width())
        val q = corner(r, 3); cx = q.first; cy = q.second
        touch(r, MotionEvent.ACTION_DOWN, cx, cy); touch(r, MotionEvent.ACTION_MOVE, cx + 3000f, cy + 3000f); layout(r); touch(r, MotionEvent.ACTION_UP, cx, cy); layout(r)
        val p = r.panelRect(); assertTrue("widest: ${p.width()}", p.width() <= sw && p.width() <= Math.round(620 * dp) + 1); assertTrue(p.right <= sw && p.bottom <= sh)
    }

    @Test fun theCornerKeysOfTheKeyboardAreNotTaken() {
        val (r, _) = root(); val (gx, gy) = gripCentre(r); detach(r, gx, gy); touch(r, MotionEvent.ACTION_UP, gx, gy); layout(r)
        val p = r.panelRect()
        assertEquals("a point inside the keys' area near the corner is the key's", -1, r.cornerAt(p.right - 14 * dp, p.bottom - 14 * dp))
        assertEquals(-1, r.cornerAt(p.centerX().toFloat(), p.centerY().toFloat()))
        assertEquals(-1, r.cornerAt(p.centerX().toFloat(), p.top - 200f))
    }

    @Test fun onlyThePanelAndItsCornersTakeTouchesWhileFloating() {
        val (r, _) = root(); val (gx, gy) = gripCentre(r); detach(r, gx, gy); touch(r, MotionEvent.ACTION_UP, gx, gy); layout(r)
        val reg = r.touchRegion(); val p = r.panelRect()
        assertTrue(reg.contains(p.centerX(), p.centerY())); assertTrue(reg.contains(p.right + 10, p.bottom + 10))
        assertFalse("the app behind gets the rest", reg.contains(p.centerX(), p.top - 100)); assertFalse(reg.contains(2, 2))
    }

    @Test fun itFloatsAgainWhereItWasLeftAndASettingTurnsTheFeatureOff() {
        val (r, _) = root(); val (gx, gy) = gripCentre(r); detach(r, gx, gy); touch(r, MotionEvent.ACTION_MOVE, gx + 80f, gy - 500f); touch(r, MotionEvent.ACTION_UP, gx + 80f, gy - 500f); layout(r)
        val where = r.panelRect()
        val again = KeyboardRoot(app); val c2 = Calls(); again.host = c2; again.applySettings(Settings(app)); layout(again)
        assertTrue(again.floating); assertEquals(listOf(true), c2.modes)
        val w2 = again.panelRect(); assertTrue("same place: $where $w2", Math.abs(where.left - w2.left) <= 2 && Math.abs(where.top - w2.top) <= 2 && where.width() == w2.width())
        app.getSharedPreferences(Prefs.FILE, 0).edit().putBoolean(Prefs.FLOAT_ON, false).commit()
        again.applySettings(Settings(app)); layout(again)
        assertFalse(again.floating); assertEquals(View.GONE, again.grip.visibility)
    }

    @Test fun safeModeIsAlwaysDocked() {
        app.getSharedPreferences(Prefs.FILE, 0).edit().clear().putBoolean(Prefs.FLOATING, true).commit()
        val r = KeyboardRoot(app); r.applySettings(Settings(app), safe = true); layout(r)
        assertFalse(r.floating); assertEquals(View.GONE, r.grip.visibility)
    }

    @Test fun theSettingsScreensCopyShowsTheBarButNeverFloats() {
        val (r, _) = root { it.putBoolean(Prefs.FLOATING, true) }
        val p = KeyboardRoot(app); p.previewOnly = true; p.applySettings(Settings(app)); layout(p)
        assertFalse(p.floating); assertEquals(View.VISIBLE, p.grip.visibility)
        assertTrue(r.floating)
    }

    // ------------------------------------------------------------------ M8: squeeze the docked keyboard and move it

    private fun two(v: View, action: Int, idx: Int, x0: Float, y0: Float, x1: Float, y1: Float) {
        val props = Array(2) { MotionEvent.PointerProperties().apply { id = it; toolType = MotionEvent.TOOL_TYPE_FINGER } }
        val coords = arrayOf(MotionEvent.PointerCoords().apply { x = x0; y = y0 }, MotionEvent.PointerCoords().apply { x = x1; y = y1 })
        val t = SystemClock.uptimeMillis()
        val a = if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP) action or (idx shl MotionEvent.ACTION_POINTER_INDEX_SHIFT) else action
        v.dispatchTouchEvent(MotionEvent.obtain(t, t, a, 2, props, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)); if (v is KeyboardRoot) layout(v)
    }

    @Test fun draggingTheRightArrowOfTheBarInwardsSqueezesTheKeyboardAndItIsSaved() {
        val (r, c) = root(); val p = r.panelRect(); val y = p.top + r.grip.top + r.grip.height / 2f
        touch(r, MotionEvent.ACTION_DOWN, p.right - 20 * dp, y)
        touch(r, MotionEvent.ACTION_MOVE, p.right - 120 * dp, y); touch(r, MotionEvent.ACTION_MOVE, p.right - 160 * dp, y); touch(r, MotionEvent.ACTION_UP, p.right - 160 * dp, y)
        val q = r.panelRect()
        assertTrue("narrower: ${q.width()} of $sw", q.width() < sw - 100 * dp); assertEquals("the left edge stays", 0, q.left)
        assertTrue(r.squeezed); assertTrue("the keys followed", r.keys.measuredWidth < sw - 100 * dp)
        assertEquals(Settings(app).dockFrac, Math.round(q.width() * 1000f / sw)); assertTrue(c.geometry > 0)
    }

    @Test fun pinchingTheBarSqueezesFromBothSidesAndItNeverGetsNarrowerThanHalfTheScreen() {
        val (r, _) = root(); val p = r.panelRect(); val y = p.top + r.grip.top + r.grip.height / 2f
        two(r, MotionEvent.ACTION_DOWN, 0, 40f * dp, y, 40f * dp, y)
        two(r, MotionEvent.ACTION_POINTER_DOWN, 1, 40f * dp, y, 372f * dp, y)
        two(r, MotionEvent.ACTION_MOVE, 0, 120f * dp, y, 290f * dp, y)
        val mid = r.panelRect(); assertTrue("squeezed from both sides: $mid", mid.left > 0 && mid.right < sw)
        two(r, MotionEvent.ACTION_MOVE, 0, 200f * dp, y, 206f * dp, y)
        assertEquals("not below half the screen", sw / 2f, r.panelRect().width().toFloat(), 2f)
        two(r, MotionEvent.ACTION_POINTER_UP, 1, 200f * dp, y, 206f * dp, y)
    }

    @Test fun aSqueezedKeyboardSlidesLeftAndRightWithTheBarAndDoubleTapMakesItFullWidthAgain() {
        val (r, _) = root { it.putInt(Prefs.DOCK_FRAC, 700); it.putInt(Prefs.DOCK_X, 0) }
        assertTrue(r.squeezed); assertEquals(0, r.panelRect().left)
        val p = r.panelRect(); val y = p.top + r.grip.top + r.grip.height / 2f; val cx = p.centerX().toFloat()
        touch(r, MotionEvent.ACTION_DOWN, cx, y); touch(r, MotionEvent.ACTION_MOVE, cx + 30 * dp, y); touch(r, MotionEvent.ACTION_MOVE, cx + 90 * dp, y); touch(r, MotionEvent.ACTION_UP, cx + 90 * dp, y)
        val moved = r.panelRect(); assertTrue("moved right: $moved", moved.left > 40 * dp); assertEquals("same width", p.width(), moved.width())
        assertEquals(Settings(app).dockX.toDouble(), Math.round(moved.left * 1000f / (sw - moved.width())).toDouble(), 3.0)
        val q = r.panelRect(); val cy = q.top + r.grip.top + r.grip.height / 2f
        touch(r, MotionEvent.ACTION_DOWN, q.centerX().toFloat(), cy); touch(r, MotionEvent.ACTION_UP, q.centerX().toFloat(), cy)
        touch(r, MotionEvent.ACTION_DOWN, q.centerX().toFloat(), cy); touch(r, MotionEvent.ACTION_UP, q.centerX().toFloat(), cy)
        assertFalse(r.squeezed); assertEquals(sw, r.panelRect().width()); assertEquals(1000, Settings(app).dockFrac)
    }

    @Test fun theSqueezedKeyboardKeepsTypingAndOnlyItTakesTouches() {
        val (r, _) = root { it.putInt(Prefs.DOCK_FRAC, 600); it.putInt(Prefs.DOCK_X, 1000) }
        val got = ArrayList<String>(); r.keys.listener = object : KeysView.Listener { override fun onKey(key: app.kestrel.core.input.Key) { (key as? app.kestrel.core.input.Key.Ch)?.let { got.add(it.text) } }; override fun onCursor(steps: Int) {} }
        val (x, y) = r.keys.centerOf("q")!!; val ox = r.keys.left + r.panelRect().left
        val t = SystemClock.uptimeMillis(); r.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x + ox, y + r.panelRect().top + r.grip.height, 0)); r.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_UP, x + ox, y + r.panelRect().top + r.grip.height, 0))
        assertEquals(listOf("q"), got)
        val reg = r.touchRegion(); assertTrue(reg.contains(r.panelRect().centerX(), r.panelRect().centerY())); assertFalse("the left side belongs to the app", reg.contains(10, r.panelRect().centerY()))
    }

    // ------------------------------------------------------------------ R18c: snap back to full size, and a resize without jitter

    @Test fun aSqueezedKeyboardShowsTheFullSizePillAndOneTapMakesItFullWidth() {
        val (r, c) = root { it.putInt(Prefs.DOCK_FRAC, 700); it.putInt(Prefs.DOCK_X, 300) }
        assertTrue(r.squeezed); assertTrue("the bar offers the way back", r.gripCanRestore())
        val before = c.geometry; val (x, y) = gripCentre(r)
        touch(r, MotionEvent.ACTION_DOWN, x, y); touch(r, MotionEvent.ACTION_UP, x, y)
        assertFalse("one tap is enough", r.squeezed); assertEquals(sw, r.panelRect().width()); assertEquals(1000, Settings(app).dockFrac); assertTrue("the glide decoder is told", c.geometry > before)
        assertFalse("nothing to restore any more", r.gripCanRestore())
    }

    @Test fun aFullWidthDockedKeyboardHasNoFullSizePill() { val (r, _) = root(); assertFalse(r.squeezed); assertFalse(r.gripCanRestore()) }

    @Test fun aFloatingKeyboardIsSnappedBackToTheFullWidthDockedOneWithOneTap() {
        val (r, c) = root { it.putInt(Prefs.DOCK_FRAC, 700) }
        val (gx, gy) = gripCentre(r); detach(r, gx, gy); touch(r, MotionEvent.ACTION_UP, gx, gy); layout(r)
        assertTrue(r.floating); assertTrue(r.gripCanRestore())
        val (x, y) = gripCentre(r)
        touch(r, MotionEvent.ACTION_DOWN, x, y); touch(r, MotionEvent.ACTION_UP, x, y); layout(r)
        assertFalse("docked again", r.floating); assertFalse("and full width, not the old squeeze", r.squeezed); assertEquals(sw, r.panelRect().width())
        assertEquals(listOf(true, false), c.modes); assertEquals(1000, Settings(app).dockFrac); assertFalse(Settings(app).floating)
    }

    @Test fun whileTheFingerDragsTheArrowOnlyAPictureChangesAndTheLayoutHappensOnceAtTheEnd() {
        val (r, _) = root(); val p = r.panelRect(); val y = p.top + r.grip.top + r.grip.height / 2f
        touch(r, MotionEvent.ACTION_DOWN, p.right - 20 * dp, y)
        for (i in 1..30) touch(r, MotionEvent.ACTION_MOVE, p.right - 20 * dp - i * 6 * dp, y)
        assertTrue("the picture follows the finger: ${r.panelRect()}", r.panelRect().width() < sw - 100 * dp)
        assertEquals("nothing was measured or laid out again meanwhile", sw, r.keys.width); assertEquals(sw, r.keys.measuredWidth)
        val shown = r.panelRect()
        touch(r, MotionEvent.ACTION_UP, p.right - 20 * dp - 180 * dp, y)
        assertTrue("laid out for real now: ${r.keys.width}", r.keys.width < sw - 100 * dp)
        assertEquals("and exactly as the picture was", shown.width(), r.panelRect().width()); assertEquals(shown.left, r.panelRect().left)
    }

    @Test fun aCornerDragIsExactNeverMovesTheOppositeEdgeAndNeverGoesBackwards() {
        for (n in 0..3) {
            val (r, _) = root(); val (gx, gy) = gripCentre(r); detach(r, gx, gy); touch(r, MotionEvent.ACTION_MOVE, gx, gy - 300f); touch(r, MotionEvent.ACTION_UP, gx, gy - 300f); layout(r)
            val start = r.panelRect(); val (cx, cy) = corner(r, n)
            val sx = if (n == 0 || n == 2) -1f else 1f; val sy = if (n == 0 || n == 1) -1f else 1f        // outwards
            touch(r, MotionEvent.ACTION_DOWN, cx, cy)
            var lastW = start.width(); var lastH = start.height(); var last: Rect? = null
            for (i in 1..50) {
                val mx = cx - sx * i * 2.5f; val my = cy - sy * i * 2f                                      // inwards, a little each time
                touch(r, MotionEvent.ACTION_MOVE, mx, my)
                val q = r.panelRect()
                assertTrue("corner $n step $i: width never grows while the finger goes in: $lastW -> ${q.width()}", q.width() <= lastW); assertTrue("height too: $lastH -> ${q.height()}", q.height() <= lastH)
                if (n == 0 || n == 2) assertEquals("corner $n step $i: the right edge stays exactly", start.right, q.right) else assertEquals("corner $n step $i: the left edge stays exactly", start.left, q.left)
                if (n == 0 || n == 1) assertEquals("corner $n step $i: the bottom edge stays exactly", start.bottom, q.bottom) else assertEquals("corner $n step $i: the top edge stays exactly", start.top, q.top)
                touch(r, MotionEvent.ACTION_MOVE, mx, my); assertEquals("the same finger position is the same picture", q, r.panelRect())
                lastW = q.width(); lastH = q.height(); last = q
            }
            touch(r, MotionEvent.ACTION_UP, cx - sx * 125f, cy - sy * 100f)
            val fin = r.panelRect()
            assertTrue("corner $n: laid out where the picture was (${last}) -> $fin", Math.abs(fin.width() - last!!.width()) <= 2 && Math.abs(fin.height() - last.height()) <= 3 && Math.abs(fin.left - last.left) <= 2 && Math.abs(fin.bottom - last.bottom) <= 3 && Math.abs(fin.top - last.top) <= 3)
        }
    }

    // ------------------------------------------------------------------ R19c: resize the docked keyboard both ways (owner 2026-11-18)

    private fun arrowY(r: KeyboardRoot): Float { val p = r.panelRect(); return p.top + r.grip.top + r.grip.height / 2f }

    @Test fun draggingAnArrowUpAndInwardsMakesTheDockedKeyboardTallerAndNarrowerAndStaysOnTheBottom() {
        val (r, c) = root(); val p = r.panelRect(); val y = arrowY(r); val h0 = p.height(); val keysH0 = r.keys.height
        touch(r, MotionEvent.ACTION_DOWN, p.right - 20 * dp, y)
        touch(r, MotionEvent.ACTION_MOVE, p.right - 60 * dp, y - 30 * dp); touch(r, MotionEvent.ACTION_MOVE, p.right - 120 * dp, y - 90 * dp)
        assertTrue("the window is the whole screen so it can grow upwards", r.windowTall); assertEquals(listOf(true), c.tall)
        val shown = r.panelRect()
        assertTrue("taller while dragging: ${shown.height()} vs $h0", shown.height() > h0 + 40 * dp); assertTrue("narrower too: ${shown.width()}", shown.width() < sw - 80 * dp)
        assertEquals("the bottom edge stays on the bottom of the screen", sh, shown.bottom); assertEquals("the left edge stays", 0, shown.left)
        assertEquals("only a picture meanwhile: nothing was laid out again", keysH0, r.keys.height)
        touch(r, MotionEvent.ACTION_UP, p.right - 120 * dp, y - 90 * dp)
        assertFalse("the window is the keyboard's height again", r.windowTall); assertEquals(listOf(true, false), c.tall)
        val q = r.panelRect()
        assertEquals("laid out as the picture showed", shown.height().toFloat(), q.height().toFloat(), 2f); assertEquals(shown.width(), q.width()); assertEquals(0, q.left)
        assertEquals("on the bottom edge of its window", r.height, q.bottom); assertTrue("the keys really are taller: ${r.keys.height} vs $keysH0", r.keys.height > keysH0 + 30 * dp)
        assertTrue(r.squeezed); assertTrue(Settings(app).dockH > 1000); assertEquals(Math.round(q.width() * 1000f / sw), Settings(app).dockFrac); assertTrue("the glide decoder is told", c.geometry > 0)
    }

    @Test fun draggingAnArrowStraightUpMakesAFullWidthKeyboardTallerAndDownShorter() {
        val (r, _) = root(); val p = r.panelRect(); val y = arrowY(r); val h0 = p.height()
        touch(r, MotionEvent.ACTION_DOWN, p.left + 20 * dp, y)
        touch(r, MotionEvent.ACTION_MOVE, p.left + 20 * dp, y - 40 * dp); touch(r, MotionEvent.ACTION_MOVE, p.left + 20 * dp, y - 100 * dp); touch(r, MotionEvent.ACTION_UP, p.left + 20 * dp, y - 100 * dp)
        val tall = r.panelRect(); assertEquals("still full width", sw, tall.width()); assertTrue("taller: ${tall.height()} vs $h0", tall.height() > h0 + 50 * dp); assertFalse(r.squeezed)
        // and down again (the arrow on the other side this time)
        val y2 = arrowY(r)
        touch(r, MotionEvent.ACTION_DOWN, tall.right - 20 * dp, y2)
        touch(r, MotionEvent.ACTION_MOVE, tall.right - 20 * dp, y2 + 40 * dp); touch(r, MotionEvent.ACTION_MOVE, tall.right - 20 * dp, y2 + 150 * dp); touch(r, MotionEvent.ACTION_UP, tall.right - 20 * dp, y2 + 150 * dp)
        val short = r.panelRect(); assertTrue("shorter than before: ${short.height()} vs ${tall.height()}", short.height() < tall.height() - 50 * dp); assertEquals(sw, short.width())
        assertTrue("never below the lowest rows: ${Settings(app).dockH}", Settings(app).dockH >= 450)
    }

    @Test fun theHeightStopsAtItsLimitsAndNeverFillsTheScreen() {
        val (r, _) = root(); val p = r.panelRect(); val y = arrowY(r)
        touch(r, MotionEvent.ACTION_DOWN, p.right - 20 * dp, y); touch(r, MotionEvent.ACTION_MOVE, p.right - 20 * dp, y - 40 * dp); touch(r, MotionEvent.ACTION_MOVE, p.right - 20 * dp, y - 5000 * dp); touch(r, MotionEvent.ACTION_UP, p.right - 20 * dp, y - 5000 * dp)
        val q = r.panelRect(); assertTrue("at most 60 % of the screen: ${q.height()} of $sh", q.height() <= 0.6f * sh + 2); assertTrue(Settings(app).dockH <= 1800)
        val z = arrowY(r)
        touch(r, MotionEvent.ACTION_DOWN, q.right - 20 * dp, z); touch(r, MotionEvent.ACTION_MOVE, q.right - 20 * dp, z + 40 * dp); touch(r, MotionEvent.ACTION_MOVE, q.right - 20 * dp, z + 5000 * dp); touch(r, MotionEvent.ACTION_UP, q.right - 20 * dp, z + 5000 * dp)
        val s2 = r.panelRect(); assertTrue("some keyboard is left: ${s2.height()}", s2.height() > 100 * dp); assertEquals(450, Settings(app).dockH)
        assertTrue("clearly lower than the old floor of 0.7 rows (R23e): ${s2.height()} px", s2.height() < 0.8f * firstFloorHeight())
    }

    /** What the lowest docked keyboard was before R23e: rows at 0.7 of their height, at the full width. */
    private fun firstFloorHeight(): Int { val (r, _) = root { it.putInt(Prefs.DOCK_H, 700) }; return r.panelRect().height() }

    @Test fun aLittleDriftWhileSqueezingSidewaysChangesNeitherTheHeightNorTheWindow() {
        val (r, c) = root(); val p = r.panelRect(); val y = arrowY(r)
        touch(r, MotionEvent.ACTION_DOWN, p.right - 20 * dp, y)
        for (i in 1..20) touch(r, MotionEvent.ACTION_MOVE, p.right - 20 * dp - i * 8 * dp, y - minOf(i, 6) * 1.5f * dp)         // the finger wanders 9 dp up while it goes 160 dp inwards
        assertFalse("the window stays the keyboard's own", r.windowTall); assertTrue(c.tall.isEmpty())
        touch(r, MotionEvent.ACTION_UP, p.right - 180 * dp, y - 9 * dp)
        assertTrue(r.squeezed); assertEquals("no height was asked for", 1000, Settings(app).dockH)
    }

    @Test fun theFullSizePillBringsBackTheHeightToo() {
        val (r, c) = root { it.putInt(Prefs.DOCK_H, 1500) }
        assertTrue("a taller keyboard offers the way back, though it is full width", r.gripCanRestore()); assertFalse(r.squeezed)
        val tall = r.panelRect().height(); val (x, y) = gripCentre(r); val g0 = c.geometry
        touch(r, MotionEvent.ACTION_DOWN, x, y); touch(r, MotionEvent.ACTION_UP, x, y)
        assertTrue("shorter again: ${r.panelRect().height()} vs $tall", r.panelRect().height() < tall - 30 * dp); assertEquals(1000, Settings(app).dockH); assertFalse(r.gripCanRestore()); assertTrue(c.geometry > g0)
    }

    @Test fun aDoubleTapOnTheBarAlsoRestoresTheHeight() {
        val (r, _) = root { it.putInt(Prefs.DOCK_H, 800); it.putInt(Prefs.DOCK_FRAC, 700) }
        assertTrue(r.gripCanRestore()); val q = r.panelRect(); val cy = q.top + r.grip.top + r.grip.height / 2f; val cx = q.left + 75 * dp           // (beside the pill, outside the arrow: a single tap on the pill restores at once)
        touch(r, MotionEvent.ACTION_DOWN, cx, cy); touch(r, MotionEvent.ACTION_UP, cx, cy); touch(r, MotionEvent.ACTION_DOWN, cx, cy); touch(r, MotionEvent.ACTION_UP, cx, cy)
        assertFalse(r.gripCanRestore()); assertEquals(1000, Settings(app).dockH); assertEquals(1000, Settings(app).dockFrac)
    }

    @Test fun theSavedHeightComesBackWhenTheKeyboardOpensAgainAndFloatingKeepsItsOwnHeight() {
        val (r, _) = root { it.putInt(Prefs.DOCK_H, 1400) }
        val h = r.panelRect().height(); val (p, _) = root { it.putInt(Prefs.DOCK_H, 1000) }
        assertTrue("1.4 rows are taller than 1.0: $h vs ${p.panelRect().height()}", h > p.panelRect().height() + 40 * dp)
        val (f, _) = root { it.putInt(Prefs.DOCK_H, 1400); it.putBoolean(Prefs.FLOATING, true); it.putInt(Prefs.FLOAT_H, 100) }
        assertTrue(f.floating); assertEquals("the floating keyboard has its own height setting", 1f, f.keys.floatMul, 0.001f)
    }

    @Test fun aPinchKeepsTheHeight() {
        val (r, _) = root { it.putInt(Prefs.DOCK_H, 1300) }; val p = r.panelRect(); val y = arrowY(r)
        two(r, MotionEvent.ACTION_DOWN, 0, 40f * dp, y, 40f * dp, y); two(r, MotionEvent.ACTION_POINTER_DOWN, 1, 40f * dp, y, 372f * dp, y); two(r, MotionEvent.ACTION_MOVE, 0, 120f * dp, y, 290f * dp, y)
        two(r, MotionEvent.ACTION_POINTER_UP, 1, 120f * dp, y, 290f * dp, y)
        assertFalse("a pinch needs no room above", r.windowTall); assertTrue(r.squeezed); assertEquals("the height setting is kept", 1300, Settings(app).dockH)
    }

    /** Found while testing R19c: the overlay (a sibling of the keys, measured like them) kept its old height, so the panel stayed tall when the keys got shorter. */
    @Test fun aShorterKeyboardReallyShrinksThePanelAndTheOverlay() {
        val (r, _) = root { it.putInt(Prefs.DOCK_H, 1500) }; val tall = r.panelRect().height()
        r.keys.floatMul = 1f; layout(r)
        assertTrue("the panel follows the keys: ${r.panelRect().height()} vs $tall", r.panelRect().height() < tall - 100 * dp)
        assertEquals("the overlay is exactly as tall as the keys", r.keys.height, r.overlay.height); assertEquals(r.keys.height + r.grip.height, r.panelRect().height())
    }

    // ---- R38 (2026-11-27): glides that start at the screen's edge were taken by the system's "back" swipe

    @Test fun theKeyboardKeepsTheSystemBackSwipeOffItsLeftAndRightEdges() {
        val (r, _) = root()
        val p = r.panelRect(); val rects = r.systemGestureExclusionRects
        assertTrue("a strip at the left edge of the keyboard: $rects", rects.any { it.left <= p.left && it.right >= p.left + 30 * dp && it.top <= p.top + p.height() / 2 && it.bottom >= p.bottom - 1 })
        assertTrue("a strip at the right edge of the keyboard: $rects", rects.any { it.right >= p.right && it.left <= p.right - 30 * dp && it.top <= p.top + p.height() / 2 && it.bottom >= p.bottom - 1 })
        assertTrue("only the edges, not the whole keyboard: $rects", rects.all { it.width() <= p.width() / 2 })
    }
    @Test fun theEdgeStripsFollowAKeyboardThatWasSqueezedOrFloated() {
        val (r, _) = root { it.putInt(Prefs.DOCK_FRAC, 600); it.putInt(Prefs.DOCK_X, 500) }
        val p = r.panelRect(); val rects = r.systemGestureExclusionRects
        assertTrue("the strips are the squeezed keyboard's own edges: $p $rects", rects.all { it.left >= p.left && it.right <= p.right })
    }

    @Test fun aTouchRightBesideASqueezedKeyboardIsTheKeysOwnAndTypesTheNearestKey() {
        val (r, _) = root { it.putInt(Prefs.DOCK_FRAC, 600); it.putInt(Prefs.DOCK_X, 500) }
        val typed = ArrayList<String>(); r.keys.listener = object : KeysView.Listener { override fun onKey(key: Key) { typed.add((key as? Key.Ch)?.text ?: key.toString()) }; override fun onCursor(steps: Int) {} }
        val p = r.panelRect(); assertTrue("squeezed: ${p.left}", p.left > 40 * dp)
        val (qx, qy) = r.keys.centerOf("q")!!; val y = p.top + r.grip.height + r.keys.top.toFloat() + qy
        touch(r, MotionEvent.ACTION_DOWN, p.left - 12 * dp, y); touch(r, MotionEvent.ACTION_UP, p.left - 12 * dp, y)
        assertEquals("a tap 12 dp outside the q key types q", listOf("q"), typed)
        typed.clear()
        touch(r, MotionEvent.ACTION_DOWN, p.left - 80 * dp, y); touch(r, MotionEvent.ACTION_UP, p.left - 80 * dp, y)
        assertTrue("far from the keys it is still nobody's: $typed", typed.isEmpty())
    }
}
