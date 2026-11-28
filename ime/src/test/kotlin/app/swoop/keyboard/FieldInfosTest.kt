package app.swoop.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import app.kestrel.core.input.editor.CapFlags
import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.ImeActions
import app.kestrel.core.input.editor.Pad
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

    @Test fun numberAndPhoneAndDateFieldsAskForTheirPad() {   // owner R23d: a field that takes only digits gets the big number pad
        fun pad(type: Int) = FieldInfos.from(EditorInfo().apply { inputType = type }).pad
        assertEquals(Pad.NUMBER, pad(InputType.TYPE_CLASS_NUMBER))
        assertEquals(Pad.NUMBER, pad(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED))
        assertEquals("a PIN is a number field too, still without learning", Pad.NUMBER, pad(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD))
        assertEquals(Pad.PHONE, pad(InputType.TYPE_CLASS_PHONE))
        assertEquals(Pad.DATETIME, pad(InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_DATE))
        assertEquals(Pad.DATETIME, pad(InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME))
        assertEquals(Pad.NONE, pad(InputType.TYPE_CLASS_TEXT))
        assertEquals("text with the number-password look is still text", Pad.NONE, pad(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        val date = FieldInfos.from(EditorInfo().apply { inputType = InputType.TYPE_CLASS_DATETIME })
        assertEquals("a date field has no words to compose", false, date.composingAllowed)
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

    /** R34b (owner 2026-11-26: "it still isn't recognizing email fields"): a plain text box that says "email" in its hint, label or field name is an e-mail box; a box ABOUT e-mail is not. */
    @Test fun aPlainTextBoxThatSaysEmailInItsHintLabelOrNameIsAnEmailBox() {
        fun kind(hint: String? = null, label: String? = null, name: String? = null, type: Int = T, opts: Int = 0) =
            FieldInfos.from(EditorInfo().also { it.inputType = type; it.imeOptions = opts; it.hintText = hint; it.label = label; it.fieldName = name }).kind
        for (h in listOf("Email", "E-mail address", "Enter your email", "Email or phone", "Username or email", "Correo electr\u00F3nico", "email_address")) assertEquals("hint '$h'", FieldKind.EMAIL, kind(hint = h))
        assertEquals(FieldKind.EMAIL, kind(label = "Your E-Mail")); assertEquals(FieldKind.EMAIL, kind(name = "user_email")); assertEquals(FieldKind.EMAIL, kind(name = "emailAddress"))
        assertEquals("the look of a web text box", FieldKind.EMAIL, kind(hint = "Email", type = T or InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT))
        for (h in listOf("Email subject", "Search mail", "Message body", "Email notifications", "Subject", "Name", "Phone", "")) assertEquals("hint '$h'", FieldKind.TEXT, kind(hint = h))
        assertEquals("no hint at all", FieldKind.TEXT, kind())
        assertEquals("a long text is a sentence, not a hint", FieldKind.TEXT, kind(hint = "Please type the email you used when you signed up for the newsletter last year"))
        assertEquals("a multi-line box is for writing", FieldKind.TEXT, kind(hint = "Email", type = T or InputType.TYPE_TEXT_FLAG_MULTI_LINE))
        assertEquals("a password box stays one", FieldKind.PASSWORD, kind(hint = "Email", type = T or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assertEquals("a search box stays one", FieldKind.SEARCH, kind(hint = "Email", opts = EditorInfo.IME_ACTION_SEARCH))
        assertEquals("an address bar stays one", FieldKind.URL, kind(hint = "Email", type = T or InputType.TYPE_TEXT_VARIATION_URI))
        assertEquals("a subject line is no address", FieldKind.TEXT, kind(hint = "Email", type = T or InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT))
        val f = FieldInfos.from(EditorInfo().also { it.inputType = T or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES; it.hintText = "Email" })
        assertEquals("an e-mail box asks for no capitals", 0, f.capsFlags); assertEquals(false, f.composingAllowed); assertEquals(true, f.learningAllowed)
    }
}
