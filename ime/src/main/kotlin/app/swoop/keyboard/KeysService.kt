package app.swoop.keyboard

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import app.kestrel.core.input.Key

/**
 * The system keyboard. KB-100 skeleton: shows the keys view and types into the field. KB-101 puts the editor state machine (EditorBridge,
 * composing text, own-edit tracking) between this class and the editor; KB-104 does the Enter/action rules in full. Keep this class THIN:
 * logic belongs in :core where the JVM tests can reach it (plan 13.3).
 */
class KeysService : InputMethodService(), KeysView.Listener {
    private var keys: KeysView? = null
    private var info: EditorInfo? = null

    override fun onCreateInputView(): View = KeysView(this).also { it.listener = this; keys = it }

    /** The keyboard never takes over the whole screen with the extract UI. */
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(editorInfo: EditorInfo, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        info = editorInfo
        keys?.setForField((editorInfo.inputType and InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_NUMBER)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        info = null
    }

    override fun onKey(key: Key) {
        val ic = currentInputConnection ?: return
        when (key) {
            is Key.Ch -> ic.commitText(keys?.labelOf(key) ?: key.text, 1)
            Key.Backspace -> sendDownUp(KeyEvent.KEYCODE_DEL)
            Key.Go -> enter()
            else -> Unit
        }
    }

    private fun sendDownUp(code: Int) {
        val ic = currentInputConnection ?: return
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    /** The field's own action (Go / Search / Send / Done / Next) when it defines one, else a real Enter key (KB-104 completes this). */
    private fun enter() {
        val ei = info
        val action = (ei?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        val noAction = ((ei?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) currentInputConnection?.performEditorAction(action)
        else sendDownUp(KeyEvent.KEYCODE_ENTER)
    }
}
