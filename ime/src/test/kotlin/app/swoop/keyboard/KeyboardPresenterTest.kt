package app.swoop.keyboard

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import app.kestrel.core.input.glide.EmojiMap
import app.kestrel.core.input.glide.LexiconFile
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
import java.io.File
import java.util.concurrent.Executor

/**
 * KB-103 end to end on framework code (API 29): real touches on the real keys view -> the presenter -> the real glide engine (a tiny word list, synchronous
 * threads) -> a real EditText through its InputConnection; and what that does to the learned-memory file. The one thing not covered is speed on the tablet.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class KeyboardPresenterTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val dp get() = app.resources.displayMetrics.density
    private val list = LexiconFile.build(listOf("for" to 900L, "fir" to 300L, "fur" to 100L, "hello" to 500L, "world" to 800L, "there" to 700L).map { LexiconFile.Entry(it.first, it.second) }, emptyList())

    private inner class Rig(inputType: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE, imeOptions: Int = 0, glidePref: Boolean = true, suggest: Boolean = false, autoFix: Boolean = false, words: ByteArray = list, val inline: Boolean = false, extra: (android.content.SharedPreferences.Editor) -> Unit = {}) {
        val act: Activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val et = EditText(act)
        val info = EditorInfo()
        val view = KeysView(app)
        val host = GlideHost(app, Executor { it.run() }, Executor { it.run() }) { words }
        val presenter: KeyboardPresenter
        val ic: android.view.inputmethod.InputConnection
        init {
            File(app.filesDir, "glide_personal.txt").delete(); File(app.filesDir, "glide_habit.txt").delete()
            File(app.filesDir, "own_words.txt").delete()
            app.getSharedPreferences(Prefs.FILE, 0).edit().clear().putBoolean(Prefs.GLIDE, glidePref).putBoolean(Prefs.SUGGEST, suggest).putBoolean(Prefs.AUTOFIX, autoFix).putBoolean(Prefs.LEARN, true).also(extra).commit()
            et.inputType = inputType; et.imeOptions = imeOptions
            act.setContentView(et)
            info.inputType = inputType; info.imeOptions = imeOptions
            val base = et.onCreateInputConnection(info)!!
            // Robolectric does not route key events from an InputConnection into the EditText: Backspace is done as the delete it stands for
            // [inline]: an editor in the keyboard's OWN process on Android 10 runs the calls inline, so its report of the new cursor position arrives INSIDE the edit call (before the call returns to the keyboard)
            ic = object : android.view.inputmethod.InputConnectionWrapper(base, true) {
                override fun sendKeyEvent(event: android.view.KeyEvent): Boolean {
                    if (event.keyCode == android.view.KeyEvent.KEYCODE_DEL && event.action == android.view.KeyEvent.ACTION_DOWN) return deleteSurroundingText(1, 0)
                    return true
                }
                override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean = super.commitText(text, newCursorPosition).also { inlineReport() }
                override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean = super.setComposingText(text, newCursorPosition).also { inlineReport() }
                override fun finishComposingText(): Boolean = super.finishComposingText().also { inlineReport() }
                override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean = super.deleteSurroundingText(beforeLength, afterLength).also { inlineReport() }
                override fun setSelection(start: Int, end: Int): Boolean = super.setSelection(start, end).also { inlineReport() }
                override fun endBatchEdit(): Boolean = super.endBatchEdit().also { inlineReport() }
            }
            presenter = KeyboardPresenter(app, { ic }, { host }, { it.run() })
            presenter.view = view
            presenter.startInput(info)
            relayout(); presenter.prepareGlide()
        }
        fun relayout() {
            view.measure(View.MeasureSpec.makeMeasureSpec((564 * dp).toInt(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        }
        fun report() { presenter.onUpdateSelection(et.selectionStart, et.selectionEnd, BaseInputConnection.getComposingSpanStart(et.text), BaseInputConnection.getComposingSpanEnd(et.text)) }
        private fun inlineReport() { if (inline) report() }
        fun ev(a: Int, x: Float, y: Float) { val t = SystemClock.uptimeMillis(); view.dispatchTouchEvent(MotionEvent.obtain(t, t, a, x, y, 0)); if (!inline) report() }
        fun tap(label: String) { val (x, y) = view.centerOf(label)!!; ev(MotionEvent.ACTION_DOWN, x, y); ev(MotionEvent.ACTION_UP, x, y) }
        /** The finger draws [word] over the keys (straight lines between the letters' centres, a point every few pixels) and lifts. */
        fun glide(word: String) {
            val pts = ArrayList<Pair<Float, Float>>()
            val cs = word.map { view.centerOf(it.toString()) ?: view.centerOf(it.uppercase().toString())!! }
            for (i in 0 until cs.size - 1) { val (x0, y0) = cs[i]; val (x1, y1) = cs[i + 1]; val n = Math.max(2, (Math.hypot((x1 - x0).toDouble(), (y1 - y0).toDouble()) / (4 * dp)).toInt()); for (k in 0 until n) pts.add((x0 + (x1 - x0) * k / n) to (y0 + (y1 - y0) * k / n)) }
            pts.add(cs.last())
            ev(MotionEvent.ACTION_DOWN, pts[0].first, pts[0].second)
            for (p in pts.drop(1).dropLast(1)) ev(MotionEvent.ACTION_MOVE, p.first, p.second)
            ev(MotionEvent.ACTION_UP, pts.last().first, pts.last().second)
        }
        val text: String get() = et.text.toString()
        fun memory(): String { presenter.finishInput(); val f = File(app.filesDir, "glide_personal.txt"); return if (f.isFile) f.readText() else "" }
    }

    @Test fun aGlideOverTheKeysTypesTheWordIntoTheField() {
        val r = Rig(); r.glide("hello")
        assertEquals("hello ", r.text); assertEquals("hello", r.view.candidatesForTest().first())
    }

    @Test fun twoGlidesAndOneBackspace() {
        val r = Rig(); r.glide("hello"); r.glide("world"); assertEquals("hello world ", r.text)
        r.tap("⌫"); assertEquals("hello ", r.text)
    }

    @Test fun shiftOnceCapitalisesTheGlidedWord() { val r = Rig(); r.tap("⇧"); r.glide("hello"); assertEquals("Hello ", r.text) }

    @Test fun aGuessFromTheStripReplacesTheWord() {
        val r = Rig(); r.glide("for")
        val cands = r.view.candidatesForTest()
        val alt = cands.drop(1).firstOrNull() ?: return                 // an unambiguous path: nothing to pick (the engine's choice is tested in :core)
        val (x, y) = r.view.chipCenterForTest(cands.indexOf(alt))!!
        r.ev(MotionEvent.ACTION_DOWN, x, y); r.ev(MotionEvent.ACTION_UP, x, y)
        assertEquals("$alt ", r.text)
    }

    @Test fun aGlideStartsAfterAFewDpOfMovementNotAtTheEdgeOfTheKey() {
        val r = Rig(); val (x, y) = r.view.centerOf("h")!!
        r.ev(MotionEvent.ACTION_DOWN, x, y); assertFalse(r.view.isGliding)
        r.ev(MotionEvent.ACTION_MOVE, x + 2 * dp, y); assertFalse("2 dp is a wobble, not a glide", r.view.isGliding)
        r.ev(MotionEvent.ACTION_MOVE, x + 6 * dp, y); assertTrue("6 dp of movement starts the glide at once (the key is 50 dp wide)", r.view.isGliding)
    }

    @Test fun aTapStillTypesLettersWithGlideOn() { val r = Rig(); r.tap("h"); r.tap("i"); assertEquals("hi", r.text) }

    @Test fun aGlideThatIsReallyATinyDragIsATap() {
        val r = Rig(); val (x, y) = r.view.centerOf("g")!!
        r.ev(MotionEvent.ACTION_DOWN, x, y); r.ev(MotionEvent.ACTION_MOVE, x + 10 * dp, y); r.ev(MotionEvent.ACTION_UP, x + 12 * dp, y)
        assertEquals("g", r.text)
    }

    @Test fun glideIsOffInAPasswordFieldAndNothingIsLearned() {
        val r = Rig(inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val h = r.view.height
        r.glide("hello"); assertTrue("no word may be guessed into a password field", !r.text.contains("hello"))
        r.tap("a"); assertTrue(r.text.endsWith("a"))
        assertEquals("", r.memory())
        assertTrue("no strip of guesses in a password field", r.view.candidatesForTest().isEmpty())
    }

    @Test fun theGlobeKeyAsksTheServiceForTheNextKeyboardAndTypesNothing() {
        val r = Rig(extra = { it.putBoolean(Prefs.GLOBE, true) }); var asked = 0; r.presenter.onNextKeyboard = { asked++ }
        r.tap("globe"); assertEquals(1, asked); assertEquals("", r.text)
    }

    @Test fun theGlobeKeyIsOffByDefaultButTheSafeLayoutAlwaysHasIt() {
        val r = Rig(); assertNull(r.view.centerOf("globe"))
        r.presenter.safeMode = true; r.presenter.startInput(r.info); r.relayout(); assertNotNull("the way out of a broken keyboard", r.view.centerOf("globe"))
    }

    @Test fun safeModeIsPlainKeysOnlyAndNeverTouchesTheEngine() {
        val r = Rig(); r.presenter.safeMode = true
        val exploding = KeyboardPresenter(app, { r.ic }, { throw AssertionError("safe mode must not even create the glide engine") }, { it.run() })
        exploding.safeMode = true; exploding.view = r.view; exploding.startInput(r.info)
        r.glide("hello"); assertTrue("no glide in safe mode: ${r.text}", r.text != "hello ")
        r.tap("a"); assertTrue(r.text.endsWith("a")); exploding.finishInput()
        assertTrue(r.view.candidatesForTest().isEmpty())
    }

    @Test fun glideCanBeSwitchedOffInSettings() {
        val r = Rig(glidePref = false); r.glide("hello"); assertFalse("a glide must not decode when the owner switched glide off", r.text == "hello ")
    }

    @Test fun aKeptWordIsSavedToTheMemoryFileAndAPrivateFieldNeverIs() {
        val r = Rig(); r.glide("hello"); r.tap("space")
        assertTrue("the kept word must be in the learned file: ${r.memory()}", r.memory().contains("hello"))
        val p = Rig(imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING); p.glide("world"); p.tap("space")
        assertEquals("a field that asked for no learning teaches the memory nothing", "", p.memory())
    }

    @Test fun theHabitFileIsWrittenWhenAGlideIsKept() {
        val r = Rig(); r.glide("hello"); r.tap("space")
        assertTrue(File(app.filesDir, "glide_habit.txt").isFile)
    }

    @Test fun aGlideAfterTextAddsTheSpaceAndTheContextWordIsUsed() {
        val r = Rig(); r.tap("h"); r.tap("i"); r.tap("space"); r.glide("there"); assertEquals("hi there ", r.text)
    }

    @Test fun slidingOverSpaceTypesNoSpaceKeepsTheGlideAndClearsTheStrip() {
        val r = Rig(); r.glide("hello"); assertTrue(r.view.candidatesForTest().isNotEmpty()); val (x, y) = r.view.centerOf("space")!!
        r.ev(MotionEvent.ACTION_DOWN, x, y); r.ev(MotionEvent.ACTION_MOVE, x - 12 * dp, y); r.ev(MotionEvent.ACTION_MOVE, x - 12 * dp - 33 * dp, y); r.ev(MotionEvent.ACTION_UP, x - 50 * dp, y)
        assertEquals("hello ", r.text)                      // (Robolectric does not deliver the arrow keys to the EditText: the cursor itself is checked in InputConnectionBridgeTest)
        assertTrue(r.view.candidatesForTest().isEmpty()); assertTrue(r.memory().contains("hello"))
    }

    // ------------------------------------------------------------------ N4 / N6: suggestions and learning while tapping

    private val tapList = LexiconFile.build(listOf("the" to 90_000_000L, "that" to 40_000_000L, "hello" to 9_000_000L, "world" to 6_000_000L, "work" to 9_500_000L, "help" to 7_000_000L, "hellp" to 10L, "from" to 20_000_000L)
        .map { LexiconFile.Entry(it.first, it.second * 200L) } + LexiconFile.Entry("zzfiller", 400_000_000_000L), emptyList())

    @Test fun tappingShowsCompletionsInTheStripEvenWithGlideTypingOff() {
        val r = Rig(glidePref = false, suggest = true, words = tapList); r.relayout()
        r.tap("w"); r.tap("o")
        assertTrue("the strip offers completions: ${r.view.candidatesForTest()}", r.view.candidatesForTest().containsAll(listOf("world", "work")))
        assertEquals("wo", r.text)
    }

    @Test fun tappingAWordInTheStripReplacesWhatWasTypedAndAddsASpace() {
        val r = Rig(glidePref = false, suggest = true, words = tapList)
        r.tap("h"); r.tap("e"); r.tap("l")
        assertTrue(r.view.candidatesForTest().contains("hello"))
        r.presenter.onCandidate("hello"); r.report()
        assertEquals("hello ", r.text)
        assertTrue("the strip moves on to what may follow", r.view.candidatesForTest().none { it == "hello" })
    }

    @Test fun theTypedWordInQuotesCanBeKept() {
        val r = Rig(glidePref = false, suggest = true, words = tapList)
        for (c in "kestrel") r.tap(c.toString())
        val lit = r.view.candidatesForTest().first()
        assertEquals("\u201Ckestrel\u201D", lit)
        r.presenter.onCandidate(lit); r.report()
        assertEquals("kestrel ", r.text)
    }

    @Test fun aSpaceFixesAnObviousTypoAndOneBackspaceTakesItBack() {
        val r = Rig(glidePref = false, suggest = true, autoFix = true, words = tapList)
        for (c in "teh") r.tap(c.toString())
        r.tap("space")
        assertEquals("the ", r.text)
        r.tap("⌫")
        assertEquals("one Backspace gives back what was typed", "teh", r.text)
        r.tap("space")
        assertEquals("and it is not fixed a second time", "teh ", r.text)
    }

    @Test fun withTheAutomaticFixOffATypoStaysAsTyped() {
        val r = Rig(glidePref = false, suggest = true, autoFix = false, words = tapList)
        for (c in "teh") r.tap(c.toString()); r.tap("space")
        assertEquals("teh ", r.text)
    }

    @Test fun aWordKnownToTheListIsNeverFixed() {
        val r = Rig(glidePref = false, suggest = true, autoFix = true, words = tapList)
        for (c in "hellp") r.tap(c.toString()); r.tap("space")
        assertEquals("hellp ", r.text)
    }

    @Test fun typedWordsAreLearnedIntoTheMemoryFile() {
        val r = Rig(glidePref = false, suggest = true, words = tapList)
        for (c in "hello") r.tap(c.toString()); r.tap("space"); for (c in "world") r.tap(c.toString()); r.tap("space")
        val m = r.memory()
        assertTrue("word learned: $m", m.lines().any { it.startsWith("w hello ") }); assertTrue(m.lines().any { it.startsWith("w world ") })
        assertTrue("the pair hello world", m.lines().any { it.startsWith("p hello world ") })
    }

    @Test fun nothingIsLearnedOrSuggestedInAPasswordField() {
        val r = Rig(inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, glidePref = false, suggest = true, words = tapList)
        for (c in "hello") r.tap(c.toString()); r.tap("space")
        assertTrue(r.view.candidatesForTest().isEmpty()); assertEquals("", r.memory())
    }

    @Test fun withSuggestionsOffTheStripStaysEmptyButTheKeyboardStillLearns() {
        val r = Rig(glidePref = false, suggest = false, words = tapList)
        for (c in "hello") r.tap(c.toString()); r.tap("space")
        assertTrue(r.view.candidatesForTest().isEmpty())
        assertTrue(r.memory().lines().any { it.startsWith("w hello ") })
    }

    @Test fun erasingAWordAndRetypingItTeachesTheCorrection() {
        val r = Rig(glidePref = false, suggest = false, words = tapList)
        for (c in "hellp") r.tap(c.toString()); r.tap("space")
        assertEquals("hellp ", r.text); r.tap("⌫"); assertEquals("hellp", r.text); r.tap("⌫"); assertEquals("hell", r.text); r.tap("o"); r.tap("space")
        val m = r.memory()
        assertEquals("hello ", r.text)
        assertTrue("the correction is remembered: $m", m.lines().any { it.startsWith("c hellp hello ") })
    }

    // ------------------------------------------------------------------ N7: options

    @Test fun theEmulatorSequenceHiBackspaceSymbolsQuestionMarkAKeepsTheSmartSpace() {      // what tools/emu/smoke.sh types on a real Android: it must behave the same on every Android version
        for (suggest in listOf(false, true)) {
            val r = Rig(glidePref = true, suggest = suggest)
            r.tap("h"); r.tap("i"); r.tap("\u232B"); r.tap("?123"); r.tap("?"); r.tap("a")
            assertEquals("suggest=$suggest", "h? A", r.text)      // (this plain test field has no capital-at-start flag; the real Android 10 emulator printed H?a here: see TASKS Q-SMARTSPACE)
        }
    }

    /**
     * Q-SMARTSPACE: on the Android 10 emulator the keyboard typed "H?a" where Android 16 typed "H? A". The field was the keyboard's OWN Setup field (same process): Android 10 runs such an editor's
     * calls inline, so the editor's report of the new cursor position arrived INSIDE commitText, before the keyboard had noted its own edit; it took the report for somebody else's change, re-read the
     * text and then counted its own edit a second time (the trace on the emulator showed the text window one character too long after every key).
     */
    @Test fun anEditorInTheKeyboardsOwnProcessReportsInsideTheEditCallAndNothingGoesWrong() {
        val r = Rig(inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE, glidePref = true, suggest = true, inline = true)
        r.tap("h"); r.tap("i"); r.tap("\u232B"); r.tap("?123"); r.tap("?"); r.tap("a")
        assertEquals("the space after ? and the capital, as on every other Android", "H? A", r.text)
        r.tap("."); r.tap("space"); r.tap("b")
        assertEquals("H? A. B", r.text)
        assertEquals("the keyboard's picture of the text is the text", r.text, r.presenter.controller.before)
        assertEquals(r.text.length, r.presenter.controller.selStart); assertEquals(r.text.length, r.presenter.controller.selEnd)
    }

    @Test fun aGlideAndAStripPickWorkInAnEditorThatReportsInsideTheEditCall() {
        val r = Rig(inline = true); r.glide("for")
        assertEquals("for ", r.text); assertEquals("for ", r.presenter.controller.before); assertNotNull("the glide can still be replaced from the strip", r.presenter.controller.lastGlide)
        val cands = r.view.candidatesForTest()
        val alt = cands.drop(1).firstOrNull() ?: return
        val (x, y) = r.view.chipCenterForTest(cands.indexOf(alt))!!
        r.ev(MotionEvent.ACTION_DOWN, x, y); r.ev(MotionEvent.ACTION_UP, x, y)
        assertEquals("$alt ", r.text); assertEquals("$alt ", r.presenter.controller.before)
    }

    @Test fun aLetterAfterPunctuationGetsItsSpaceAndTheSwitchTurnsItOff() {
        val r = Rig(glidePref = false); for (c in "hello") r.tap(c.toString()); r.tap("."); r.tap("w")
        assertEquals("the new sentence starts with a capital by default", "hello. W", r.text)
        val lower = Rig(glidePref = false, extra = { it.putBoolean(Prefs.AUTOCAPS, false) }); for (c in "hello") lower.tap(c.toString()); lower.tap("."); lower.tap("w")
        assertEquals("hello. w", lower.text)
        val off = Rig(glidePref = false, extra = { it.putBoolean(Prefs.AUTOSPACE, false) }); for (c in "hello") off.tap(c.toString()); off.tap("."); off.tap("w")
        assertEquals("hello.w", off.text)
    }

    @Test fun twoQuickSpacesMakeAFullStopAndTheSwitchTurnsItOff() {
        val r = Rig(glidePref = false); for (c in "hello") r.tap(c.toString()); r.tap("space"); r.tap("space")
        assertEquals("hello. ", r.text)
        val off = Rig(glidePref = false, extra = { it.putBoolean(Prefs.DOUBLESPACE, false) }); for (c in "hello") off.tap(c.toString()); off.tap("space"); off.tap("space")
        assertEquals("hello  ", off.text)
    }

    @Test fun aLoneIBecomesACapitalI() {
        val r = Rig(glidePref = false, suggest = true); r.tap("i"); r.tap("space"); for (c in "am") r.tap(c.toString())
        assertEquals("I am", r.text)
        val off = Rig(glidePref = false, suggest = true, extra = { it.putBoolean(Prefs.CAPI, false) }); off.tap("i"); off.tap("space")
        assertEquals("i ", off.text)
    }

    @Test fun theViewTakesItsLookFromTheSettings() {
        val r = Rig(extra = { it.putBoolean(Prefs.NUMBERROW, true).putInt(Prefs.KEYHEIGHT, 3).putInt(Prefs.THEME, 2).putInt(Prefs.ACCENT, 3).putBoolean(Prefs.HAPTIC, false).putBoolean(Prefs.PREVIEW, false).putBoolean(Prefs.SOUND, true).putBoolean(Prefs.TRAIL, false) })
        assertTrue(r.view.numberRow); assertEquals(1.3f, r.view.keyHeightScale, 0f); assertEquals(2, r.view.themeMode); assertEquals(Settings.ACCENTS[3].color, r.view.accent)
        assertFalse(r.view.hapticsOn); assertFalse(r.view.keyPreview); assertTrue(r.view.soundOn); assertFalse(r.view.trailOn)
    }

    /** Owner M1: "exactly like the photo", and the photo is the dark theme: a phone or tablet left in day mode still gets the dark keyboard until the person picks Automatic or Light. */
    @Test fun theKeyboardIsDarkUntilAnotherLookIsChosen() {
        val r = Rig()
        assertEquals(Settings.DEFAULT_THEME, Settings(app).theme); assertEquals("Dark", Settings.THEMES[Settings.DEFAULT_THEME])
        assertEquals(1, r.view.themeMode)
        assertEquals("the dark teal of the photo behind the keys, although this test runs in day mode", 0xFF0D1D1D.toInt(), r.view.backgroundColor)
    }

    @Test fun aTypedWordWithAnEmojiOffersItAndTappingItReplacesTheWord() {
        val r = Rig(glidePref = false, suggest = true, words = LexiconFile.build(listOf("clown" to 900L, "clowns" to 300L, "the" to 9000L).map { LexiconFile.Entry(it.first, it.second) }, emptyList()))
        r.presenter.canDraw = { true }; Thread.sleep(200); org.robolectric.shadows.ShadowLooper.idleMainLooper()
        for (c in "clown") r.tap(c.toString())
        val clown = "\uD83E\uDD21"
        assertTrue("the emoji is among the guesses: ${r.view.candidatesForTest()}", clown in r.view.candidatesForTest())
        val i = r.view.candidatesForTest().indexOf(clown); val (x, y) = r.view.chipCenterForTest(i)!!
        r.ev(MotionEvent.ACTION_DOWN, x, y); r.ev(MotionEvent.ACTION_UP, x, y)
        assertEquals("the written word is gone, the emoji is in", "$clown ", r.text)
        assertEquals("the emoji is not learned as a word", "", r.memory().lines().filter { it.contains(clown) }.joinToString())
    }

    @Test fun emojiSuggestionsCanBeSwitchedOff() {
        val r = Rig(glidePref = false, suggest = true, extra = { it.putBoolean(Prefs.EMOJI_SUGGEST, false) }, words = LexiconFile.build(listOf("clown" to 900L, "the" to 9000L).map { LexiconFile.Entry(it.first, it.second) }, emptyList()))
        Thread.sleep(50); org.robolectric.shadows.ShadowLooper.idleMainLooper()
        for (c in "clown") r.tap(c.toString())
        assertTrue(r.view.candidatesForTest().none { EmojiMap.isEmoji(it) })
    }

    @Test fun aWordRemovedWithALongPressIsNeverOfferedAgain() {
        val r = Rig(glidePref = true, suggest = true)
        r.presenter.onRemoveWord("hello", true)
        r.glide("hello")
        assertTrue("hello is not among the guesses: ${r.view.candidatesForTest()}", "hello" !in r.view.candidatesForTest())
        assertTrue(File(app.filesDir, "user_dictionary.txt").readText().contains("-hello"))
    }

    @Test fun pastingAClipboardItemPutsItInAtTheCursor() {
        val r = Rig(glidePref = false); for (c in "hi ") r.tap(if (c == ' ') "space" else c.toString())
        r.presenter.pasteText("a pasted line"); r.report()
        assertEquals("hi a pasted line", r.text)
    }

    @Test fun afterAFullStopAndASpaceTheNextLetterIsACapitalByDefault() {
        val r = Rig(glidePref = false); for (c in "hi") r.tap(c.toString()); r.tap("."); r.tap("space"); r.tap("A")
        assertEquals("hi. A", r.text)
    }
}
