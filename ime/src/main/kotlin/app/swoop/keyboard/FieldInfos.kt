package app.swoop.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import app.kestrel.core.input.editor.FieldInfo
import app.kestrel.core.input.editor.FieldKind

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
                else -> if (action == EditorInfo.IME_ACTION_SEARCH) FieldKind.SEARCH else FieldKind.TEXT
            }
            else -> FieldKind.TEXT
        }
        return FieldInfo(
            kind = kind,
            imeAction = action,                      // EditorInfo.IME_ACTION_* and ImeActions use the same numbers
            noEnterAction = (e.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0,
            noPersonalizedLearning = (e.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0,
            noSuggestions = noSuggestions,
            initialSelStart = e.initialSelStart, initialSelEnd = e.initialSelEnd,
        )
    }
}
