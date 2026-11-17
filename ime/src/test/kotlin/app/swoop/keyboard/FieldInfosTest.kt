package app.swoop.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import app.kestrel.core.input.editor.CapFlags
import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.ImeActions
import org.junit.Assert.assertEquals
import org.junit.Test

/** KB-104: the table of what each kind of field means to the keyboard (kind, learning, composing text, capitals, what Enter does). */
class FieldInfosTest {
    private class Row(val name: String, val type: Int, val opts: Int, val kind: FieldKind, val learn: Boolean, val composing: Boolean, val caps: Int, val action: Int = ImeActions.UNSPECIFIED, val noEnter: Boolean = false)
    private val T = InputType.TYPE_CLASS_TEXT
    private val rows = listOf(
        Row("plain text", T, 0, FieldKind.TEXT, true, true, 0),
        Row("sentences", T or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 0, FieldKind.TEXT, true, true, CapFlags.SENTENCES),
        Row("words", T or InputType.TYPE_TEXT_FLAG_CAP_WORDS, 0, FieldKind.TEXT, true, true, CapFlags.WORDS),
        Row("search box", T, EditorInfo.IME_ACTION_SEARCH, FieldKind.SEARCH, true, true, 0, ImeActions.SEARCH),
        Row("chat Send", T or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE, EditorInfo.IME_ACTION_SEND, FieldKind.TEXT, true, true, CapFlags.SENTENCES, ImeActions.SEND),
        Row("multi-line, Enter is a newline", T or InputType.TYPE_TEXT_FLAG_MULTI_LINE, EditorInfo.IME_FLAG_NO_ENTER_ACTION, FieldKind.TEXT, true, true, 0, noEnter = true),
        Row("password", T or InputType.TYPE_TEXT_VARIATION_PASSWORD, 0, FieldKind.PASSWORD, false, false, 0),
        Row("visible password", T or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, 0, FieldKind.PASSWORD, false, false, 0),
        Row("web password", T or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD, 0, FieldKind.PASSWORD, false, false, 0),
        Row("PIN", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD, 0, FieldKind.PASSWORD, false, false, 0),
        Row("e-mail", T or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, EditorInfo.IME_ACTION_NEXT, FieldKind.EMAIL, true, false, 0, ImeActions.NEXT),
        Row("web e-mail", T or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS, 0, FieldKind.EMAIL, true, false, 0),
        Row("address bar", T or InputType.TYPE_TEXT_VARIATION_URI, EditorInfo.IME_ACTION_GO, FieldKind.URL, true, false, 0, ImeActions.GO),
        Row("number", InputType.TYPE_CLASS_NUMBER, EditorInfo.IME_ACTION_DONE, FieldKind.NUMBER, true, false, 0, ImeActions.DONE),
        Row("phone", InputType.TYPE_CLASS_PHONE, 0, FieldKind.PHONE, true, false, 0),
        Row("no suggestions", T or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS, 0, FieldKind.TEXT, true, false, 0),
        Row("filter list", T or InputType.TYPE_TEXT_VARIATION_FILTER, 0, FieldKind.TEXT, true, false, 0),
        Row("incognito (app asked for no learning)", T, EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING, FieldKind.TEXT, false, true, 0),
        Row("incognito search", T, EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING, FieldKind.SEARCH, false, true, 0, ImeActions.SEARCH),
    )

    @Test fun everyRowOfTheFieldTable() {
        for (r in rows) {
            val f = FieldInfos.from(EditorInfo().apply { inputType = r.type; imeOptions = r.opts })
            assertEquals("${r.name}: kind", r.kind, f.kind)
            assertEquals("${r.name}: learning allowed", r.learn, f.learningAllowed)
            assertEquals("${r.name}: composing allowed", r.composing, f.composingAllowed)
            assertEquals("${r.name}: capitals requested", r.caps, f.capsFlags)
            assertEquals("${r.name}: action", r.action, f.imeAction)
            assertEquals("${r.name}: Enter is a newline", r.noEnter, f.noEnterAction)
        }
    }

    @Test fun theInitialSelectionIsCarriedOver() {
        val f = FieldInfos.from(EditorInfo().apply { inputType = T; initialSelStart = 4; initialSelEnd = 9 })
        assertEquals(4, f.initialSelStart); assertEquals(9, f.initialSelEnd)
    }

    /** docs/KESTREL-LINK.md: the Kestrel browser marks its address-and-search box with a private option; the box then learns, glides and guesses like a search box but takes no automatic capitals, spaces or fixes. */
    @Test fun theKestrelAddressBoxIsTheOmniboxAndNobodyElseIs() {
        fun info(type: Int, opts: Int, private: String?) = FieldInfos.from(EditorInfo().also { it.inputType = type; it.imeOptions = opts; it.privateImeOptions = private })
        val box = info(T, EditorInfo.IME_ACTION_GO, "other.app.x=1,app.kestrel.omnibox=1")
        assertEquals(FieldKind.OMNIBOX, box.kind); assertEquals(ImeActions.GO, box.imeAction)
        assertEquals("words are tapped as composing text and guessed", true, box.composingAllowed); assertEquals("and learned", true, box.learningAllowed); assertEquals("no automatic capitals", 0, box.capsFlags)
        assertEquals("a private tab asks for no learning with the standard flag", false, info(T, EditorInfo.IME_ACTION_GO or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING, FieldInfos.OMNIBOX_OPTION).learningAllowed)
        assertEquals("without the option an address bar stays a URL field", FieldKind.URL, info(T or InputType.TYPE_TEXT_VARIATION_URI, 0, null).kind)
        assertEquals("the option does nothing in a password field", FieldKind.PASSWORD, info(T or InputType.TYPE_TEXT_VARIATION_PASSWORD, 0, FieldInfos.OMNIBOX_OPTION).kind)
        assertEquals("a look-alike option is not the option", FieldKind.TEXT, info(T, 0, "app.kestrel.omnibox=2,xapp.kestrel.omnibox=1").kind)
        assertEquals("no option, no change", FieldKind.TEXT, info(T, 0, "").kind)
    }
}
