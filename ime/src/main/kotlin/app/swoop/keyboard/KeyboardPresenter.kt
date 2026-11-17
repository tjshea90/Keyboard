package app.swoop.keyboard

import android.content.Context
import android.os.SystemClock
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import app.kestrel.core.input.ImeLayout
import app.kestrel.core.input.Key
import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.InputController
import app.kestrel.core.input.glide.EmojiMap
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
    private val connection: () -> InputConnection?,
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
    private var emojiOn = false
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
    // ------------------------------------------------------------------ emoji among the guesses (M11) and removing a guess for good (M15)

    private var emojiMap: EmojiMap? = null
    private var emojiLoading = false
    private val glyphPaint by lazy { android.graphics.Paint() }
    private val glyphOk = HashMap<String, Boolean>()
    /** Whether the font can draw an emoji (tests replace it). */
    var canDraw: (String) -> Boolean = { e -> try { glyphPaint.hasGlyph(e) } catch (_: Exception) { false } }
    private fun loadEmoji() {
        if (emojiMap != null || emojiLoading) return
        emojiLoading = true
        Io.exec { val m = try { EmojiMap(context.assets.open("emoji/en.tsv").use { String(it.readBytes(), Charsets.UTF_8) }) } catch (e: Exception) { null }; post(Runnable { emojiMap = m; emojiLoading = false }) }
    }
    /** The emoji for [word] when the person has the option on and this phone's font can draw it (an emoji the font lacks would show as an empty box). */
    private fun emojiFor(word: String?): String? {
        if (!emojiOn || word == null || safeMode) return null
        val m = emojiMap ?: return null
        return m.forWord(word) { e -> glyphOk.getOrPut(e) { canDraw(e) } }
    }
    /** The strip with the emoji for [word] as its last chip (three chips at most; an emoji already there stays where it is). */
    private fun withEmoji(strip: List<String>, word: String?): List<String> {
        val e = emojiFor(word) ?: return strip
        if (e in strip) return strip
        return strip.take(2) + e
    }

    /** An emoji chip was tapped: it takes the place of the word the person typed or glided (and a space follows, as after any suggestion). The emoji is never learned as a word. */
    private fun chooseEmoji(emoji: String) {
        if (controller.lastGlide != null) { controller.swapGlide(emoji); session.settle() }
        else { session.settle(); learner.settle(); controller.pickWord(emoji) }
        latest = null; literalShown = null; suggestSeq++
        view?.setShift(controller.shift)
        refreshSuggestions()
    }

    override fun onCandidateLong(word: String) {
        if (EmojiMap.isEmoji(word) || word == literalShown) return
        view?.confirmWord = word
    }

    override fun onRemoveWord(word: String, yes: Boolean) {
        if (!yes) return
        glide.engine.blockWord(word) { post(Runnable {
            val rest = (view?.currentCandidates() ?: emptyList()).filter { !it.equals(word, ignoreCase = true) }
            view?.showCandidates(rest)
            latest = null; suggestSeq++
            refreshSuggestions()
        }) }
    }

    /** The toolbar: the cog opens the settings screen, the clipboard icon shows or hides the clipboard, the translate icon switches translating. */
    var onOpenSettings: () -> Unit = {}
    var onToggleClipboard: () -> Unit = {}
    var onToggleTranslate: () -> Unit = {}

    /** A clipboard card was tapped: its text goes in at the cursor. */
    fun pasteText(text: String) { session.settle(); learner.settle(); controller.paste(text); view?.setShift(controller.shift); refreshSuggestions() }

    // ------------------------------------------------------------------ translate (M7): Enter translates the box to Mexican Spanish, Enter again sends

    private val flow = app.kestrel.core.translate.TranslateFlow(autoOffMs = 10 * 60_000L)
    private var translator: app.swoop.keyboard.translate.Translator? = null
    /** A short message for the strip (an error or a hint); shown until the next suggestion. */
    var onTranslateMessage: (String) -> Unit = {}

    private fun fieldText(): String? {
        val c = connection() ?: return null
        val b = c.getTextBeforeCursor(MAX_FIELD, 0) ?: return null
        val a = c.getTextAfterCursor(MAX_FIELD, 0) ?: ""
        return b.toString() + a.toString()
    }

    private fun replaceField(text: String) {
        val c = connection() ?: return
        c.beginBatchEdit()
        c.deleteSurroundingText(MAX_FIELD, MAX_FIELD)
        c.commitText(text, 1)
        c.endBatchEdit()
    }

    private fun toggleTranslate() {
        if (translateAllowed.not()) { onTranslateMessage(context.getString(R.string.translate_not_here)); return }
        flow.setOn(!flow.isOn(), clock())
        view?.translateOn = flow.isOn()
        if (flow.isOn()) onTranslateMessage(context.getString(R.string.translate_on_hint)) else translator?.cancel()
    }
    private var translateAllowed = true

    /** Enter with translating on. True when the flow took the key (translating, or a message shown); false means "do the normal Enter". */
    private fun translateEnter(): Boolean {
        if (!flow.isOn()) return false
        val field = fieldText() ?: return false
        when (flow.enter(field, clock())) {
            app.kestrel.core.translate.TranslateFlow.Do.SEND -> { controller.enter(); flow.sent(); view?.translateOn = flow.isOn(); return true }
            app.kestrel.core.translate.TranslateFlow.Do.NOTHING -> return true
            app.kestrel.core.translate.TranslateFlow.Do.TOO_LONG -> { onTranslateMessage(flow.why()); return true }
            app.kestrel.core.translate.TranslateFlow.Do.TRANSLATE -> {
                val n = flow.start(field, clock())
                val t = translator ?: app.swoop.keyboard.translate.Translator().also { translator = it }
                onTranslateMessage(context.getString(R.string.translate_working))
                t.translate(field) { res -> post(Runnable {
                    val out = flow.answer(n, fieldText() ?: "", res, clock())
                    if (out != null) { session.settle(); learner.settle(); replaceField(out); onTranslateMessage(context.getString(R.string.translate_done, flow.by())) }
                    else onTranslateMessage(flow.why())
                }) }
                return true
            }
        }
    }

    override fun onToolbar(tool: Int) {
        when (tool) {
            KeysView.TOOL_SETTINGS -> onOpenSettings()
            KeysView.TOOL_CLIPBOARD -> onToggleClipboard()
            KeysView.TOOL_TRANSLATE -> { toggleTranslate(); onToggleTranslate() }
        }
    }

    var view: KeysView? = null
        set(v) { field = v; v?.listener = this; session.ui = this; prepareGlide() }

    // ------------------------------------------------------------------ lifecycle (the service forwards these)

    fun startInput(info: EditorInfo) {
        val field = FieldInfos.from(info)
        if (Diag.enabled) Diag.d("startInput kind=${field.kind} noLearn=${field.noPersonalizedLearning} noSuggest=${field.noSuggestions} caps=${field.capsFlags} action=${field.imeAction} pkgIsKestrel=${info.packageName?.startsWith("app.kestrel.browser") == true} omnibox=${FieldInfos.hasOmniboxOption(info.privateImeOptions)}")     // (debug builds: facts only, never text)
        session.settle(); learner.settle()
        controller.startInput(field)
        val st = Settings(context)
        glideOn = !safeMode && st.glide && field.kind != FieldKind.PASSWORD
        learnOn = !safeMode && st.learn && field.learningAllowed
        suggestOn = !safeMode && st.suggest && field.composingAllowed
        autoFixOn = suggestOn && st.autoFix && field.kind != FieldKind.OMNIBOX            // (never rewrite what is typed into an address box)
        capitalizeI = !safeMode && st.capitalizeI && field.composingAllowed && field.kind != FieldKind.OMNIBOX
        doubleSpaceOn = !safeMode && st.doubleSpace
        emojiOn = !safeMode && st.emojiSuggestions && field.composingAllowed && field.kind != FieldKind.OMNIBOX
        if (emojiOn) loadEmoji()
        controller.autoSpace = !safeMode && st.autoSpace
        controller.autoCapsOn = st.autoCaps; controller.sentenceCaps = st.autoCaps
        latest = null; literalShown = null; noFixFor = null; suggestSeq++
        flow.leaveField(); translator?.cancel(); translateAllowed = !safeMode && field.kind != FieldKind.PASSWORD && field.kind != FieldKind.OMNIBOX && st.translateEnabled
        view?.let {
            it.reset()
            it.applyOptions(st)
            if (safeMode) it.showGlobe = true                // (the crash-loop safe layout always has the key that leaves this keyboard)
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
        if (Diag.enabled) Diag.d("sel report new=$newSelStart/$newSelEnd cand=$candStart/$candEnd ours=$ours believed=${controller.selStart}/${controller.selEnd} composing=${controller.composing != null}")
        view?.setShift(controller.shift)
        if (!ours) { learner.settle(); refreshSuggestions(); if (flow.isOn()) { val t = fieldText(); if (t != null && flow.fieldChanged(t, clock())) translator?.cancel() } }       // the person or the app moved the cursor or changed the text: new word, new guesses
    }

    /** The decoder needs the real key positions: known once the keys view has a size (and again when it changes). Cheap when nothing changed. */
    fun prepareGlide() {
        val v = view ?: return
        if (safeMode || (!glideOn && v.width <= 0)) return
        glide.prepare(v.glideKeys(), "${v.width}x${v.height}")
    }

    // ------------------------------------------------------------------ KeysView.Listener

    /** Debug build only (the emulator smoke test): which conditions behind "space after punctuation" and "capital after a sentence end" hold as a key is typed. Booleans, counts and state names: never a character. */
    private fun traceKey(key: Key.Ch, fixed: Boolean, stage: String) {
        val b = controller.before
        Diag.d("key $stage letter=${key.text.length == 1 && key.text[0].isLetter()} autoSpace=${controller.autoSpace} composing=${controller.composing != null} fix=$fixed sel=${controller.selStart}/${controller.selEnd} beforeLen=${b.length} lastPunct=${b.isNotEmpty() && b.last() in InputController.AUTO_SPACE_AFTER} lastSpace=${b.isNotEmpty() && b.last() == ' '} shift=${controller.shift} kind=${controller.field.kind} caps=${controller.field.capsFlags}")
    }

    override fun onKey(key: Key) {
        session.beforeKey(key)
        val fix = autoFixFor(key)
        if (Diag.enabled && key is Key.Ch) traceKey(key, fix != null, "before")
        learner.beforeKey(key, fix)
        when (key) {
            is Key.Ch -> if (fix != null) controller.typeCorrected(fix, key.text) else if (key.text == " " && doubleSpaceOn && clock() - lastSpaceAt < DOUBLE_SPACE_MS && controller.doubleSpace()) { /* ". " is in */ } else controller.type(key.text)
            Key.Backspace -> controller.backspace()
            Key.Go -> if (!translateEnter()) controller.enter()
            Key.NextKeyboard -> onNextKeyboard()
            Key.Shift -> {
                val now = clock()
                if (now - lastShiftTap < DOUBLE_TAP_MS) controller.lockShift() else controller.tapShift()
                lastShiftTap = now
            }
            else -> Unit
        }
        if (key is Key.Ch && key.text == " ") lastSpaceAt = clock()
        if (Diag.enabled && key is Key.Ch) traceKey(key, fix != null, "after")
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
        val shown = withEmoji(strip, res.typed.ifEmpty { null })
        if (shown.isEmpty()) view?.clearCandidates() else view?.showCandidates(shown)
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
        if (EmojiMap.isEmoji(word)) { chooseEmoji(word); return }
        if (controller.lastGlide != null || session.hasCandidates()) session.pick(word)        // (the strip still shows a glide's guesses even when an app rewrote the field)
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

    override fun showCandidates(words: List<String>) { view?.showCandidates(withEmoji(words, words.firstOrNull())) }
    override fun clearCandidates() { if (!suggestOn) view?.clearCandidates() }   // (with suggestions on, the next answer replaces the strip: no flicker)
    override fun showNoWord() { view?.showNoWord() }

    private companion object {
        const val DOUBLE_TAP_MS = 350L
        const val MAX_FIELD = 6000
        /** A second space this soon after the first makes a full stop. */
        const val DOUBLE_SPACE_MS = 600L
        val I_ENDINGS = setOf("m", "ll", "ve", "d")
        /** The keys after which an automatic fix may happen: the space and the punctuation that ends a sentence or a clause. */
        val FIX_AT = setOf(" ", ".", ",", "!", "?", ";", ":")
    }
}
