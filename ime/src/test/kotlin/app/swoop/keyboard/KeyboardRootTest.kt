package app.swoop.keyboard

import android.app.Application
import android.graphics.Rect
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
        val modes = ArrayList<Boolean>(); var geometry = 0
        override fun modeChanged(floating: Boolean) { modes.add(floating) }
        override fun geometryChanged() { geometry++ }
    }

    private fun root(prefs: (android.content.SharedPreferences.Editor) -> Unit = {}): Pair<KeyboardRoot, Calls> {
        app.getSharedPreferences(Prefs.FILE, 0).edit().clear().also(prefs).commit()
        val r = KeyboardRoot(app); val c = Calls(); r.host = c
        r.applySettings(Settings(app))
        layout(r)
        return r to c
    }
    /** What the input method window does: the window is as tall as the screen while floating, as the keyboard while docked. */
    private fun layout(r: KeyboardRoot) {
        val floating = r.floating
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
}
