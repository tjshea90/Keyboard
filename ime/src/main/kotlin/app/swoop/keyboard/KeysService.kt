package app.swoop.keyboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo

/**
 * The system keyboard. A thin shell (plan 13.3): the lifecycle and the selection reports are forwarded to [KeyboardPresenter], which holds every decision
 * and is tested with a real EditText. KB-104 completes the Enter/caps rules, KB-106 adds the crash-loop fallback.
 */
class KeysService : InputMethodService() {
    private val main = Handler(Looper.getMainLooper())
    private val glide by lazy { GlideHost(applicationContext) }
    private val presenter by lazy { KeyboardPresenter(this, { currentInputConnection }, glide, { main.post(it) }) }

    override fun onCreateInputView(): View = KeysView(this).also { v ->
        presenter.view = v
        v.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob -> if (r - l != or - ol || b - t != ob - ot) presenter.prepareGlide() }
    }

    /** The keyboard never takes over the whole screen with the extract UI. */
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(editorInfo: EditorInfo, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        presenter.startInput(editorInfo)
    }

    override fun onFinishInput() { presenter.finishInput(); super.onFinishInput() }

    /** Android tells us every selection change, ours and everybody else's; the controller tells them apart (plan 6.4). */
    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        presenter.onUpdateSelection(newSelStart, newSelEnd, candidatesStart, candidatesEnd)
    }
}
