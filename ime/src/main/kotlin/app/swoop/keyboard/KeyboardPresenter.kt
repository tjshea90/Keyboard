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
import app.kestrel.core.input.glide.Suggestions
import app.kestrel.core.input.glide.TypingLearner

/**
 * Everything the keyboard does, minus the Android service plumbing: the keys view reports taps and glides, this decides (InputController for the text,
 * GlideSession for what a glide teaches, GlideHost for the engine) and tells the view what to show. The service is a thin shell around it, and the tests
 * drive it with a real EditText and a real view. All methods run on the UI thread; the engine's answers are posted back with [post].
 */
class KeyboardPresenter(
    private val context: Context,
    connection: () -> InputConnection?,
    private val glideProvider: () -> GlideHost,
    private val post: (Runnable) -> Unit,
    stats: app.kestrel.core.input.editor.InputStats = Engine.inputStats,
    now: () -> Long = { SystemClock.uptimeMillis() },
) : KeysView.Listener, GlideSession.Ui {
    val controller = InputController(InputConnectionBridge(connection), stats)
    private var glideOn = false
    private val session = GlideSession(controller, { if (glideOn) glide.engine else null }, now) { h -> if (!safeMode) glide.saveHabit(h) }
    private var learnOn = false
    private var suggestOn = false
    private var autoFixOn = false
    private var capitalizeI = false
    private var doubleSpaceOn = false
    private var lastSpaceAt = Long.MIN_VALUE / 2
    private val learner = TypingLearner(controller, { if (learnOn && !safeMode) glide.engine else null }, now)
    /** Bumped for every request to the engine: an answer for an older one is dropped. */
    private var suggestSeq = 0
    /** The engine's latest answer for the word being tapped (so a space can apply its automatic fix) and the typed word shown in quotes in the strip, when it is. */
    private var latest: Suggestions? = null
    private var literalShown: String? = null
    /** The typed word whose automatic fix the person just took back: it is not fixed again, whatever the engine still thinks. */
    private var noFixFor: String? = null
    private var lastShiftTap = Long.MIN_VALUE / 2   // "never": a first tap shortly after boot must not count as a double tap
    private val clock = now
    private val glide: GlideHost get() = glideProvider()
    /** True while the crash-loop guard has put the keyboard in safe mode: plain keys only, nothing that loads or learns. */
    var safeMode = false
    /** The globe key: the service switches to the next keyboard. */
    var onNextKeyboard: () -> Unit = {}

    var view: KeysView? = null
        set(v) { field = v; v?.listener = this; session.ui = this; prepareGlide() }

    // ------------------------------------------------------------------ lifecycle (the service forwards these)

    fun startInput(info: EditorInfo) {
        val field = FieldInfos.from(info)
        session.settle(); learner.settle()
        controller.startInput(field)
        val st = Settings(context)
        glideOn = !safeMode && st.glide && field.kind != FieldKind.PASSWORD
        learnOn = !safeMode && st.learn && field.learningAllowed
        suggestOn = !safeMode && st.suggest && field.composingAllowed
        autoFixOn = suggestOn && st.autoFix
        capitalizeI = !safeMode && st.capitalizeI && field.composingAllowed
        doubleSpaceOn = !safeMode && st.doubleSpace
        controller.autoSpace = !safeMode && st.autoSpace
        controller.autoCapsOn = st.autoCaps
        latest = null; literalShown = null; noFixFor = null; suggestSeq++
        view?.let {
            it.reset()
            it.applyOptions(st)
            it.setField(field.kind, ImeLayout.actionLabel(field.imeAction, field.noEnterAction))
            it.setGlideEnabled(glideOn)
            it.setSuggestionsEnabled(suggestOn)
            it.setShift(controller.shift)
        }
        prepareGlide()
        refreshSuggestions()
    }

    fun finishInput() { session.settle(); learner.settle(); suggestSeq++; controller.finishInput(); if (!safeMode) glide.engine.saveLearned() }

    /** Writes what was learned (cheap when nothing changed). */
    fun saveLearned() { if (!safeMode) try { glide.engine.saveLearned() } catch (_: Exception) { } }

    fun onUpdateSelection(newSelStart: Int, newSelEnd: Int, candStart: Int, candEnd: Int) {
        val ours = controller.onSelectionUpdate(newSelStart, newSelEnd, candStart, candEnd)
        view?.setShift(controller.shift)
        if (!ours) { learner.settle(); refreshSuggestions() }       // the person or the app moved the cursor or changed the text: new word, new guesses
    }

    /** The decoder needs the real key positions: known once the keys view has a size (and again when it changes). Cheap when nothing changed. */
    fun prepareGlide() {
        val v = view ?: return
        if (safeMode || (!glideOn && v.width <= 0)) return
        glide.prepare(v.glideKeys(), "${v.width}x${v.height}")
    }

    // ------------------------------------------------------------------ KeysView.Listener

    override fun onKey(key: Key) {
        session.beforeKey(key)
        val fix = autoFixFor(key)
        learner.beforeKey(key, fix)
        when (key) {
            is Key.Ch -> if (fix != null) controller.typeCorrected(fix, key.text) else if (key.text == " " && doubleSpaceOn && clock() - lastSpaceAt < DOUBLE_SPACE_MS && controller.doubleSpace()) { /* ". " is in */ } else controller.type(key.text)
            Key.Backspace -> controller.backspace()
            Key.Go -> controller.enter()
            Key.NextKeyboard -> onNextKeyboard()
            Key.Shift -> {
                val now = clock()
                if (now - lastShiftTap < DOUBLE_TAP_MS) controller.lockShift() else controller.tapShift()
                lastShiftTap = now
            }
            else -> Unit
        }
        if (key is Key.Ch && key.text == " ") lastSpaceAt = clock()
        session.afterKey(key)
        learner.afterKey()
        controller.revertedCorrection?.let { noFixFor = it.typed }
        view?.setShift(controller.shift)
        refreshSuggestions()
    }

    override fun onCursor(steps: Int) { session.settle(); learner.settle(); controller.moveCursor(steps); view?.setShift(controller.shift); refreshSuggestions() }

    // ------------------------------------------------------------------ word suggestions while tapping (N4) and the automatic fix

    /** The word an automatic fix will put in when [key] ends the word being tapped: the engine's latest answer for exactly this word, only for a space or sentence punctuation, never for one just taken back. */
    private fun autoFixFor(key: Key): String? {
        if (key !is Key.Ch || key.text !in FIX_AT) return null
        val typed = controller.composing ?: return null
        if (typed == noFixFor) return null
        if (capitalizeI) { val i = capitalizedI(typed); if (i != null) return i }
        if (!autoFixOn) return null
        val l = latest ?: return null
        return if (l.typed == typed) l.autoCorrect else null
    }

    /** "i" -> "I", "i'm" -> "I'm", "i'll", "i've", "i'd": the one fix that needs no word list. */
    private fun capitalizedI(typed: String): String? = if (typed == "i" || (typed.length in 3..4 && typed.startsWith("i'") && typed.substring(2) in I_ENDINGS)) "I" + typed.substring(1) else null

    /** Asks the engine what to offer for the word at the cursor (or the likely next word); the answer is shown only if it still fits what is on screen. */
    private fun refreshSuggestions() {
        if (!suggestOn || safeMode) return
        if (controller.lastGlide != null || view?.isGliding == true) return          // the strip shows a glide's other guesses
        val typed = controller.wordBeforeCursor()
        val b = controller.before
        val startsWord = typed == null && controller.selStart == controller.selEnd && (b.isEmpty() || b.last().isWhitespace())
        if (typed == null && !startsWord) { latest = null; literalShown = null; suggestSeq++; view?.clearCandidates(); return }
        val (p2, p1) = if (typed != null) controller.contextBefore(typed) else controller.previousTwo()
        val my = ++suggestSeq
        glide.engine.suggest(typed ?: "", p1, p2, 3) { res -> post(Runnable { if (my == suggestSeq) applySuggestions(res) }) }
    }

    private fun applySuggestions(res: Suggestions) {
        if (!suggestOn || controller.lastGlide != null || view?.isGliding == true) return
        if ((controller.wordBeforeCursor() ?: "") != res.typed) return                // the text moved on while the engine worked
        latest = res
        val strip = ArrayList<String>(3)
        literalShown = null
        if (res.typed.isNotEmpty() && !res.typedKnown) {
            val lit = "\u201C" + res.typed + "\u201D"
            val fix = res.autoCorrect
            if (fix != null) { strip.add(fix); strip.add(lit) } else strip.add(lit)
            literalShown = lit
        }
        for (w in res.words) if (strip.size < 3 && strip.none { it.equals(w, ignoreCase = true) }) strip.add(w)
        if (strip.isEmpty()) view?.clearCandidates() else view?.showCandidates(strip)
    }

    override fun onGlideMove(xs: FloatArray, ys: FloatArray, count: Int) {
        if (!glideOn) return
        suggestSeq++; latest = null                                                        // a glide began: no tap suggestion may land on the strip now
        val (p2, p1) = session.context()
        glide.engine.preview(xs, ys, count, p1, p2) { res -> post(Runnable { if (view?.isGliding == true) session.onPreview(res) }) }
    }

    override fun onGlideEnd(xs: FloatArray, ys: FloatArray, count: Int) {
        if (!glideOn) return
        session.glideLifted()
        val (p2, p1) = session.context()
        glide.engine.decode(xs, ys, count, p1, p2) { res: GlideResult -> post(Runnable { session.onDecoded(res, p1, p2); view?.setShift(controller.shift) }) }
    }

    override fun onCandidate(word: String) {
        if (controller.lastGlide != null) session.pick(word)
        else if (suggestOn) {
            val typed = controller.wordBeforeCursor()
            val w = if (word == literalShown && typed != null) typed else word      // the typed word in quotes: keep it as it is (and learn it)
            session.settle()
            latest = null; literalShown = null; suggestSeq++
            learner.pick(w)
            noFixFor = null
        }
        view?.setShift(controller.shift)
        refreshSuggestions()
    }

    // ------------------------------------------------------------------ GlideSession.Ui -> the view

    override fun showCandidates(words: List<String>) { view?.showCandidates(words) }
    override fun clearCandidates() { if (!suggestOn) view?.clearCandidates() }   // (with suggestions on, the next answer replaces the strip: no flicker)
    override fun showNoWord() { view?.showNoWord() }

    private companion object {
        const val DOUBLE_TAP_MS = 350L
        /** A second space this soon after the first makes a full stop. */
        const val DOUBLE_SPACE_MS = 600L
        val I_ENDINGS = setOf("m", "ll", "ve", "d")
        /** The keys after which an automatic fix may happen: the space and the punctuation that ends a sentence or a clause. */
        val FIX_AT = setOf(" ", ".", ",", "!", "?", ";", ":")
    }
}
