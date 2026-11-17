package app.swoop.keyboard

import android.content.Context
import android.os.SystemClock
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import app.kestrel.core.input.ImeLayout
import app.kestrel.core.input.Key
import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.InputController
import app.kestrel.core.input.glide.GlideResult
import app.kestrel.core.input.glide.GlideSession

/**
 * Everything the keyboard does, minus the Android service plumbing: the keys view reports taps and glides, this decides (InputController for the text,
 * GlideSession for what a glide teaches, GlideHost for the engine) and tells the view what to show. The service is a thin shell around it, and the tests
 * drive it with a real EditText and a real view. All methods run on the UI thread; the engine's answers are posted back with [post].
 */
class KeyboardPresenter(
    private val context: Context,
    connection: () -> InputConnection?,
    private val glide: GlideHost,
    private val post: (Runnable) -> Unit,
    now: () -> Long = { SystemClock.uptimeMillis() },
) : KeysView.Listener, GlideSession.Ui {
    val controller = InputController(InputConnectionBridge(connection))
    private var glideOn = false
    private val session = GlideSession(controller, { if (glideOn) glide.engine else null }, now) { h -> glide.saveHabit(h) }
    private var lastShiftTap = Long.MIN_VALUE / 2   // "never": a first tap shortly after boot must not count as a double tap
    private val clock = now

    var view: KeysView? = null
        set(v) { field = v; v?.listener = this; session.ui = this; prepareGlide() }

    private fun prefs() = context.getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)

    // ------------------------------------------------------------------ lifecycle (the service forwards these)

    fun startInput(info: EditorInfo) {
        val field = FieldInfos.from(info)
        session.settle()
        controller.startInput(field)
        glideOn = prefs().getBoolean(Prefs.GLIDE, true) && field.kind != FieldKind.PASSWORD
        view?.let {
            it.reset()
            it.setField(field.kind, ImeLayout.actionLabel(field.imeAction, field.noEnterAction))
            it.setGlideEnabled(glideOn)
            it.setShift(controller.shift)
        }
        prepareGlide()
    }

    fun finishInput() { session.settle(); controller.finishInput(); glide.engine.saveLearned() }

    fun onUpdateSelection(newSelStart: Int, newSelEnd: Int, candStart: Int, candEnd: Int) {
        controller.onSelectionUpdate(newSelStart, newSelEnd, candStart, candEnd)
        view?.setShift(controller.shift)
    }

    /** The decoder needs the real key positions: known once the keys view has a size (and again when it changes). Cheap when nothing changed. */
    fun prepareGlide() {
        val v = view ?: return
        if (!glideOn && v.width <= 0) return
        glide.prepare(v.glideKeys(), "${v.width}x${v.height}")
    }

    // ------------------------------------------------------------------ KeysView.Listener

    override fun onKey(key: Key) {
        session.beforeKey(key)
        when (key) {
            is Key.Ch -> controller.type(key.text)
            Key.Backspace -> controller.backspace()
            Key.Go -> controller.enter()
            Key.Shift -> {
                val now = clock()
                if (now - lastShiftTap < DOUBLE_TAP_MS) controller.lockShift() else controller.tapShift()
                lastShiftTap = now
            }
            else -> Unit
        }
        session.afterKey(key)
        view?.setShift(controller.shift)
    }

    override fun onCursor(steps: Int) { session.settle(); controller.moveCursor(steps); view?.setShift(controller.shift) }

    override fun onGlideMove(xs: FloatArray, ys: FloatArray, count: Int) {
        if (!glideOn) return
        val (p2, p1) = session.context()
        glide.engine.preview(xs, ys, count, p1, p2) { res -> post(Runnable { if (view?.isGliding == true) session.onPreview(res) }) }
    }

    override fun onGlideEnd(xs: FloatArray, ys: FloatArray, count: Int) {
        if (!glideOn) return
        session.glideLifted()
        val (p2, p1) = session.context()
        glide.engine.decode(xs, ys, count, p1, p2) { res: GlideResult -> post(Runnable { session.onDecoded(res, p1, p2); view?.setShift(controller.shift) }) }
    }

    override fun onCandidate(word: String) { session.pick(word); view?.setShift(controller.shift) }

    // ------------------------------------------------------------------ GlideSession.Ui -> the view

    override fun showCandidates(words: List<String>) { view?.showCandidates(words) }
    override fun clearCandidates() { view?.clearCandidates() }
    override fun showNoWord() { view?.showNoWord() }

    private companion object { const val DOUBLE_TAP_MS = 350L }
}

/** The keyboard's few settings (SharedPreferences, private to the app). */
object Prefs {
    const val FILE = "swoop"
    const val GLIDE = "glide_typing"
}
