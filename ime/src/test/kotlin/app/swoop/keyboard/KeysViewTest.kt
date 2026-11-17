package app.swoop.keyboard

import android.app.Application
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import app.kestrel.core.input.Key
import app.kestrel.core.input.Page
import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** KB-102: how the keys view reads fingers. Real MotionEvents on the tablet's API level (29); no pixels are compared here (KB-102 look test is separate). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class KeysViewTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val dp get() = app.resources.displayMetrics.density

    private class Rec : KeysView.Listener {
        val keys = ArrayList<String>(); var cursor = 0
        override fun onKey(key: Key) { keys.add(when (key) { is Key.Ch -> key.text; Key.Backspace -> "<bs>"; Key.Go -> "<go>"; Key.Shift -> "<shift>"; Key.NextKeyboard -> "<next>"; else -> key.toString() }) }
        override fun onCursor(steps: Int) { cursor += steps }
    }

    private fun view(w: Int = 564, kind: FieldKind = FieldKind.TEXT, action: String = "⏎"): Pair<KeysView, Rec> {
        val v = KeysView(app); val r = Rec(); v.listener = r
        val px = (w * dp).toInt()
        v.setField(kind, action)
        v.measure(View.MeasureSpec.makeMeasureSpec(px, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        v.layout(0, 0, v.measuredWidth, v.measuredHeight)
        return v to r
    }

    private fun ev(action: Int, x: Float, y: Float, t: Long = SystemClock.uptimeMillis()) = MotionEvent.obtain(t, t, action, x, y, 0)
    private fun down(v: View, x: Float, y: Float) { v.dispatchTouchEvent(ev(MotionEvent.ACTION_DOWN, x, y)) }
    private fun move(v: View, x: Float, y: Float) { v.dispatchTouchEvent(ev(MotionEvent.ACTION_MOVE, x, y)) }
    private fun up(v: View, x: Float, y: Float) { v.dispatchTouchEvent(ev(MotionEvent.ACTION_UP, x, y)) }
    private fun tap(v: View, p: Pair<Float, Float>) { down(v, p.first, p.second); up(v, p.first, p.second) }
    private fun at(v: KeysView, label: String) = v.centerOf(label) ?: throw AssertionError("no key '$label'")
    private fun advance(ms: Long) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms)) }

    private fun two(v: View, action: Int, idx: Int, x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
        val props = Array(2) { MotionEvent.PointerProperties().apply { id = it; toolType = MotionEvent.TOOL_TYPE_FINGER } }
        val coords = arrayOf(MotionEvent.PointerCoords().apply { x = x0; y = y0 }, MotionEvent.PointerCoords().apply { x = x1; y = y1 })
        val t = SystemClock.uptimeMillis()
        return v.dispatchTouchEvent(MotionEvent.obtain(t, t, action or (idx shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, props, coords, 0, 0, 1f, 1f, 0, 0, 0, 0))
    }

    // ------------------------------------------------------------------ taps
    @Test fun aTapTypesTheKeyAndEveryLetterKeyCanBeHit() {
        val (v, r) = view()
        for (c in "qwertyuiopasdfghjklzxcvbnm") tap(v, at(v, c.toString()))
        assertEquals("qwertyuiopasdfghjklzxcvbnm", r.keys.joinToString(""))
    }
    @Test fun aTouchBetweenTwoKeysTypesTheNearerOne() {
        val (v, r) = view(); val (qx, qy) = at(v, "q"); val (wx, _) = at(v, "w")
        tap(v, (qx + (wx - qx) * 0.4f) to qy); tap(v, (qx + (wx - qx) * 0.6f) to qy)
        assertEquals(listOf("q", "w"), r.keys)
    }
    @Test fun aTouchInTheGapBetweenRowsTypesTheNearerRowsKey() {
        val (v, r) = view(); val (ax, ay) = at(v, "a"); val (zx, zy) = at(v, "z")
        tap(v, ax to (ay + zy) / 2f - 1f)                    // in the gap between rows 2 and 3, a little nearer row 2
        assertEquals(listOf("a"), r.keys)
    }
    @Test fun aFarTouchAboveTheKeysTypesNothing() { val (v, r) = view(); tap(v, 100f to -60f); assertTrue(r.keys.isEmpty()) }
    @Test fun aThumbThatDriftsOffALetterStillTypesTheLetterItWentDownOn() {
        val (v, r) = view(); val (ax, ay) = at(v, "a"); val (sx, _) = at(v, "s")
        down(v, ax, ay); move(v, sx, ay); up(v, sx, ay)
        assertEquals(listOf("a"), r.keys)
    }
    @Test fun aNonLetterKeyTypesTheKeyUnderTheFingerAtLiftSoASlipCanBeCorrectedBySliding() {
        val (v, r) = view(); val (gx, gy) = at(v, "⏎"); val (px, _) = at(v, ".")
        down(v, gx, gy); move(v, px, gy); up(v, px, gy)
        assertEquals(listOf("."), r.keys)
    }
    @Test fun aFingerThatSlidesOffTheKeyboardTypesNothing() {
        val (v, r) = view(); val (gx, gy) = at(v, "⏎")
        down(v, gx, gy); move(v, gx, -80f); up(v, gx, -80f)
        assertTrue(r.keys.isEmpty())
    }
    @Test fun aCancelledTouchTypesNothing() { val (v, r) = view(); val p = at(v, "q"); down(v, p.first, p.second); v.dispatchTouchEvent(ev(MotionEvent.ACTION_CANCEL, p.first, p.second)); assertTrue(r.keys.isEmpty()) }

    // ------------------------------------------------------------------ special keys
    @Test fun spaceTypesASpaceOnATap() { val (v, r) = view(); tap(v, at(v, "space")); assertEquals(listOf(" "), r.keys) }
    @Test fun backspaceFiresAtOnceRepeatsWhileHeldAndStopsWhenLifted() {
        val (v, r) = view(); val (x, y) = at(v, "⌫")
        down(v, x, y); assertEquals(listOf("<bs>"), r.keys)
        advance(KeysView.REPEAT_FIRST_MS - 50); assertEquals(1, r.keys.size)
        advance(100); assertEquals(2, r.keys.size)
        advance(KeysView.REPEAT_MS * 3); assertEquals(5, r.keys.size)
        up(v, x, y); val n = r.keys.size; advance(500); assertEquals("repeat must stop at lift, and the lift itself types nothing", n, r.keys.size)
    }
    @Test fun theGlobeKeyIsOnEveryPageAndReportsNextKeyboard() {
        val (v, r) = view(); tap(v, at(v, "globe")); assertEquals(listOf("<next>"), r.keys)
        tap(v, at(v, "123")); tap(v, at(v, "globe")); assertEquals(listOf("<next>", "<next>"), r.keys)
        val (n, rn) = view(kind = FieldKind.NUMBER); tap(n, at(n, "globe")); assertEquals(listOf("<next>"), rn.keys)
    }
    @Test fun theActionKeyReportsGoAndShowsItsLabel() {
        val (v, r) = view(action = "Search"); tap(v, at(v, "Search")); assertEquals(listOf("<go>"), r.keys)
    }
    @Test fun shiftIsReportedAndTheViewShowsCapitalsOnlyWhenTheControllerSaysSo() {
        val (v, r) = view(); assertNotNull(v.centerOf("q")); assertNull(v.centerOf("Q"))
        tap(v, at(v, "⇧")); assertEquals(listOf("<shift>"), r.keys)
        v.setShift(ShiftState.ONCE); assertNotNull(v.centerOf("Q")); assertNull(v.centerOf("q"))
        tap(v, at(v, "Q")); assertEquals("Q", r.keys.last())
        v.setShift(ShiftState.OFF); assertNotNull(v.centerOf("q"))
    }
    @Test fun theSymbolsKeyPagesInsideTheViewAndIsNotReported() {
        val (v, r) = view(); tap(v, at(v, "123")); assertEquals(Page.SYMBOLS, v.pageForTest()); assertTrue(r.keys.isEmpty())
        tap(v, at(v, "7")); tap(v, at(v, "abc")); assertEquals(Page.LETTERS, v.pageForTest()); assertEquals(listOf("7"), r.keys)
    }

    // ------------------------------------------------------------------ hold, bubble, slide
    @Test fun heldTopRowLetterTypesItsDigitAndShowsItInTheBubble() {
        val (v, r) = view(); val (x, y) = at(v, "e")
        down(v, x, y); assertEquals("e", v.bubbleTextForTest())
        advance(KeysView.HOLD_MS + 20); assertEquals("3", v.bubbleTextForTest())
        up(v, x, y); assertEquals(listOf("3"), r.keys)
    }
    @Test fun aQuickTapOnATopRowLetterIsTheLetter() { val (v, r) = view(); tap(v, at(v, "e")); advance(600); assertEquals(listOf("e"), r.keys) }
    @Test fun holdingAMiddleRowLetterDoesNothingSpecial() { val (v, r) = view(); val (x, y) = at(v, "g"); down(v, x, y); advance(900); up(v, x, y); assertEquals(listOf("g"), r.keys) }
    @Test fun slidingOverSpaceMovesTheCursorAndTypesNoSpace() {
        val (v, r) = view(); val (x, y) = at(v, "space")
        down(v, x, y); move(v, x + 5 * dp, y); assertEquals(0, r.cursor)           // under the start distance: still a press
        move(v, x + 11f * dp, y); assertEquals("crossing the start distance only starts the slide", 0, r.cursor)
        move(v, x + 11f * dp + 3 * 11 * dp, y); assertEquals(3, r.cursor)
        move(v, x - 40 * dp, y); assertTrue(r.cursor < 0)
        up(v, x - 40 * dp, y); assertTrue("a cursor slide types no space", r.keys.isEmpty())
    }
    @Test fun aSpaceThatWiggledALittleIsStillASpace() { val (v, r) = view(); val (x, y) = at(v, "space"); down(v, x, y); move(v, x + 6 * dp, y); up(v, x + 6 * dp, y); assertEquals(listOf(" "), r.keys); assertEquals(0, r.cursor) }

    // ------------------------------------------------------------------ two thumbs
    @Test fun aSecondFingerTypesTheFirstLettersAtOnceAndThenItsOwn() {
        val (v, r) = view(); val (ax, ay) = at(v, "a"); val (sx, sy) = at(v, "s")
        down(v, ax, ay)
        two(v, MotionEvent.ACTION_POINTER_DOWN, 1, ax, ay, sx, sy); assertEquals("the first letter is typed when the second finger lands", listOf("a"), r.keys)
        two(v, MotionEvent.ACTION_POINTER_UP, 1, ax, ay, sx, sy); assertEquals(listOf("a", "s"), r.keys)
        up(v, ax, ay); assertEquals("the first finger's letter must not be typed twice", listOf("a", "s"), r.keys)
    }

    // ------------------------------------------------------------------ fields
    @Test fun aNumberFieldGetsAKeypadWithNoLetters() {
        val (v, r) = view(kind = FieldKind.NUMBER); assertNull(v.centerOf("q")); tap(v, at(v, "5")); assertEquals(listOf("5"), r.keys)
        assertNull("no digit hold on a keypad", v.bubbleTextForTest().let { if (it == null) null else if (it == "5") null else it })
    }
    @Test fun aUrlFieldHasSlashAndDotComKeys() { val (v, r) = view(kind = FieldKind.URL); tap(v, at(v, "/")); tap(v, at(v, ".com")); assertEquals(listOf("/", ".com"), r.keys) }
    @Test fun changingTheFieldReturnsToTheLettersPage() { val (v, _) = view(); tap(v, at(v, "123")); v.setField(FieldKind.EMAIL, "Next"); assertEquals(Page.LETTERS, v.pageForTest()); assertNotNull(v.centerOf("@")) }

    // ------------------------------------------------------------------ geometry
    @Test fun theGlideDecoderGetsTheCentresOfAllTwentySixLettersInsideTheView() {
        val (v, _) = view(); val g = v.glideKeys(); assertNotNull(g)
        for (i in 0 until 26) { assertTrue(g!!.cx[i] in 0f..v.width.toFloat() && g.cy[i] in 0f..v.height.toFloat()) }
        val (qx, qy) = at(v, "q"); assertEquals(qx, g!!.cx['q' - 'a'], 0.5f); assertEquals(qy, g.cy['q' - 'a'], 0.5f)
    }
    @Test fun theTabletKeyboardIsAboutAQuarterOfThePortraitScreen() {
        val (v, _) = view(564); val dpH = v.height / dp
        assertTrue("height $dpH dp", dpH in 200f..260f)
    }
}
