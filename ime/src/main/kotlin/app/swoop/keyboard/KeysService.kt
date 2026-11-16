package app.swoop.keyboard

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.view.View
import android.view.inputmethod.EditorInfo
import app.kestrel.core.input.Key
import app.kestrel.core.input.editor.InputController

/**
 * The system keyboard. THIN on purpose (plan 13.3): every decision about the text is made by [InputController] in :core, where the JVM tests and
 * the fuzzer reach it; this class forwards the lifecycle, the selection reports and the keys, and keeps the keys view in step.
 * KB-104 completes the Enter/caps rules, KB-103 adds the glide engine, KB-106 the crash-loop fallback.
 */
class KeysService : InputMethodService(), KeysView.Listener {
    private var keys: KeysView? = null
    private val controller = InputController(InputConnectionBridge { currentInputConnection })
    private var lastShiftTap = 0L

    override fun onCreateInputView(): View = KeysView(this).also { it.listener = this; keys = it }

    /** The keyboard never takes over the whole screen with the extract UI. */
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(editorInfo: EditorInfo, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        val field = FieldInfos.from(editorInfo)
        controller.startInput(field)
        keys?.setForField(FieldInfos.wantsLetters(field))
        keys?.setShift(controller.shift)
    }

    override fun onFinishInput() { controller.finishInput(); super.onFinishInput() }

    /** Android tells us every selection change, ours and everybody else's; the controller tells them apart (plan 6.4). */
    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        controller.onSelectionUpdate(newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        keys?.setShift(controller.shift)
    }

    override fun onKey(key: Key) {
        when (key) {
            is Key.Ch -> controller.type(key.text)
            Key.Backspace -> controller.backspace()
            Key.Go -> controller.enter()
            Key.Shift -> {
                val now = SystemClock.uptimeMillis()
                if (now - lastShiftTap < DOUBLE_TAP_MS) controller.lockShift() else controller.tapShift()
                lastShiftTap = now
            }
            else -> Unit
        }
        keys?.setShift(controller.shift)
    }

    private companion object { const val DOUBLE_TAP_MS = 350L }
}
