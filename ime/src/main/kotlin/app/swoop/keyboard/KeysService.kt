package app.swoop.keyboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import app.kestrel.core.input.CrashGuard
import java.io.File

/**
 * The system keyboard. A thin shell (plan 13.3): the lifecycle and the selection reports are forwarded to [KeyboardPresenter], which holds every decision
 * and is tested with a real EditText. SAFE MODE (KB-106): if the service started three times within a minute without ever drawing its keys, it runs with the plain
 * keys only (no glide engine, no word list, no learning), so a crash loop can never lock the owner out of typing; the globe key is always on the bottom row.
 */
class KeysService : InputMethodService() {
    private val main = Handler(Looper.getMainLooper())
    private val glide by lazy { Engine.host(applicationContext) }
    private val presenter by lazy { KeyboardPresenter(this, { currentInputConnection }, { glide }, { main.post(it) }) }
    /** True when the crash-loop guard switched the plain keys on. */
    var safeMode = false; private set
    private val guard by lazy { CrashGuard(FileStore(File(filesDir, "crash_guard.txt")), { SystemClock.elapsedRealtime() }) }

    override fun onCreate() {
        super.onCreate()
        safeMode = try { guard.onStart() } catch (e: Exception) { false }
        Engine.safeMode = safeMode
        presenter.safeMode = safeMode
        presenter.onNextKeyboard = { nextKeyboard() }
    }

    override fun onCreateInputView(): View = KeyboardRoot(this).let { root ->
        val v = root.keys
        presenter.view = v
        v.onFirstDraw = { try { guard.healthy() } catch (_: Exception) { } }
        v.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob -> if (r - l != or - ol || b - t != ob - ot) presenter.prepareGlide() }
        root
    }

    /** The globe key: the next keyboard; if Android will not switch (a single keyboard, or no permission) the picker. */
    private fun nextKeyboard() {
        val ok = try { switchToNextInputMethod(false) } catch (e: Exception) { false }
        if (!ok) try { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() } catch (_: Exception) { }
    }

    /** The keyboard never takes over the whole screen with the extract UI. */
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(editorInfo: EditorInfo, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        presenter.startInput(editorInfo)
    }

    override fun onFinishInput() { presenter.finishInput(); super.onFinishInput() }

    /** The keyboard goes away (Back, another app): what was learned is written now, so a killed process loses nothing. */
    override fun onFinishInputView(finishingInput: Boolean) { presenter.saveLearned(); super.onFinishInputView(finishingInput) }
    override fun onWindowHidden() { presenter.saveLearned(); super.onWindowHidden() }
    override fun onDestroy() { presenter.saveLearned(); super.onDestroy() }

    /** Android tells us every selection change, ours and everybody else's; the controller tells them apart (plan 6.4). */
    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        presenter.onUpdateSelection(newSelStart, newSelEnd, candidatesStart, candidatesEnd)
    }
}

/** [CrashGuard.Store] on a small private file. */
class FileStore(private val f: File) : CrashGuard.Store {
    override fun read(): String = if (f.isFile) f.readText() else ""
    override fun write(text: String) { if (text.isEmpty()) f.delete() else f.writeText(text) }
}
