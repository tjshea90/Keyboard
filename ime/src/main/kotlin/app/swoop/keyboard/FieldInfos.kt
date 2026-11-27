package app.swoop.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import app.kestrel.core.input.editor.CapFlags
import app.kestrel.core.input.editor.FieldInfo
import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.Pad

/** EditorInfo -> the plain facts the controller needs (KB-104 extends the table with the Enter/caps rules and its tests). */
object FieldInfos {
    fun from(e: EditorInfo): FieldInfo {
        val cls = e.inputType and InputType.TYPE_MASK_CLASS
        val variation = e.inputType and InputType.TYPE_MASK_VARIATION
        val action = e.imeOptions and EditorInfo.IME_MASK_ACTION
        var noSuggestions = (e.inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
        val kind = when (cls) {
            InputType.TYPE_CLASS_NUMBER -> if (variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) FieldKind.PASSWORD else FieldKind.NUMBER
            InputType.TYPE_CLASS_PHONE -> FieldKind.PHONE
            InputType.TYPE_CLASS_TEXT -> when (variation) {
                InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> FieldKind.PASSWORD
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> FieldKind.EMAIL
                InputType.TYPE_TEXT_VARIATION_URI -> FieldKind.URL
                InputType.TYPE_TEXT_VARIATION_FILTER -> { noSuggestions = true; FieldKind.TEXT }
                else -> if (action == EditorInfo.IME_ACTION_SEARCH) FieldKind.SEARCH
                    // R34b (owner 2026-11-26: "it still isn't recognizing email fields"): a login page or an app that makes its e-mail box a plain text box still says what it is in its hint, label or field name
                    else if ((variation == InputType.TYPE_TEXT_VARIATION_NORMAL || variation == InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT) && (e.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0 && looksLikeEmailBox(e.hintText, e.label, e.fieldName)) FieldKind.EMAIL
                    else FieldKind.TEXT
            }
            InputType.TYPE_CLASS_DATETIME -> FieldKind.NUMBER           // dates and times are digits and a few marks: no words to guess, no learning, the number pad (R23d)
            else -> FieldKind.TEXT
        }
        // R23d: a field for numbers only gets a number pad, a PIN or number password too (it stays a password for everything else: nothing learned, the copy shown as dots)
        val pad = when (cls) { InputType.TYPE_CLASS_NUMBER -> Pad.NUMBER; InputType.TYPE_CLASS_PHONE -> Pad.PHONE; InputType.TYPE_CLASS_DATETIME -> Pad.DATETIME; else -> Pad.NONE }
        // The Kestrel browser marks its address-and-search box in privateImeOptions (any app may send such options; the only effect here is a layout and typing style, nothing is shared).
        val omnibox = cls == InputType.TYPE_CLASS_TEXT && kind != FieldKind.PASSWORD && hasOmniboxOption(e.privateImeOptions)
        val fieldKind = if (omnibox) FieldKind.OMNIBOX else kind
        return FieldInfo(
            kind = fieldKind,
            imeAction = action,                      // EditorInfo.IME_ACTION_* and ImeActions use the same numbers
            noEnterAction = (e.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0,
            noPersonalizedLearning = (e.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0,
            noSuggestions = noSuggestions,
            capsFlags = if (cls == InputType.TYPE_CLASS_TEXT && fieldKind != FieldKind.PASSWORD && fieldKind != FieldKind.EMAIL && fieldKind != FieldKind.URL && fieldKind != FieldKind.OMNIBOX) e.inputType and CapFlags.ALL else 0,
            initialSelStart = e.initialSelStart, initialSelEnd = e.initialSelEnd,
            pad = pad,
        )
    }

    private val MAIL_WORD = Regex("e[-_ ]?mail|correo", RegexOption.IGNORE_CASE)
    private val NOT_AN_ADDRESS = Regex("subject|asunto|body|message|mensaje|signature|firma|template|notif|preview|search|buscar|filter", RegexOption.IGNORE_CASE)

    /**
     * R34b: does what a box says about itself (its hint, label and field name) mean "type an e-mail address here"? Short texts only, and never a box about an e-mail ("Email subject", "Search mail", "Email notifications"):
     * a wrong yes costs a box its word suggestions, so the words that say it is NOT an address are checked first. Facts only; the texts are read, never stored or logged.
     */
    fun looksLikeEmailBox(hint: CharSequence?, label: CharSequence?, fieldName: String?): Boolean {
        val sb = StringBuilder(80)
        for (t in arrayOf<CharSequence?>(hint, label, fieldName)) { if (t == null) continue; if (t.length > 60) return false; sb.append(t).append(' ') }
        if (sb.isBlank()) return false
        val all = sb.toString()
        return MAIL_WORD.containsMatchIn(all) && !NOT_AN_ADDRESS.containsMatchIn(all)
    }

    /** The private option the Kestrel browser sets on its address-and-search box (docs/KESTREL-LINK.md): "app.kestrel.omnibox=1" among the comma-separated options. */
    const val OMNIBOX_OPTION = "app.kestrel.omnibox=1"
    fun hasOmniboxOption(privateImeOptions: String?): Boolean = privateImeOptions != null && privateImeOptions.split(',').any { it.trim() == OMNIBOX_OPTION }
}
