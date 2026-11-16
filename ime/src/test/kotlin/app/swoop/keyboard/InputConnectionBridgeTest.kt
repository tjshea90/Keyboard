package app.swoop.keyboard

import android.app.Activity
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText
import app.kestrel.core.input.editor.FieldInfo
import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.ImeActions
import app.kestrel.core.input.editor.InputController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The controller on a REAL android.widget.EditText through the real InputConnection (framework code, API 29): proves the bridge and the
 * controller agree with Android's own semantics, not just with our fake. Selection reports are fed back by hand (Robolectric has no IMM).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class InputConnectionBridgeTest {
    private class Rig(initial: String = "", editorInfo: EditorInfo.() -> Unit = {}) {
        val act: Activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val et = EditText(act)
        val info = EditorInfo()
        val ic: InputConnection
        val controller: InputController
        var live: InputConnection? = null
        init {
            et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            act.setContentView(et); et.setText(initial); et.setSelection(initial.length)
            info.initialSelStart = initial.length; info.initialSelEnd = initial.length
            info.inputType = et.inputType
            info.editorInfo()
            ic = et.onCreateInputConnection(info)!!; live = ic
            controller = InputController(InputConnectionBridge { live })
            controller.startInput(FieldInfos.from(info))
        }
        /** What Android's InputMethodManager would do after each change. */
        fun report() { controller.onSelectionUpdate(et.selectionStart, et.selectionEnd, BaseInputConnection.getComposingSpanStart(et.text), BaseInputConnection.getComposingSpanEnd(et.text)) }
        fun act(f: InputController.() -> Unit) { controller.f(); report() }
        val text: String get() = et.text.toString()
    }

    @Test fun tappedLettersBecomeAComposingWordInARealEditText() {
        val r = Rig(); for (c in "hello") r.act { type(c.toString()) }
        assertEquals("hello", r.text); assertEquals(0, BaseInputConnection.getComposingSpanStart(r.et.text)); assertEquals(5, BaseInputConnection.getComposingSpanEnd(r.et.text))
        r.act { space() }; assertEquals("hello ", r.text); assertEquals(-1, BaseInputConnection.getComposingSpanStart(r.et.text))
    }

    @Test fun backspaceShortensTheWordThenUsesTheDeleteKey() {
        val r = Rig("ab"); for (c in "cd") r.act { type(c.toString()) }
        r.act { backspace() }; assertEquals("abc", r.text)
        r.act { space() }; r.act { backspace() }; assertEquals("abc", r.text)   // the space was plain text: the Delete key removes it
    }

    @Test fun aGlideGoesInWithItsSpacesAndOneBackspaceTakesItAll() {
        val r = Rig("hi"); r.act { commitGlide("there") }; assertEquals("hi there ", r.text)
        r.act { backspace() }; assertEquals("hi", r.text)
    }

    @Test fun aStripCandidateReplacesTheGlideOnTheRealEditable() {
        val r = Rig(); r.act { commitGlide("for") }
        assertTrue(r.controller.swapGlide("fir")); r.report(); assertEquals("fir ", r.text)
    }

    @Test fun theCursorKeysMoveTheCursorAndTypingGoesThere() {
        val r = Rig("hello world"); r.act { moveCursor(-5) }; assertEquals(6, r.et.selectionStart)
        r.act { type("X") }; assertEquals("hello Xworld", r.text)
    }

    @Test fun theFieldsOwnActionRunsOnEnter() {
        var got = -1
        val r = Rig("") { imeOptions = EditorInfo.IME_ACTION_SEARCH }
        r.et.setOnEditorActionListener { _, action, _ -> got = action; true }
        r.act { enter() }; assertEquals(EditorInfo.IME_ACTION_SEARCH, got); assertEquals("", r.text)
    }

    @Test fun enterWithoutAnActionInsertsANewline() { val r = Rig("a"); r.act { enter() }; assertEquals("a\n", r.text) }

    @Test fun noConnectionMeansNothingHappensAndNothingCrashes() {
        val r = Rig("abc"); r.live = null
        r.controller.type("d"); r.controller.backspace(); r.controller.commitGlide("x"); r.controller.enter(); r.controller.moveCursor(-1)
        assertEquals("abc", r.text)
        val b = InputConnectionBridge { null }
        assertNull(b.textBefore(5)); assertFalse(b.commitText("x")); assertFalse(b.sendKey(KeyEvent.KEYCODE_DEL)); assertFalse(b.beginBatch())
    }

    @Test fun aConnectionThatThrowsIsReportedAsAFailureNotACrash() {
        val broken = object : BaseInputConnection(android.view.View(android.app.Activity()), true) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean = throw IllegalStateException("dead")
            override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? = throw IllegalStateException("dead")
        }
        val b = InputConnectionBridge { broken }
        assertFalse(b.commitText("x")); assertNull(b.textBefore(3))
    }

    @Test fun fieldKindsFollowTheInputType() {
        fun kind(type: Int, opts: Int = 0): FieldInfo = FieldInfos.from(EditorInfo().apply { inputType = type; imeOptions = opts })
        assertEquals(FieldKind.PASSWORD, kind(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD).kind)
        assertEquals(FieldKind.PASSWORD, kind(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD).kind)
        assertEquals(FieldKind.PASSWORD, kind(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD).kind)
        assertEquals(FieldKind.EMAIL, kind(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS).kind)
        assertEquals(FieldKind.URL, kind(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI).kind)
        assertEquals(FieldKind.NUMBER, kind(InputType.TYPE_CLASS_NUMBER).kind)
        assertEquals(FieldKind.PHONE, kind(InputType.TYPE_CLASS_PHONE).kind)
        assertEquals(FieldKind.SEARCH, kind(InputType.TYPE_CLASS_TEXT, EditorInfo.IME_ACTION_SEARCH).kind)
        assertEquals(FieldKind.TEXT, kind(InputType.TYPE_CLASS_TEXT).kind)
        assertTrue(kind(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS).noSuggestions)
        assertTrue(kind(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_FILTER).noSuggestions)
    }

    @Test fun learningAndActionFlagsAreCarriedOver() {
        val f = FieldInfos.from(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT; imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or EditorInfo.IME_FLAG_NO_ENTER_ACTION })
        assertEquals(ImeActions.SEND, f.imeAction); assertTrue(f.noEnterAction); assertTrue(f.noPersonalizedLearning); assertFalse(f.learningAllowed)
        assertFalse(FieldInfos.wantsLetters(FieldInfo(kind = FieldKind.NUMBER))); assertTrue(FieldInfos.wantsLetters(FieldInfo()))
    }
}
