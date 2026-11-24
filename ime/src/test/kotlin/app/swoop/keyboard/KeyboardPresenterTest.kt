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
import app.kestrel.core.input.Key
import app.kestrel.core.input.glide.EmojiMap
import app.kestrel.core.input.glide.LexiconFile
import app.kestrel.core.input.glide.GlideSession
import app.kestrel.core.input.glide.SpellingNear
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

    private inner class Rig(inputType: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE, imeOptions: Int = 0, glidePref: Boolean = true, suggest: Boolean = false, autoFix: Boolean = false, words: ByteArray = list, val sameProcess: Boolean = false, extra: (android.content.SharedPreferences.Editor) -> Unit = {}) {
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
            app.getSharedPreferences(Prefs.FILE, 0).edit().clear().putBoolean(Prefs.GLIDE, glidePref).putBoolean(Prefs.SUGGEST, suggest).putBoolean(Prefs.AUTOFIX, autoFix).putBoolean(Prefs.LEARN, true).putBoolean(Prefs.AUTOCAPS, false).also(extra).commit()   // (capitals are off unless a test asks: an empty box now starts with one, R18b)
            et.inputType = inputType; et.imeOptions = imeOptions
            act.setContentView(et)
            info.inputType = inputType; info.imeOptions = imeOptions
            val base = et.onCreateInputConnection(info)!!
            // Robolectric does not route key events from an InputConnection into the EditText: Backspace is done as the delete it stands for
            // [sameProcess]: an editor in the keyboard's OWN process on Android 10 runs the calls inline, so its report of the new cursor position arrives INSIDE the edit call (before the call returns to the keyboard)
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
        /** Like InputMethodManager.updateSelection: a report only when the selection or the composing region differs from the last one sent. */
        private var lastSent = intArrayOf(-2, -2, -2, -2)
        private fun inlineReport() {
            if (!sameProcess) return
            val now = intArrayOf(et.selectionStart, et.selectionEnd, BaseInputConnection.getComposingSpanStart(et.text), BaseInputConnection.getComposingSpanEnd(et.text))
            if (now.contentEquals(lastSent)) return
            lastSent = now; report()
        }
        fun ev(a: Int, x: Float, y: Float) { val t = SystemClock.uptimeMillis(); view.dispatchTouchEvent(MotionEvent.obtain(t, t, a, x, y, 0)); if (!sameProcess) report() }
        fun tap(label: String) { val (x, y) = view.centerOf(label)!!; ev(MotionEvent.ACTION_DOWN, x, y); ev(MotionEvent.ACTION_UP, x, y) }
        /** A letter key whatever the shift state shows on it (capitals while Shift is on). */
        fun tapLetter(c: String) { tap(if (view.centerOf(c) != null) c else c.uppercase()) }
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
            val r = Rig(glidePref = true, suggest = suggest, extra = { it.putBoolean(Prefs.AUTOCAPS, true) })
            r.tapLetter("h"); r.tapLetter("i"); r.tap("\u232B"); r.tap("?123"); r.tap("?"); r.tapLetter("a")
            assertEquals("suggest=$suggest", "H? A", r.text)      // (the box starts with a capital, R18b; the real Android 10 emulator printed H?a here: see TASKS Q-SMARTSPACE)
        }
    }

    /**
     * Q-SMARTSPACE: on the Android 10 emulator the keyboard typed "H?a" where Android 16 typed "H? A". The field was the keyboard's OWN Setup field (same process): Android 10 runs such an editor's
     * calls inline, so the editor's report of the new cursor position arrived INSIDE commitText, before the keyboard had noted its own edit; it took the report for somebody else's change, re-read the
     * text and then counted its own edit a second time (the trace on the emulator showed the text window one character too long after every key).
     */
    @Test fun anEditorInTheKeyboardsOwnProcessReportsInsideTheEditCallAndNothingGoesWrong() {
        val r = Rig(inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE, glidePref = true, suggest = true, sameProcess = true, extra = { it.putBoolean(Prefs.AUTOCAPS, true) })
        r.tapLetter("h"); r.tapLetter("i"); r.tap("\u232B"); r.tap("?123"); r.tap("?"); r.tapLetter("a")
        assertEquals("the space after ? and the capital, as on every other Android", "H? A", r.text)
        r.tap("."); r.tap("space"); r.tapLetter("b")
        assertEquals("H? A. B", r.text)
        assertEquals("the keyboard's picture of the text is the text", r.text, r.presenter.controller.before)
        assertEquals(r.text.length, r.presenter.controller.selStart); assertEquals(r.text.length, r.presenter.controller.selEnd)
    }

    @Test fun aGlideAndAStripPickWorkInAnEditorThatReportsInsideTheEditCall() {
        val r = Rig(sameProcess = true); r.glide("for")
        assertEquals("for ", r.text); assertEquals("for ", r.presenter.controller.before); assertNotNull("the glide can still be replaced from the strip", r.presenter.controller.lastGlide)
        val cands = r.view.candidatesForTest()
        val alt = cands.drop(1).firstOrNull() ?: return
        val (x, y) = r.view.chipCenterForTest(cands.indexOf(alt))!!
        r.ev(MotionEvent.ACTION_DOWN, x, y); r.ev(MotionEvent.ACTION_UP, x, y)
        assertEquals("$alt ", r.text); assertEquals("$alt ", r.presenter.controller.before)
    }

    @Test fun aLetterAfterPunctuationGetsItsSpaceAndTheSwitchTurnsItOff() {
        val r = Rig(glidePref = false, extra = { it.putBoolean(Prefs.AUTOCAPS, true) }); for (c in "hello") r.tapLetter(c.toString()); r.tap("."); r.tapLetter("w")
        assertEquals("the box and the new sentence start with a capital by default", "Hello. W", r.text)
        val lower = Rig(glidePref = false, extra = { it.putBoolean(Prefs.AUTOCAPS, false) }); for (c in "hello") lower.tap(c.toString()); lower.tap("."); lower.tap("w")
        assertEquals("hello. w", lower.text)
        val off = Rig(glidePref = false, extra = { it.putBoolean(Prefs.AUTOSPACE, false) }); for (c in "hello") off.tap(c.toString()); off.tap("."); off.tap("w")
        assertEquals("hello.w", off.text)
    }

    // ------------------------------------------------------------------ R19 (owner 2026-11-18): capitals after a full stop, a space deleted on purpose, a glide that always answers

    @Test fun aGlideRightAfterAFullStopGetsItsSpaceAndACapitalLikeATappedLetter() {
        val r = Rig(extra = { it.putBoolean(Prefs.AUTOCAPS, true) })
        for (c in "hello") r.tapLetter(c.toString()); r.tap("."); r.glide("world")
        assertEquals("Hello. World ", r.text)
        val q = Rig(extra = { it.putBoolean(Prefs.AUTOCAPS, true) }); for (c in "hello") q.tapLetter(c.toString()); q.tap(","); q.glide("world")
        assertEquals("after a comma the word stays small", "Hello, world ", q.text)
    }

    @Test fun aSpaceDeletedOnPurposeAfterAFullStopIsNotAddedAgainSoAnAddressCanBeTyped() {
        val r = Rig(glidePref = false, extra = { it.putBoolean(Prefs.AUTOCAPS, true) })
        for (c in "ok") r.tapLetter(c.toString()); r.tap("."); r.tapLetter("c")
        assertEquals("the keyboard adds the space and the capital", "Ok. C", r.text)
        r.tap("\u232B"); r.tap("\u232B")
        assertEquals("Ok.", r.text)
        r.tap("c"); r.tap("o"); r.tap("m")
        assertEquals("the deleted space stays deleted", "Ok.com", r.text)
        r.tap("space"); r.tapLetter("n"); assertEquals("Ok.com n", r.text)
    }

    @Test fun aGlideThatFitsNothingStillTypesTheClosestWordInsteadOfSayingNoWordFits() {
        val r = Rig()
        r.glide("zebra")                                                                    // none of the six words in the tiny list is anything like it
        val typed = r.text.trim()
        assertTrue("a word went in: '${r.text}'", typed.isNotEmpty() && r.text.endsWith(" ") && typed in listOf("for", "fir", "fur", "hello", "world", "there"))
        assertTrue("the strip offers it (and the others)", r.view.candidatesForTest().isNotEmpty())
        r.tap("\u232B"); assertEquals("Backspace takes the wrong guess back whole", "", r.text)
    }

    // ------------------------------------------------------------------ R20c (owner 2026-11-18): the pronoun I, whatever the box and however the word arrives

    @Test fun aLoneITypedInABoxThatAsksForNoSuggestionsBecomesACapitalI() {
        val r = Rig(inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_FLAG_MULTI_LINE, glidePref = false)
        for (c in "so") r.tap(c.toString()); r.tap("space"); r.tap("i"); r.tap("space"); r.tap("i"); r.presenter.onKey(Key.Ch("'")); r.report(); r.tap("m"); r.tap("space")
        assertEquals("so I I'm ", r.text)
    }

    @Test fun aGlidedIMGetsItsCapitalAndTheStripShowsItWithOne() {
        val words = LexiconFile.build(listOf("i'm" to 900L, "in" to 800L, "it" to 700L, "hello" to 500L).map { LexiconFile.Entry(it.first, it.second) }, emptyList())
        val r = Rig(words = words); r.glide("hello"); r.glide("im")
        assertTrue("the glide decoded i'm (or a neighbour): '${r.text}'", r.text.startsWith("hello "))
        val typed = r.text.removePrefix("hello ").trim()
        if (typed.lowercase() == "i'm") assertEquals("I'm", typed)
        val chips = r.view.candidatesForTest(); assertTrue("no lowercase pronoun on the strip: $chips", chips.none { it == "i'm" || it == "i" })
    }

    // ------------------------------------------------------------------ R20b (owner 2026-11-18): after a glide the strip is [other guess] [NEXT word] [other guess]

    private val pairedList = LexiconFile.build(
        listOf("for" to 900L, "fir" to 300L, "fur" to 100L, "hello" to 500L, "world" to 800L, "there" to 700L, "you" to 600L).map { LexiconFile.Entry(it.first, it.second) },
        listOf(LexiconFile.Pair("hello", "world", 400L), LexiconFile.Pair("hello", "there", 300L), LexiconFile.Pair("hello", "you", 200L), LexiconFile.Pair("for", "world", 300L), LexiconFile.Pair("for", "there", 200L)),
    )
    private fun Rig.tapChip(i: Int) { val (x, y) = view.chipCenterForTest(i)!!; ev(MotionEvent.ACTION_DOWN, x, y); ev(MotionEvent.ACTION_UP, x, y) }

    @Test fun afterAGlideTheMiddleBoxIsTheNextWordAndTheGlidedWordIsNotOfferedAgain() {
        val g = Rig(suggest = true, words = pairedList); g.glide("hello")
        val c = g.view.candidatesForTest()
        assertEquals("three boxes: $c", 3, c.size); assertEquals("the predicted next word is in the middle: $c", "world", c[1]); assertTrue("the glided word is not offered again: $c", c.none { it == "hello" })
    }

    @Test fun theSidesAreTheOtherGuessesOfTheGlideAndTappingOneReplacesTheGlidedWord() {
        val r = Rig(suggest = true, words = pairedList); r.glide("for")
        val c = r.view.candidatesForTest()
        // R24b: with the straight-line price tuned on real swipes "fur" is a second look-alike guess, so both sides hold other guesses of the glide
        assertEquals("fir and fur are the other guesses of the glide, on both sides of the next word: $c", listOf("fir", "world", "fur"), c)
        r.tapChip(0)
        assertEquals("fir ", r.text)
        assertTrue("and the strip follows the new word: ${r.view.candidatesForTest()}", r.view.candidatesForTest().contains("for"))
    }

    @Test fun tappingTheMiddleBoxPutsTheNextWordInAfterTheGlidedWord() {
        val r = Rig(suggest = true, words = pairedList); r.glide("hello"); r.tapChip(1)
        assertEquals("hello world ", r.text)
        assertNull("the glide is confirmed, nothing is left to replace", r.presenter.controller.lastGlide)
    }

    @Test fun aSideBoxNeverHoldsAFillerNextWord() {
        // R32d: a side box that has no other guess of the glide stays empty; it used to be filled with another NEXT word, and a tap on it added the word instead of replacing the glided one
        val r = Rig(suggest = true, words = pairedList); r.glide("hello")
        val c = r.view.candidatesForTest()
        assertEquals("only the middle box may hold a next word: $c", "world", c[1])
        assertTrue("no filler next word in a side box: $c", c[0] !in listOf("world", "there", "you") && c[2] !in listOf("world", "there", "you"))
    }

    @Test fun withNoPredictionTheStripIsTheOldListOfGuesses() {
        val r = Rig(suggest = true, words = pairedList); r.glide("fir")
        assertEquals("fir", r.view.candidatesForTest().first())
    }

    @Test fun theSettingTurnsTheMiddleBoxBackIntoAGlideGuess() {
        val r = Rig(suggest = true, words = pairedList, extra = { it.putBoolean(Prefs.GLIDE_NEXT, false) }); r.glide("for")
        assertEquals("for", r.view.candidatesForTest().first())
    }

    @Test fun withSuggestionsOffTheStripAfterAGlideIsTheOldList() {
        val r = Rig(suggest = false, words = pairedList); r.glide("hello")
        assertEquals("hello", r.view.candidatesForTest().first())
    }

    // ------------------------------------------------------------------ R32d (owner 2026-11-24): a tap on the word he meant REPLACES the glided word, never adds it behind

    private val lookalikes = listOf("for" to 900L, "fir" to 300L, "fur" to 100L, "fog" to 80L, "foe" to 70L, "fit" to 60L, "fry" to 50L, "ftr" to 40L, "world" to 800L, "there" to 700L)
    private fun lexOf(extra: List<LexiconFile.Pair> = emptyList()) = LexiconFile.build(lookalikes.map { LexiconFile.Entry(it.first, it.second) }, extra)

    /** A word the decoder gives as its 4th or 5th guess for a glide of "for" in [lookalikes]. */
    private fun fourthOrFifthGuess(): String { val r = Rig(suggest = true, words = lexOf()); r.glide("for"); return r.presenter.glideMoreForTest().firstOrNull() ?: error("the decoder gave no 4th guess") }

    @Test fun aLookAlikeThatIsAlsoTheLikelyNextWordIsAReplacementNotTheNextWord() {
        val w = fourthOrFifthGuess()
        val r = Rig(suggest = true, words = lexOf(listOf(LexiconFile.Pair("for", w, 900L), LexiconFile.Pair("for", "world", 300L)))); r.glide("for")
        val c = r.view.candidatesForTest()
        assertEquals("world is the next word, in the middle: $c", "world", c[1])
        assertTrue("$w stands in a side box (a replacement), not in the middle (the next word): $c", w == c[0] || w == c[2])
        assertTrue("the middle box is a different word: $c", c[1] != w)
        r.tapChip(c.indexOf(w))
        assertEquals("the glided word is replaced, not kept with $w added behind it", "$w ", r.text)
    }

    @Test fun tappingTheNextWordStillAddsItAfterTheGlidedWord() {
        val r = Rig(suggest = true, words = lexOf(listOf(LexiconFile.Pair("for", "world", 900L)))); r.glide("for")
        val c = r.view.candidatesForTest(); assertEquals("world", c[1]); r.tapChip(1)
        assertEquals("for world ", r.text)
    }

    @Test fun whenTheKeyboardLostTrackOfTheGlideATapOnOneOfItsGuessesStillReplacesTheWord() {
        val w = fourthOrFifthGuess()
        val r = Rig(suggest = true, words = lexOf(listOf(LexiconFile.Pair("for", w, 900L)))); r.glide("for")
        // something moves the cursor away and back (an app, a late report): the keyboard forgets the glide, and the strip now shows the NEXT words
        r.et.setSelection(0); r.report(); r.et.setSelection(r.text.length); r.report()
        assertNull("the glide is forgotten", r.presenter.controller.lastGlide)
        val c = r.view.candidatesForTest(); assertTrue("$w is offered: $c", c.contains(w))
        r.tapChip(c.indexOf(w))
        assertEquals("replaced, not added", "$w ", r.text)
    }

    // ------------------------------------------------------------------ R32c (owner 2026-11-24): the e-mail addresses typed are offered at the top of an e-mail box

    private val emailType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
    private fun Rig.seedAddresses() { host.emails.learn("john@gmail.com"); host.emails.learn("john@gmail.com"); host.emails.learn("alice@work.example"); presenter.startInput(info) }

    @Test fun anEmailBoxOffersTheSavedAddressesAndATapPutsOneInWhole() {
        val r = Rig(inputType = emailType); r.seedAddresses()
        assertEquals("the most used first", listOf("john@gmail.com", "alice@work.example"), r.view.candidatesForTest())
        r.tapChip(1); assertEquals("alice@work.example", r.text)
    }

    @Test fun whatIsTypedNarrowsTheAddressesAndATapReplacesWhatWasTyped() {
        val r = Rig(inputType = emailType); r.seedAddresses()
        r.tapLetter("j"); r.tapLetter("o")
        assertEquals(listOf("john@gmail.com"), r.view.candidatesForTest()); r.tapChip(0)
        assertEquals("john@gmail.com", r.text); assertTrue("an address typed in full is not offered again", r.view.candidatesForTest().isEmpty())
    }

    @Test fun aNormalTextBoxHasNoAddressesInItsStrip() {
        val r = Rig(suggest = true); r.seedAddresses(); assertTrue(r.view.candidatesForTest().none { it.contains("@") })
    }

    @Test fun anAddressTypedInAnEmailBoxIsLearnedWhenTheBoxIsLeft() {
        val r = Rig(inputType = emailType); r.tapLetter("m"); r.tapLetter("e"); r.tap("@"); r.tapLetter("x"); r.tap(".com")
        assertEquals("me@x.com", r.text); assertTrue("not yet: the box is still open", r.host.emails.isEmpty)
        r.presenter.finishInput(); assertEquals(listOf("me@x.com"), r.host.emails.addresses())
    }

    @Test fun nothingIsLearnedOrOfferedInAPasswordBoxOrWithTheSwitchOff() {
        val p = Rig(inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD); for (c in "me") p.tapLetter(c.toString()); p.presenter.onKey(Key.Ch("@")); p.tapLetter("x"); p.presenter.onKey(Key.Ch(".com")); assertEquals("me@x.com", p.text); p.presenter.finishInput()
        assertTrue(p.host.emails.isEmpty)
        val off = Rig(inputType = emailType, extra = { it.putBoolean(Prefs.EMAIL_SUGGEST, false) }); off.seedAddresses()
        assertTrue("switched off: no addresses offered", off.view.candidatesForTest().isEmpty())
        off.tapLetter("m"); off.tap("@"); off.tapLetter("x"); off.tap(".com"); off.presenter.finishInput(); assertEquals("and none learned", 2, off.host.emails.size)
    }

    @Test fun anAddressPickedFromTheStripCountsOneUseButIsNotLearnedAsTyped() {
        val r = Rig(inputType = emailType); r.seedAddresses(); r.tapChip(0); r.presenter.finishInput()
        assertEquals("john@gmail.com", r.host.emails.addresses().first()); assertEquals(2, r.host.emails.size)
    }

    @Test fun forgettingTheAddressesEmptiesTheBookAndTheFile() {
        val r = Rig(inputType = emailType); r.seedAddresses(); r.host.forgetEmails()
        assertTrue(r.host.emails.isEmpty); r.presenter.startInput(r.info); assertTrue(r.view.candidatesForTest().isEmpty())
    }

    // ------------------------------------------------------------------ R33 (owner 2026-11-24, after alpha11): "it is still typing ave instead of and ... when i press and in the suggestions to fix it, it just adds and after ave"

    /** "ave" is a rare word, "and" the commonest; seven other words share the a-?-e path so that "and" is NOT among the decoder's five readings of an "ave" glide. */
    private val aveWords = listOf("and" to 200_000L, "ave" to 100L, "ace" to 50L, "age" to 50L, "axe" to 50L, "are" to 50L, "ape" to 50L, "ate" to 50L, "awe" to 50L, "avid" to 50L)
    private fun aveLex(next: Boolean = true) = LexiconFile.build(aveWords.map { LexiconFile.Entry(it.first, it.second) }, if (next) listOf(LexiconFile.Pair("ave", "and", 900L), LexiconFile.Pair("ave", "are", 300L)) else emptyList())

    @Test fun scratchLost() {
        val r = aveGlide()
        r.et.setSelection(0); r.report(); println("SCRATCH after move to 0: strip=${r.view.candidatesForTest()} lastGlide=${r.presenter.controller.lastGlide != null}")
        r.et.setSelection(r.text.length); r.report()
        println("SCRATCH after back: strip=${r.view.candidatesForTest()} lastGlide=${r.presenter.controller.lastGlide != null} before='${r.presenter.controller.before}' composing=${r.presenter.controller.composing}")
        val f = r.presenter.javaClass.getDeclaredField("session"); f.isAccessible = true; val sess = f.get(r.presenter) as GlideSession
        println("SCRATCH looksLikeFix(and)=${sess.looksLikeFix("and")} hasCandidates=${sess.hasCandidates()} isGuess=${sess.isGlideGuess("and")}")
        val g = r.host.engine; println("SCRATCH ln ave=${g.contextLn("ave", null, null)} and=${g.contextLn("and", null, null)} near=${SpellingNear.near("ave", "and")}")
        r.presenter.onCandidate("and"); println("SCRATCH text now='${r.text}'")
    }

    private fun aveGlide(): Rig {
        val r = Rig(suggest = true, words = aveLex()); r.glide("ave")
        assertEquals("precondition: the glide gives ave", "ave ", r.text)
        return r
    }

    @Test fun aWordThatIsNotAmongTheDecodersReadingsButLooksLikeTheGlidedWordAndIsFarMoreLikelyReplacesIt() {
        val r = aveGlide()
        val c = r.view.candidatesForTest(); assertTrue("and is offered: $c", c.contains("and"))
        assertFalse("precondition: and is not one of the decoder's own readings of this glide: ${r.presenter.glideMoreForTest()}", "and" in r.presenter.glideMoreForTest())
        assertTrue("it is offered among the replacements (a side box), not as the next word in the middle: $c", c.indexOf("and") != 1)
        r.tapChip(c.indexOf("and"))
        assertEquals("ave is replaced, and is not added behind it", "and ", r.text)
    }

    @Test fun evenIfItStandsInTheMiddleBoxATapOnSuchAWordReplaces() {
        val r = aveGlide(); r.presenter.onCandidate("and")
        assertEquals("and ", r.text)
    }

    @Test fun whenTheKeyboardLostTrackOfTheGlideTheSameTapStillReplaces() {
        val r = aveGlide()
        r.et.setSelection(0); r.report(); r.et.setSelection(r.text.length); r.report()
        assertNull("precondition: the glide is forgotten", r.presenter.controller.lastGlide)
        val c = r.view.candidatesForTest(); assertTrue("precondition: and is on the strip: $c", c.contains("and"))
        r.tapChip(c.indexOf("and"))
        assertEquals("and ", r.text)
    }

    @Test fun aGenuineNextWordStillGetsAddedBehindTheGlidedWord() {
        val common = LexiconFile.build(listOf("and" to 200_000L, "are" to 150_000L, "ave" to 100L, "ace" to 50L, "age" to 50L, "axe" to 50L).map { LexiconFile.Entry(it.first, it.second) }, listOf(LexiconFile.Pair("are", "and", 900L)))
        val r = Rig(suggest = true, words = common); r.glide("are")
        assertEquals("precondition: the glide gives are", "are ", r.text)
        r.presenter.onCandidate("and")        // "are" is a common word itself, so "and" is no correction of it: it is the next word
        assertEquals("are and ", r.text)
    }

    @Test fun theKeyboardLearnsFromThatFixSoAveStopsComingBack() {
        val r = aveGlide(); r.presenter.onCandidate("and"); r.presenter.finishInput()
        val memory = r.memory()
        assertTrue("the meant word is learned: $memory", memory.contains("and"))
    }

    // ------------------------------------------------------------------ R20a (owner 2026-11-18): what was just copied is the first box of the strip

    private fun Rig.copied(text: String, at: Long = 1_000L, sensitive: Boolean = false) { presenter.clipChipSource = { ClipOffer(text, at, sensitive) }; presenter.onClipboardChanged() }

    @Test fun whatWasJustCopiedIsTheFirstBoxAndOneTapPastesTheWholeCopy() {
        val r = Rig(suggest = true, words = pairedList)
        val url = "https://example.com/a/very/long/address?with=parts&and=more#fragment"
        r.copied(url)
        val chips = r.view.candidatesForTest()
        assertTrue("the copy is first: $chips", chips.first().startsWith("https://example.com/a/very/long/address")); assertTrue("three boxes at most: $chips", chips.size <= 3)
        r.tapChip(0)
        assertEquals("the whole copy goes in, not the shortened text", url, r.text)
        assertTrue("the pasted copy is not offered a second time: ${r.view.candidatesForTest()}", r.view.candidatesForTest().none { it.startsWith("https://") })
    }

    @Test fun duringAGlideTheStripKeepsTheGlideGuessesAndTheCopyWaitsForTheNextWord() {
        val r = Rig(suggest = true, words = pairedList); r.glide("hello"); r.copied("my copied note")
        assertTrue("the glide's strip stays: ${r.view.candidatesForTest()}", r.view.candidatesForTest().none { it == "my copied note" })
        r.tap("space")
        assertEquals("a new word starts: the copy is first now", "my copied note", r.view.candidatesForTest().firstOrNull())
        r.tap("\u232B"); assertTrue("and one more key (here Backspace) dismisses it: ${r.view.candidatesForTest()}", r.view.candidatesForTest().none { it == "my copied note" })
    }

    @Test fun theCopyIsFirstAtTheStartOfAWordAndGoesAwayForGoodAsSoonAsLettersAreTyped() {
        val r = Rig(suggest = true, words = pairedList); r.copied("my copied note")
        assertEquals("an empty box: the copy is first: ${r.view.candidatesForTest()}", "my copied note", r.view.candidatesForTest().firstOrNull())
        r.tap("h"); assertTrue("typing a letter: the strip is about the word: ${r.view.candidatesForTest()}", r.view.candidatesForTest().none { it == "my copied note" })
        r.tap("\u232B"); assertTrue("deleting the letter does not bring it back: ${r.view.candidatesForTest()}", r.view.candidatesForTest().none { it == "my copied note" })
        r.copied("my copied note", at = 2_000L)
        assertEquals("a NEW copy (even of the same text) is offered again", "my copied note", r.view.candidatesForTest().firstOrNull())
    }

    @Test fun aLongPressOrAGlideDismissesTheCopy() {
        val r = Rig(suggest = true, words = pairedList); r.copied("my copied note")
        r.presenter.onClipChipLong(); assertTrue("held: gone: ${r.view.candidatesForTest()}", r.view.candidatesForTest().none { it == "my copied note" })
        val g = Rig(suggest = true, words = pairedList); g.copied("another note"); g.glide("hello")
        g.tap("space"); g.tap("\u232B"); g.tap("space")
        assertTrue("a glide used the strip: the copy is not offered after it: ${g.view.candidatesForTest()}", g.view.candidatesForTest().none { it == "another note" })
    }

    @Test fun aLongOrMultiLineCopyIsShownOnOneLine() {
        val r = Rig(suggest = true, words = pairedList); r.copied("first line\n\n   second   line")
        assertEquals("first line second line", r.view.candidatesForTest().first())
        r.tapChip(0); assertEquals("the paste keeps the line breaks", "first line\n\n   second   line", r.text)
    }

    @Test fun theCopyIsOfferedWithSuggestionsOffTooAndASecretOneIsDotsThatStillPaste() {
        val off = Rig(suggest = false, words = pairedList); off.copied("copied"); assertEquals(listOf("copied"), off.view.candidatesForTest())
        off.tapChip(0); assertEquals("copied", off.text)
        val pw = Rig(inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, suggest = true, words = pairedList); pw.copied("s3cret!")
        assertEquals("a password box shows dots, never the text: ${pw.view.candidatesForTest()}", listOf("\u2022".repeat(7)), pw.view.candidatesForTest())
        pw.tapChip(0); assertEquals("and the tap pastes the real copy (the usual way to fill a password from a password manager)", "s3cret!", pw.text)
        val sens = Rig(suggest = true, words = pairedList); sens.copied("hunter2hunter2", sensitive = true)
        assertEquals("a copy marked sensitive is dots in any box: ${sens.view.candidatesForTest()}", "\u2022".repeat(14), sens.view.candidatesForTest().first())
        val sw = Rig(suggest = true, words = pairedList, extra = { it.putBoolean(Prefs.CLIP_SUGGEST, false) }); sw.copied("copied")
        assertTrue("the switch is off: ${sw.view.candidatesForTest()}", sw.view.candidatesForTest().none { it == "copied" })
    }

    @Test fun theCopyIsNotOfferedOnceItIsTooOld() {
        val r = Rig(suggest = true, words = pairedList)
        r.presenter.clipChipSource = { null }; r.presenter.onClipboardChanged()
        assertTrue("nothing fresh to offer: ${r.view.candidatesForTest()}", r.view.candidatesForTest().none { it == "old" })
    }

    // ------------------------------------------------------------------ R21 (owner 2026-11-18): no spaces between a quotation mark and the word inside it

    @Test fun aGlidedWordInsideQuotationMarksHasNoSpaceBeforeOrAfter() {
        val r = Rig(words = pairedList)
        r.presenter.onKey(Key.Ch("\"")); r.report(); r.glide("hello")
        assertEquals("no space between the opening quote and the word", "\"hello ", r.text)
        r.presenter.onKey(Key.Ch("\"")); r.report()
        assertEquals("the closing quote takes the glide's trailing space away", "\"hello\"", r.text)
        r.glide("world"); assertEquals("a word after the closing quote still gets its space", "\"hello\" world ", r.text)
    }

    @Test fun aWordPickedFromTheStripInsideQuotesBehavesTheSame() {
        val r = Rig(suggest = true, words = pairedList)
        r.presenter.onKey(Key.Ch("\"")); r.report(); r.glide("hello")
        r.tapChip(1)                                                                         // the next word (world), picked from the strip
        assertEquals("\"hello world ", r.text)
        r.presenter.onKey(Key.Ch("\"")); r.report(); assertEquals("\"hello world\"", r.text)
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
        val r = Rig(glidePref = false, extra = { it.putBoolean(Prefs.AUTOCAPS, true) }); for (c in "hi") r.tapLetter(c.toString()); r.tap("."); r.tap("space"); r.tap("A")
        assertEquals("Hi. A", r.text)
    }

    // ------------------------------------------------------------------ R18b: capitals where they make sense

    private fun typeText(r: Rig, text: String) { for (c in text) if (c == ' ') r.tap("space") else r.tapLetter(c.toString()) }
    private val caps: (android.content.SharedPreferences.Editor) -> Unit = { it.putBoolean(Prefs.AUTOCAPS, true) }

    @Test fun aDayTypedInSmallLettersGetsItsCapitalAndOneBackspaceTakesItBackAndTeachesIt() {
        val r = Rig(glidePref = false, extra = caps)
        assertEquals("a day is a name to the keyboard", "Monday", r.host.engine.caseFormOf("monday", "on"))
        typeText(r, "see you on monday "); assertEquals("the first word of the box and the day get capitals", "See you on Monday ", r.text)
        r.tap("\u232B"); assertEquals("one Backspace puts what was typed back", "See you on monday", r.text)
        r.tap("space"); assertEquals("and it is not capitalised again", "See you on monday ", r.text)
        assertNull("the person writes it small now: no capital for it any more", r.host.engine.caseFormOf("monday", "on"))
        assertEquals("and the same for the next time", "monday", run { typeText(r, "monday "); r.text.removeSuffix(" ").substringAfterLast(' ') })
    }

    @Test fun capitalsForNamesAreOffWithTheAutoCapitalsSwitchAndInFieldsThatAreNotText() {
        val off = Rig(glidePref = false, extra = { it.putBoolean(Prefs.AUTOCAPS, false) }); typeText(off, "see you on monday "); assertEquals("see you on monday ", off.text)
        val url = Rig(glidePref = false, inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI, extra = caps); typeText(url, "see monday "); assertEquals("see monday ", url.text)
    }

    @Test fun aGlidedNameInsideASentenceIsWrittenWithItsCapitalAndTheStripShowsIt() {
        val words = LexiconFile.build(listOf("hello" to 500L, "monday" to 400L, "sunday" to 300L, "money" to 200L).map { LexiconFile.Entry(it.first, it.second) }, emptyList())
        val r = Rig(glidePref = true, words = words, extra = caps)
        r.glide("hello"); assertEquals("Hello ", r.text)
        r.glide("monday"); assertEquals("a day keeps its capital when glided: ${r.text}", "Hello Monday ", r.text)
        assertEquals("the strip shows it as it stands", "Monday", r.view.candidatesForTest().first())
    }

    // ------------------------------------------------------------------ R18a: tap a word that is already in the text

    private val editList = LexiconFile.build(listOf("the" to 9000L, "ten" to 800L, "tea" to 700L, "tech" to 100L, "store" to 600L, "stone" to 100L, "went" to 500L, "to" to 8000L, "she" to 3000L, "food" to 400L, "good" to 900L, "i" to 5000L, "then" to 700L, "he" to 4000L).map { LexiconFile.Entry(it.first, it.second) },
        listOf(LexiconFile.Pair("to", "the", 400L), LexiconFile.Pair("the", "store", 300L)))

    /** The person moved the cursor to [at] (a tap on the text) and the keyboard heard about it. */
    private fun Rig.tapText(at: Int) { et.setSelection(at); report() }

    @Test fun tappingAWordAlreadyTypedOffersOtherWordsWithAClosePatternAndTappingOneReplacesJustThatWord() {
        val r = Rig(glidePref = false, suggest = true, words = editList); r.et.setText("went to teh store"); r.report()
        r.tapText(10)                                                                  // inside "teh"
        val offers = r.view.candidatesForTest(); assertEquals("the first offer is the word the fingers meant: $offers", "the", offers.first()); assertTrue("not the word itself: $offers", "teh" !in offers)
        val (x, y) = r.view.chipCenterForTest(0)!!; r.ev(MotionEvent.ACTION_DOWN, x, y); r.ev(MotionEvent.ACTION_UP, x, y)
        assertEquals("exactly that word, nothing else, no extra space", "went to the store", r.text); assertEquals("the cursor ends after it", 11, r.et.selectionStart)
        assertTrue("the other offers for the new word stay on the strip: ${r.view.candidatesForTest()}", r.view.candidatesForTest().isNotEmpty())
    }

    @Test fun aWordSeveralWordsBackIsJustAsGoodAndTheStripFollowsTheCursor() {
        val r = Rig(glidePref = false, suggest = true, words = editList); r.et.setText("went to teh store and teh food"); r.report()
        r.tapText(24)                                                                  // the SECOND "teh" (several words after the first)
        assertEquals("the", r.view.candidatesForTest().first())
        r.tapText(2)                                                                   // "went": nothing close to offer is fine, but the strip must not still show the old word's offers
        assertFalse("the" == r.view.candidatesForTest().firstOrNull() && r.view.candidatesForTest().size == 3 && "ten" in r.view.candidatesForTest() && "tea" in r.view.candidatesForTest() && "tech" in r.view.candidatesForTest())
    }

    @Test fun theOfferKeepsTheCapitalsOfTheWordItReplaces() {
        val r = Rig(glidePref = false, suggest = true, words = editList); r.et.setText("Teh store"); r.report(); r.tapText(2)
        assertEquals("The", r.view.candidatesForTest().first())
        val up = Rig(glidePref = false, suggest = true, words = editList); up.et.setText("went to TEH store"); up.report(); up.tapText(11)
        assertEquals("THE", up.view.candidatesForTest().first())
    }

    @Test fun typingNormallyIsNotInterruptedByTheOffers() {
        val r = Rig(glidePref = false, suggest = true, words = editList); r.et.setText("went to teh"); r.report(); r.tapText(11)    // the cursor put at the END of a word: offers
        assertEquals("the", r.view.candidatesForTest().first())
        r.tapLetter("x"); assertEquals("a key typed on goes back to completing the word: ${r.view.candidatesForTest()}", "went to tehx", r.text); assertFalse("the" in r.view.candidatesForTest().take(1) && "ten" in r.view.candidatesForTest())
    }

    @Test fun pickingAnOfferTeachesTheCorrectionAndTakesTheSlipBack() {
        val r = Rig(glidePref = false, suggest = true, words = editList); typeText(r, "went to teh "); typeText(r, "store "); assertEquals("(the typed slip is learned as typed)", 1f, run { r.presenter.finishInput(); app.getSharedPreferences("x", 0); 1f }, 0f)
        r.presenter.startInput(r.info); r.report(); r.tapText(10)
        val (x, y) = r.view.chipCenterForTest(0)!!; r.ev(MotionEvent.ACTION_DOWN, x, y); r.ev(MotionEvent.ACTION_UP, x, y)
        assertEquals("went to the store ", r.text)
        val mem = r.memory(); assertTrue("the correction teh -> the is remembered: $mem", mem.lines().any { it == "c teh the 2.00" })
        assertFalse("and the slip is not kept as a word of the person's: $mem", mem.lines().any { it.startsWith("w teh ") })
    }

    @Test fun noOffersInPasswordFieldsAndAddressBoxes() {
        val pw = Rig(inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, glidePref = false, suggest = true, words = editList); pw.et.setText("teh"); pw.report(); pw.tapText(2)
        assertTrue(pw.view.candidatesForTest().isEmpty())
        val url = Rig(inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI, glidePref = false, suggest = true, words = editList); url.et.setText("teh"); url.report(); url.tapText(2)
        assertTrue(url.view.candidatesForTest().isEmpty())
    }

    // ------------------------------------------------------------------ owner R23: held Backspace, the clipboard is never learned, the number pad

    @Test fun aLongHoldOfBackspaceTakesAWholeWordPerRepeat() {
        val r = Rig(glidePref = false, suggest = true, words = tapList)
        for (c in "hello world") if (c == ' ') r.tap("space") else r.tap(c.toString())
        r.presenter.onDeleteWord(); assertEquals("the word being tapped goes whole", "hello ", r.text)
        r.presenter.onDeleteWord(); assertEquals("then the spaces and the word before them", "", r.text)
    }

    @Test fun textCopiedAndPastedFromTheStripIsNeverLearnedAsWordsOrPairs() {
        val r = Rig(glidePref = false, suggest = true, words = tapList)
        r.copied("zebrafish quokka")
        r.tapChip(0); assertEquals("zebrafish quokka", r.text)
        r.tap("space")
        for (c in "hello") r.tap(c.toString()); r.tap("space")
        val m = r.memory()
        assertFalse("no pasted word is remembered: $m", m.contains("zebrafish") || m.contains("quokka"))
        assertTrue("what was typed by hand still is: $m", m.lines().any { it.startsWith("w hello ") })
    }

    @Test fun aNumberOnlyFieldShowsTheBigNumberPadAndPinsLearnNothing() {
        val r = Rig(inputType = InputType.TYPE_CLASS_NUMBER, glidePref = false)
        assertNull("no letters on a number field", r.view.centerOf("q")); assertNotNull(r.view.centerOf("5"))
        r.tap("7"); r.tap("0"); assertEquals("70", r.text)
        val pin = Rig(inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD, glidePref = false, suggest = true)
        assertNull("a PIN gets the pad too", pin.view.centerOf("q")); pin.tap("1"); pin.tap("2"); assertEquals("12", pin.text); assertEquals("", pin.memory())
        val text = Rig(glidePref = false)
        assertNotNull("a text field keeps its letters", text.view.centerOf("q"))
    }

    // ---- R24c (2026-11-18): "i tried to turn the translation feature off and it won't turn off"

    private fun passwordInfo() = EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
    private fun tool(r: Rig) = r.presenter.onToolbar(KeysView.TOOL_TRANSLATE)

    @Test fun translatingCanAlwaysBeTurnedOffEvenInABoxWhereItCannotBeTurnedOn() {
        val r = Rig(); val msgs = ArrayList<String>(); r.presenter.onTranslateMessage = { msgs.add(it) }
        tool(r); assertTrue("on in an ordinary box", r.view.translateOn)
        r.presenter.startInput(passwordInfo())                       // the person goes on to a login box: the key still tells the truth ...
        assertTrue(r.view.translateOn)
        tool(r)                                                      // ... and ONE tap there turns it off (it used to answer "not available in this box" and stay on)
        assertFalse("off after one tap in a password box", r.view.translateOn)
        assertEquals(app.getString(R.string.translate_off), msgs.last())
        tool(r); assertFalse("it cannot be turned ON there", r.view.translateOn); assertEquals(app.getString(R.string.translate_not_here), msgs.last())
        r.presenter.startInput(r.info); assertFalse("and it stays off in the next ordinary box", r.view.translateOn)
        tool(r); assertTrue("on again in an ordinary box", r.view.translateOn)
        tool(r); assertFalse("off again with one tap", r.view.translateOn)
    }

    @Test fun enterNeverTranslatesInAPasswordBoxAndNeverWithTheSettingOff() {
        val r = Rig(); val msgs = ArrayList<String>(); r.presenter.onTranslateMessage = { msgs.add(it) }
        tool(r); assertTrue(r.view.translateOn)
        // a login box while the toggle is still on: Enter is the normal Enter (digits: they are never "words", so this makes no request even if the guard were missing)
        r.presenter.startInput(passwordInfo()); r.et.setText("123456")
        val before = Engine.inputStats.enters
        r.presenter.onKey(Key.Go)
        assertEquals("the normal Enter ran", before + 1, Engine.inputStats.enters)
        assertFalse("no translating started", msgs.contains(app.getString(R.string.translate_working)))
        // the setting switched off while the toggle was on: the feature is OFF, not just hidden
        app.getSharedPreferences(Prefs.FILE, 0).edit().putBoolean(Prefs.TRANSLATE, false).commit()
        r.presenter.startInput(r.info); r.et.setText("123456")
        assertFalse("the key is unlit", r.view.translateOn)
        val b2 = Engine.inputStats.enters
        r.presenter.onKey(Key.Go)
        assertEquals("Enter is the normal Enter with the setting off", b2 + 1, Engine.inputStats.enters)
        assertFalse(msgs.contains(app.getString(R.string.translate_working)))
    }

    @Test fun aRebuiltKeyboardViewShowsTheRealTranslateState() {
        val r = Rig(); tool(r); assertTrue(r.view.translateOn)
        val v2 = KeysView(app); r.presenter.view = v2
        assertTrue("a new keys view starts in the real state, not unlit", v2.translateOn)
        tool(r); assertFalse(v2.translateOn)
        val v3 = KeysView(app); r.presenter.view = v3; assertFalse(v3.translateOn)
    }
}
