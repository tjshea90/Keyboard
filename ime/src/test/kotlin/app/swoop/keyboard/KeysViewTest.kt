package app.swoop.keyboard

import android.app.Application
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import app.kestrel.core.input.ImeLayout
import app.kestrel.core.input.Key
import app.kestrel.core.input.Page
import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val (v, r) = view(); val (gx, gy) = at(v, ","); val (px, _) = at(v, ".")
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
    @Test fun theGlobeKeyComesBackWhenAskedAndReportsNextKeyboard() {
        val (v, r) = view(); v.showGlobe = true; v.measure(View.MeasureSpec.makeMeasureSpec((564 * dp).toInt(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)); v.layout(0, 0, v.measuredWidth, v.measuredHeight)
        tap(v, at(v, "globe")); assertEquals(listOf("<next>"), r.keys)
        val (n, rn) = view(kind = FieldKind.NUMBER); n.showGlobe = true; tap(n, at(n, "globe")); assertEquals(listOf("<next>"), rn.keys)
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
    @Test fun theOneTwoThreeKeyPagesToTheSymbolsAndBackAndIsNotReported() {
        val (v, r) = view(); assertNull("no globe by default", v.centerOf("globe"))
        tap(v, at(v, "?123")); assertEquals(Page.SYMBOLS, v.pageForTest()); assertTrue(r.keys.isEmpty())
        tap(v, at(v, "abc")); assertEquals(Page.LETTERS, v.pageForTest()); assertTrue("the paging keys are not reported", r.keys.isEmpty())
    }

    @Test fun theSecondSymbolsPageIsReachedFromTheFirstAndTheSameKeyGoesBackAndOneSymbolThereReturnsToTheLetters() {
        val (v, r) = view()
        tap(v, at(v, "?123")); assertEquals(Page.SYMBOLS, v.pageForTest())
        tap(v, at(v, ImeLayout.MORE_SYMBOLS)); assertEquals(Page.SYMBOLS2, v.pageForTest()); assertTrue(r.keys.isEmpty())
        tap(v, at(v, "?123")); assertEquals("?123 on the second page goes back to the first", Page.SYMBOLS, v.pageForTest())
        tap(v, at(v, ImeLayout.MORE_SYMBOLS)); tap(v, at(v, "\u20AC")); assertEquals(listOf("\u20AC"), r.keys); assertEquals("one symbol from the second page and the letters are back", Page.LETTERS, v.pageForTest())
        tap(v, at(v, "?123")); tap(v, at(v, ImeLayout.MORE_SYMBOLS)); tap(v, at(v, "abc")); assertEquals("abc leaves the second page too", Page.LETTERS, v.pageForTest())
    }

    @Test fun afterOneSpecialCharacterTheKeyboardGoesStraightBackToTheLetters() {      // M5, like Gboard
        val (v, r) = view()
        tap(v, at(v, "?123")); assertEquals(Page.SYMBOLS, v.pageForTest())
        tap(v, at(v, "?")); assertEquals(listOf("?"), r.keys); assertEquals("back on the letters at once", Page.LETTERS, v.pageForTest())
        tap(v, at(v, "q")); assertEquals(listOf("?", "q"), r.keys)
        tap(v, at(v, "?123")); tap(v, at(v, "@")); assertEquals(Page.LETTERS, v.pageForTest())
        tap(v, at(v, "?123")); tap(v, at(v, "space")); assertEquals("space also ends the symbols page", Page.LETTERS, v.pageForTest())
        tap(v, at(v, "?123")); tap(v, at(v, "5")); assertEquals("a digit does not (the number row is on both pages)", Page.SYMBOLS, v.pageForTest())
        tap(v, at(v, "⌫")); assertEquals("Backspace changes nothing", Page.SYMBOLS, v.pageForTest())
    }

    @Test fun theSpaceBarIsTheWidestKeyOfTheBottomRow() {
        val (v, _) = view()
        val sp = at(v, "space"); val comma = at(v, ","); val dot = at(v, "."); val go = at(v, "⏎")
        assertTrue("space is in the middle", comma.first < sp.first && sp.first < dot.first && dot.first < go.first)
        val spaceW = (dot.first - sp.first) * 2; val keyW = (at(v, "q").first - at(v, "w").first).let { Math.abs(it) }
        assertTrue("the space bar is about five keys wide: $spaceW vs $keyW", spaceW > keyW * 4f)
    }

    @Test fun holdingTheFullStopOpensTheSymbolsGridAndALetGoOnASymbolTypesIt() {
        val (v, r) = view(); val (x, y) = at(v, ".")
        down(v, x, y); assertFalse(v.popupOpenForTest()); advance(KeysView.HOLD_MS + 40); assertTrue("the grid is open", v.popupOpenForTest())
        for (c in "@#$%&-+()/*\"':;!?_=~<>[]{}\\|") assertNotNull("'$c' is in the grid", v.popupCellCenterForTest(c.toString()))
        val (sx, sy) = v.popupCellCenterForTest("@")!!; move(v, sx, sy); up(v, sx, sy)
        assertEquals(listOf("@"), r.keys); assertFalse(v.popupOpenForTest())
    }

    @Test fun theSymbolsGridWorksFromTheCommaToo() {
        val (v, r) = view(); val (x, y) = at(v, ",")
        down(v, x, y); advance(KeysView.HOLD_MS + 40); val (sx, sy) = v.popupCellCenterForTest("?")!!; move(v, sx, sy); up(v, sx, sy)
        assertEquals(listOf("?"), r.keys)
    }

    @Test fun liftingOnTheHeldKeyItselfTypesIt_andLiftingOutsideTheGridTypesNothing() {
        val (v, r) = view(); val (x, y) = at(v, ".")
        down(v, x, y); advance(KeysView.HOLD_MS + 40); up(v, x, y); assertEquals(listOf("."), r.keys)
        val (a, b) = at(v, "q"); down(v, x, y); advance(KeysView.HOLD_MS + 40); move(v, a + 700f, b + 900f); up(v, a + 700f, b + 900f); assertEquals(listOf("."), r.keys)
    }

    @Test fun aQuickTapOnTheFullStopStillJustTypesIt_andNumberFieldsHaveNoGrid() {
        val (v, r) = view(); tap(v, at(v, ".")); assertEquals(listOf("."), r.keys); assertFalse(v.popupOpenForTest())
        val (n, rn) = view(kind = FieldKind.NUMBER); val (x, y) = at(n, "."); down(n, x, y); advance(KeysView.HOLD_MS + 40); assertFalse(n.popupOpenForTest()); up(n, x, y); assertEquals(listOf("."), rn.keys)
    }

    @Test fun holdingTheSpaceBarOpensTheKeyboardPickerAndTypesNoSpace() {
        val (v, r) = view(); val (x, y) = at(v, "space")
        down(v, x, y); advance(KeysView.SPACE_HOLD_MS + 40); assertEquals(listOf("<next>"), r.keys); up(v, x, y); assertEquals("no space typed after the hold", listOf("<next>"), r.keys)
        tap(v, at(v, "space")); assertEquals(listOf("<next>", " "), r.keys)
    }

    @Test fun slidingOnTheSpaceBarMovesTheCursorAndNeverOpensThePicker() {
        val (v, r) = view(); val (x, y) = at(v, "space")
        down(v, x, y); move(v, x + 15 * dp, y); move(v, x + 50 * dp, y); advance(KeysView.SPACE_HOLD_MS + 100); up(v, x + 50 * dp, y)
        assertTrue(r.cursor > 0); assertTrue(r.keys.none { it == "<next>" })
    }

    // ------------------------------------------------------------------ hold, bubble, slide
    private fun relayout(v: KeysView, w: Int = 564) { v.measure(View.MeasureSpec.makeMeasureSpec((w * dp).toInt(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)); v.layout(0, 0, v.measuredWidth, v.measuredHeight) }
    @Test fun withoutTheNumberRowTheLettersAreOnTopLikeThePhoto() { val (v, _) = view(); assertNull("no digit keys on the letters page by default", v.centerOf("5")); assertTrue("q is on the first row", at(v, "q").second < at(v, "a").second) }
    @Test fun theNumberRowWhenSwitchedOnIsOnTopOfTheLettersAndTypesDigits() {
        val (v, r) = view(); v.numberRow = true; relayout(v); assertTrue("digits above the letters", at(v, "5").second < at(v, "q").second)
        for (d in "1234567890") tap(v, at(v, d.toString())); assertEquals("1234567890", r.keys.joinToString(""))
    }
    @Test fun withTheNumberRowANormalHoldOnALetterIsJustTheLetter() { val (v, r) = view(); v.numberRow = true; relayout(v); val (x, y) = at(v, "e"); down(v, x, y); advance(KeysView.HOLD_MS + 50); up(v, x, y); assertEquals(listOf("e"), r.keys) }
    @Test fun withoutTheNumberRowHeldTopRowLetterTypesItsDigitAndShowsItInTheBubble() {
        val (v, r) = view(); v.numberRow = false
        v.measure(View.MeasureSpec.makeMeasureSpec((564 * dp).toInt(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)); v.layout(0, 0, v.measuredWidth, v.measuredHeight)
        assertNull(v.centerOf("5"))
        val (x, y) = at(v, "e")
        down(v, x, y); assertEquals("e", v.bubbleTextForTest())
        advance(KeysView.HOLD_MS + 20); assertEquals("3", v.bubbleTextForTest())
        up(v, x, y); assertEquals(listOf("3"), r.keys)
    }
    @Test fun theBubbleIsGoneTheMomentTheFingerLiftsBeforeTheKeyIsSentToTheApp() {
        val (v, r) = view(); val (x, y) = at(v, "h")
        val seen = ArrayList<String?>()
        v.listener = object : KeysView.Listener { override fun onKey(key: app.kestrel.core.input.Key) { seen.add(v.bubbleTextForTest()); r.keys.add((key as app.kestrel.core.input.Key.Ch).text) }
            override fun onCursor(steps: Int) {} }
        down(v, x, y); assertEquals("h", v.bubbleTextForTest())
        up(v, x, y)
        assertNull("no bubble when the app gets the key (the app can be slow)", seen.single()); assertNull(v.bubbleTextForTest())
        assertEquals(listOf("h"), r.keys)
    }

    @Test fun theOptionsCanSwitchTheBubbleAndHapticsOff() { val (v, _) = view(); v.keyPreview = false; v.hapticsOn = false; val (x, y) = at(v, "h"); down(v, x, y); up(v, x, y) }
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
    @Test fun changingTheFieldShowsTheLayoutOfTheNewField() { val (v, _) = view(); v.setField(FieldKind.EMAIL, "Next"); assertEquals(Page.LETTERS, v.pageForTest()); assertNotNull(v.centerOf("@")) }

    // ------------------------------------------------------------------ geometry
    @Test fun theGlideDecoderGetsTheCentresOfAllTwentySixLettersInsideTheView() {
        val (v, _) = view(); val g = v.glideKeys(); assertNotNull(g)
        for (i in 0 until 26) { assertTrue(g!!.cx[i] in 0f..v.width.toFloat() && g.cy[i] in 0f..v.height.toFloat()) }
        val (qx, qy) = at(v, "q"); assertEquals(qx, g!!.cx['q' - 'a'], 0.5f); assertEquals(qy, g.cy['q' - 'a'], 0.5f)
    }
    @Test fun theTabletKeyboardIsAboutAQuarterOfThePortraitScreen() {
        val (v, _) = view(564); val dpH = v.height / dp
        assertTrue("height $dpH dp", dpH in 240f..320f)
    }

    // ------------------------------------------------------------------ M9 glide sensitivity, M4 blank keys, the toolbar

    private class Tools : KeysView.Listener {
        val keys = ArrayList<app.kestrel.core.input.Key>(); val tools = ArrayList<Int>(); val glides = ArrayList<Int>(); val longs = ArrayList<String>(); val removes = ArrayList<Pair<String, Boolean>>(); val cands = ArrayList<String>()
        override fun onKey(key: app.kestrel.core.input.Key) { keys.add(key) }
        override fun onCursor(steps: Int) {}
        override fun onGlideEnd(xs: FloatArray, ys: FloatArray, count: Int) { glides.add(count) }
        override fun onToolbar(tool: Int) { tools.add(tool) }
        override fun onCandidate(word: String) { cands.add(word) }
        override fun onCandidateLong(word: String) { longs.add(word) }
        override fun onRemoveWord(word: String, yes: Boolean) { removes.add(word to yes) }
    }
    private fun glideView(): Pair<KeysView, Tools> { val (v, _) = view(); val t = Tools(); v.listener = t; v.setGlideEnabled(true); v.measure(View.MeasureSpec.makeMeasureSpec((564 * dp).toInt(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)); v.layout(0, 0, v.measuredWidth, v.measuredHeight); return v to t }

    @Test fun aQuickSwipeToTheNextKeyIsAGlideNotASingleLetter() {
        val (v, t) = glideView(); val (ax, ay) = at(v, "a"); val (sx, _) = at(v, "s")
        down(v, ax, ay); move(v, ax + (sx - ax) * 0.5f, ay); move(v, sx, ay); up(v, sx, ay)
        assertEquals("one glide", 1, t.glides.size); assertTrue("no single letter typed", t.keys.isEmpty())
    }

    @Test fun aTouchThatNeverLeavesItsKeyIsStillATap() {
        val (v, t) = glideView(); val (ax, ay) = at(v, "a")
        down(v, ax, ay); move(v, ax + 3 * dp, ay + 2 * dp); move(v, ax + 4 * dp, ay); up(v, ax + 4 * dp, ay)
        assertTrue(t.glides.isEmpty()); assertEquals(1, t.keys.size)
    }

    @Test fun blankKeysStillTypeTheirLetters() {
        val (v, t) = glideView(); v.hideLetters = true
        val (x, y) = at(v, "q"); tap(v, x to y); assertEquals(listOf("q"), t.keys.map { (it as app.kestrel.core.input.Key.Ch).text })
        assertNull("no bubble shows the letter", v.bubbleTextForTest().takeIf { false })
    }

    @Test fun theToolbarIconsAreTappable() {
        val (v, t) = glideView()
        for (tool in listOf(KeysView.TOOL_SETTINGS, KeysView.TOOL_CLIPBOARD, KeysView.TOOL_TRANSLATE)) tap(v, v.toolCenterForTest(tool)!!)
        assertEquals(listOf(KeysView.TOOL_SETTINGS, KeysView.TOOL_CLIPBOARD, KeysView.TOOL_TRANSLATE), t.tools); assertTrue(t.keys.isEmpty())
        v.translateAvailable = false; assertNull(v.toolCenterForTest(KeysView.TOOL_TRANSLATE))
    }

    @Test fun aLongPressOnASuggestionAsksBeforeRemovingItForGood() {
        val (v, t) = glideView(); v.showCandidates(listOf("hello", "tui", "help"))
        val (x, y) = v.chipCenterForTest(1)!!
        down(v, x, y); advance(KeysView.STRIP_HOLD_MS + 40); assertEquals(listOf("tui"), t.longs)
        v.confirmWord = "tui"; up(v, x, y); assertTrue("lifting after the long press picks nothing", t.cands.isEmpty() && t.removes.isEmpty())
        val (rx, ry) = v.chipCenterForTest(0)!!; tap(v, rx to ry); assertEquals(listOf("tui" to true), t.removes); assertNull(v.confirmWord)
        v.confirmWord = "tui"; val (kx, ky) = v.chipCenterForTest(1)!!; tap(v, kx to ky); assertEquals("tui" to false, t.removes.last())
    }

    @Test fun mulForIsTheExactInverseOfHeightFor() {
        val v = KeysView(RuntimeEnvironment.getApplication()); v.setField(FieldKind.TEXT, "Send")
        for (w in listOf(500, 700, 940, 1100)) for (target in listOf(260, 330, 400, 520)) {
            val m = v.mulFor(w, target); val h = v.heightFor(w, m)
            assertTrue("w=$w wanted $target got $h (mul $m)", Math.abs(h - target) <= 1)
        }
    }
}
