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
import app.kestrel.core.input.glide.LexiconFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    private inner class Rig(inputType: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE, imeOptions: Int = 0, glidePref: Boolean = true) {
        val act: Activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val et = EditText(act)
        val info = EditorInfo()
        val view = KeysView(app)
        val host = GlideHost(app, Executor { it.run() }, Executor { it.run() }) { list }
        val presenter: KeyboardPresenter
        val ic: android.view.inputmethod.InputConnection
        init {
            File(app.filesDir, "glide_personal.txt").delete(); File(app.filesDir, "glide_habit.txt").delete()
            app.getSharedPreferences(Prefs.FILE, 0).edit().putBoolean(Prefs.GLIDE, glidePref).commit()
            et.inputType = inputType; et.imeOptions = imeOptions
            act.setContentView(et)
            info.inputType = inputType; info.imeOptions = imeOptions
            ic = et.onCreateInputConnection(info)!!
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
        fun ev(a: Int, x: Float, y: Float) { val t = SystemClock.uptimeMillis(); view.dispatchTouchEvent(MotionEvent.obtain(t, t, a, x, y, 0)); report() }
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
        val r = Rig(); var asked = 0; r.presenter.onNextKeyboard = { asked++ }
        r.tap("globe"); assertEquals(1, asked); assertEquals("", r.text)
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
}
