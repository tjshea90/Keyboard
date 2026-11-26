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
 * How long a copy stays the first box of the strip (owner R20a). HeliBoard, the one keyboard whose rule is public, uses 3 minutes; Gboard's own chip time is not documented (its one hour is for the clipboard history). Five minutes is
 * our pick between them, and typing, deleting, gliding, a long press or a tap dismisses the copy at once anyway. research/R20-clipboard-and-strip.md
 */
const val CLIP_CHIP_TTL_MS = 5L * 60L * 1000L

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
    // ---- owner R20a: what was just copied is the first box of the strip
    /** The newest copy still fresh enough to offer (set by the service from its clipboard host). */
    var clipChipSource: () -> ClipOffer? = { null }
    private var clipOn = false
    /** The copy that is on the strip now, and the time of the copy that was already pasted from it (not offered again). */
    private var clipShown: ClipOffer? = null
    private var clipUsedAt = -1L
    /** The middle box after a glide is the next word (Settings, owner R20b). */
    private var glideNextOn = true
    private var doubleSpaceOn = false
    private var emojiOn = false
    /**
     * R18a: the cursor was PUT in a word that is already in the text (a tap, a slide over the space bar): the strip offers words with a close glide pattern in its place. Cleared by the next key, pick or glide.
     * [editWord] is the word the offers on the strip are for; [editRaw] maps what the strip shows to the word itself.
     */
    private var userMoved = false
    private var editWord: String? = null
    private var editRaw: Map<String, String> = emptyMap()
    /** R18b: names, days and months typed or glided in small letters are written with their capital (the auto-capitals setting). */
    private var properOn = false
    private var lastSpaceAt = Long.MIN_VALUE / 2
    private val learner = TypingLearner(controller, { if (learnOn && !safeMode) glide.engine else null }, now)
    // ---- R32c: the e-mail addresses typed are remembered and offered in e-mail boxes
    private var emailOn = false
    private val emailLearner = app.kestrel.core.input.glide.EmailLearner(controller) { if (emailOn && !safeMode) glide.emails else null }
    /** The addresses on the strip now (an e-mail box only): a tap on one replaces what was typed of the address. */
    private var emailShown: List<String> = emptyList()
    /** R34c: what each chip of the address strip shows (a domain only, once the part before the @ is typed) -> the whole address a tap puts in. */
    private var emailLabels: Map<String, String> = emptyMap()
    private fun saveEmailsIfChanged() { if (emailOn && !safeMode) try { glide.saveEmails() } catch (_: Exception) { } }
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
    fun pasteText(text: String) { userMoved = false; session.settle(); learner.settle(); controller.paste(text); view?.setShift(controller.shift); refreshSuggestions() }

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

    /**
     * The toolbar toggle. R24c (2026-11-18, the owner could not turn translating OFF): turning it OFF always works, in every field (it used to refuse in a password box, an address
     * bar and with the setting off, because "not available in this box" was checked first); only turning it ON needs a field where translating is allowed.
     */
    private fun toggleTranslate() {
        if (flow.isOn()) { turnTranslateOff(); onTranslateMessage(context.getString(R.string.translate_off)); return }
        if (translateAllowed.not()) { onTranslateMessage(context.getString(R.string.translate_not_here)); return }
        flow.setOn(true, clock())
        view?.translateOn = true
        onTranslateMessage(context.getString(R.string.translate_on_hint))
    }

    /** Off: forget everything that was going on, cancel a request in flight (its answer is dropped) and show the key unlit. */
    private fun turnTranslateOff() {
        flow.setOn(false, clock())
        translator?.cancel()
        view?.translateOn = false
    }
    private var translateAllowed = true
    /** R34e: the box translating was switched on in (its app and its own facts), whether the keyboard has been off the screen since, and whether the person is told that translating switched itself off. */
    private var translateBox: String? = null
    private var viewLeft = false
    private var autoOffNotice = false
    /** Who a text box is: the app and what the box says about itself. Not its action key or flags (apps change those while typing); a new app, tab or field gives a new key. */
    private fun boxKeyOf(i: EditorInfo): String =
        "${i.packageName}|${i.fieldId}|${i.inputType and (android.text.InputType.TYPE_MASK_CLASS or android.text.InputType.TYPE_MASK_VARIATION)}|${i.fieldName}|${i.hintText}|${i.label}|${i.privateImeOptions}"

    /** Enter with translating on. True when the flow took the key (translating, or a message shown); false means "do the normal Enter". */
    private fun translateEnter(): Boolean {
        if (!flow.isOn() || !translateAllowed) return false        // (R24c: never translate in a password box or an address bar, whatever the toggle says: the text would go to the internet)
        val field = fieldText() ?: return false
        when (flow.enter(field, clock())) {
            app.kestrel.core.translate.TranslateFlow.Do.SEND -> { controller.enter(); flow.sent(); view?.translateOn = flow.isOn(); return true }
            app.kestrel.core.translate.TranslateFlow.Do.NOTHING -> return true
            app.kestrel.core.translate.TranslateFlow.Do.TOO_LONG -> { onTranslateMessage(flow.why()); return true }
            app.kestrel.core.translate.TranslateFlow.Do.TRANSLATE -> {
                val n = flow.start(field, clock())
                val t = translator ?: app.swoop.keyboard.translate.Translator(app.swoop.keyboard.translate.ConnectionLog).also { translator = it }
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
        set(v) { field = v; v?.listener = this; v?.translateOn = flow.isOn(); session.ui = this; prepareGlide() }

    // ------------------------------------------------------------------ lifecycle (the service forwards these)

    fun startInput(info: EditorInfo, restarting: Boolean = false) {
        val field = FieldInfos.from(info)
        // R34b: a focus that moves to the next box of the same window (a login page: e-mail box, then password box) never "finishes" the old box: its typed address is learned HERE, before the new box starts (it was lost before)
        /*RED-CHECK emailLearner.finishField(); saveEmailsIfChanged()*/
        // R34e (owner 2026-11-26: "if i have translation feature on, then switch apps or tabs, automatically turn translation off. It should only stay on if I'm actively using it"): translating belongs to the box it was switched on in
        val box = boxKeyOf(info)
        val sameBox = restarting && box == translateBox && !viewLeft
        val wasOn = flow.isOn()
        if (wasOn && !sameBox) turnTranslateOff()
        val offNotice = (wasOn && !sameBox) || autoOffNotice
        translateBox = box; viewLeft = false; autoOffNotice = false
        if (Diag.enabled) Diag.d("startInput kind=${field.kind} noLearn=${field.noPersonalizedLearning} noSuggest=${field.noSuggestions} caps=${field.capsFlags} action=${field.imeAction} pkgIsKestrel=${info.packageName?.startsWith("app.kestrel.browser") == true} omnibox=${FieldInfos.hasOmniboxOption(info.privateImeOptions)}")     // (debug builds: facts only, never text)
        session.settle(); learner.settle()
        val st = Settings(context)
        controller.autoCapsOn = st.autoCaps; controller.sentenceCaps = st.autoCaps          // (before the field starts: its first Shift state already depends on them)
        controller.startInput(field)
        glideOn = !safeMode && st.glide && field.kind != FieldKind.PASSWORD
        learnOn = !safeMode && st.learn && field.learningAllowed
        suggestOn = !safeMode && st.suggest && field.composingAllowed
        autoFixOn = suggestOn && st.autoFix && field.kind != FieldKind.OMNIBOX            // (never rewrite what is typed into an address box)
        capitalizeI = !safeMode && st.capitalizeI && field.composingAllowed && field.kind != FieldKind.OMNIBOX
        clipOn = !safeMode && st.suggestCopied; clipShown = null; glideNextOn = st.glideNextWord; session.reviseBefore = !safeMode && st.reviseBefore
        if (!safeMode) { glide.engine.shapeModel = st.shapeModel; if (st.shapeModel) glide.engine.warmShape() }            // R30 (safe mode never creates the engine)
        controller.capitalizeI = !safeMode && st.capitalizeI && field.kind != FieldKind.OMNIBOX          // (R20c: also in boxes that show no composing text, for glided and strip words, and at Enter)
        properOn = !safeMode && st.autoCaps && field.composingAllowed && field.kind != FieldKind.OMNIBOX
        doubleSpaceOn = !safeMode && st.doubleSpace
        emojiOn = !safeMode && st.emojiSuggestions && field.composingAllowed && field.kind != FieldKind.OMNIBOX
        if (emojiOn) loadEmoji()
        controller.autoSpace = !safeMode && st.autoSpace
        controller.caseForm = { w, prev -> if (properOn) glide.engine.caseFormOf(w, prev) else null }
        latest = null; literalShown = null; noFixFor = null; suggestSeq++
        userMoved = false; editWord = null; editRaw = emptyMap()
        emailOn = !safeMode && st.emailSuggestions; emailLearner.reset(); emailShown = emptyList(); emailLabels = emptyMap()
        flow.leaveField(); translator?.cancel(); translateAllowed = !safeMode && field.kind != FieldKind.PASSWORD && field.kind != FieldKind.OMNIBOX && st.translateEnabled
        if (safeMode || !st.translateEnabled) flow.setOn(false, clock())      // (R24c: the feature switched off in Settings is OFF, not just hidden)
        else flow.tick(clock())                                               // (the inactivity timer may have switched it off meanwhile)
        view?.let {
            it.reset()
            it.applyOptions(st)
            if (safeMode) it.showGlobe = true                // (the crash-loop safe layout always has the key that leaves this keyboard)
            it.translateOn = flow.isOn()                      // (R24c: the key always shows the real state, also after the keyboard view was rebuilt)
            it.setField(field.kind, ImeLayout.actionLabel(field.imeAction, field.noEnterAction), field.pad)
            it.setGlideEnabled(glideOn)
            it.setSuggestionsEnabled(suggestOn || (emailOn && field.kind == FieldKind.EMAIL))
            it.setShift(controller.shift)
        }
        prepareGlide()
        refreshSuggestions()
        if (offNotice && !safeMode) onTranslateMessage(context.getString(R.string.translate_auto_off))
    }

    fun finishInput() { translateLeft(); session.settle(); learner.settle(); suggestSeq++; emailLearner.finishField(); saveEmailsIfChanged(); controller.finishInput(); if (!safeMode) glide.engine.saveLearned() }

    /** The keyboard went off the screen (Back, another app or tab, the screen turned off, a rotation): R34e, translating that was on is over; the strip says so the next time the keyboard shows. */
    fun inputViewFinished() { translateLeft(); viewLeft = true }
    private fun translateLeft() { if (flow.isOn()) { turnTranslateOff(); autoOffNotice = true } }

    /** Writes what was learned (cheap when nothing changed). */
    fun saveLearned() { if (!safeMode) try { glide.engine.saveLearned() } catch (_: Exception) { } }

    fun onUpdateSelection(newSelStart: Int, newSelEnd: Int, candStart: Int, candEnd: Int) {
        val ours = controller.onSelectionUpdate(newSelStart, newSelEnd, candStart, candEnd)
        if (Diag.enabled) Diag.d("sel report new=$newSelStart/$newSelEnd cand=$candStart/$candEnd ours=$ours believed=${controller.selStart}/${controller.selEnd} composing=${controller.composing != null}")
        view?.setShift(controller.shift)
        if (!ours) { userMoved = true; if (controller.lastGlide == null) session.settle(); learner.settle(); refreshSuggestions(); if (flow.isOn()) { val t = fieldText(); if (t != null && flow.fieldChanged(t, clock())) translator?.cancel() } }       // the person or the app moved the cursor or changed the text: new word, new guesses
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

    /** A held Backspace now deletes a word per repeat (owner R23a): the same handling as one Backspace, with the word-sized delete. */
    private var deleteWordPending = false
    override fun onDeleteWord() { deleteWordPending = true; try { onKey(Key.Backspace) } finally { deleteWordPending = false } }

    override fun onKey(key: Key) {
        userMoved = false
        if (key is Key.Ch || key == Key.Backspace) dismissClip()
        session.beforeKey(key)
        val fix = autoFixFor(key)
        if (Diag.enabled && key is Key.Ch) traceKey(key, fix != null, "before")
        learner.beforeKey(key, fix)
        emailLearner.beforeKey(key)
        when (key) {
            is Key.Ch -> if (fix != null) controller.typeCorrected(fix, key.text) else if (key.text == " " && doubleSpaceOn && clock() - lastSpaceAt < DOUBLE_SPACE_MS && controller.doubleSpace()) { /* ". " is in */ } else controller.type(key.text)
            Key.Backspace -> if (deleteWordPending) controller.deleteWord() else controller.backspace()
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
        emailLearner.afterKey(); saveEmailsIfChanged()
        controller.revertedCorrection?.let { noFixFor = it.typed }
        view?.setShift(controller.shift)
        refreshSuggestions()
    }

    override fun onCursor(steps: Int) { session.settle(); learner.settle(); controller.moveCursor(steps); userMoved = true; view?.setShift(controller.shift); refreshSuggestions() }

    // ------------------------------------------------------------------ word suggestions while tapping (N4) and the automatic fix

    /** The word an automatic fix will put in when [key] ends the word being tapped: the engine's latest answer for exactly this word, only for a space or sentence punctuation, never for one just taken back. */
    private fun autoFixFor(key: Key): String? {
        if (key !is Key.Ch || key.text !in FIX_AT) return null
        val typed = controller.composing ?: return null
        if (typed == noFixFor) return null
        if (capitalizeI) { val i = capitalizedI(typed); if (i != null) return i }
        if (properOn) { val p = properNoun(typed); if (p != null) return p }
        if (!autoFixOn) return null
        val l = latest ?: return null
        return if (l.typed == typed) l.autoCorrect else null
    }

    /** R18b: "monday" -> "Monday", "sarah" -> "Sarah", "iphone" -> "iPhone": a word typed all in small letters that is written with a capital wherever it stands (the case book: the names the person writes with capitals, then the built-in list). */
    private fun properNoun(typed: String): String? {
        if (typed.length < 2 || typed != typed.lowercase()) return null
        val f = glide.engine.caseFormOf(typed, controller.contextBefore(typed).second) ?: return null
        return f.takeIf { it != typed && it.lowercase() == typed }
    }

    /** "i" -> "I", "i'm" -> "I'm", "i'll", "i've", "i'd": the one fix that needs no word list. */
    private fun capitalizedI(typed: String): String? = if (InputController.isPronounI(typed)) InputController.pronounI(typed) else null

    /** Asks the engine what to offer for the word at the cursor (or the likely next word); the answer is shown only if it still fits what is on screen. */
    private fun refreshSuggestions() {
        if (safeMode) return
        if (controller.lastGlide != null || view?.isGliding == true) return          // the strip shows a glide's other guesses
        if (emailBoxStrip()) return
        if (!suggestOn) { showClipOnly(); return }
        if (offerOtherWords()) return
        editWord = null
        val typed = controller.wordBeforeCursor()
        val b = controller.before
        val startsWord = typed == null && controller.selStart == controller.selEnd && (b.isEmpty() || b.last().isWhitespace())
        if (typed == null && !startsWord) { latest = null; literalShown = null; suggestSeq++; view?.clearCandidates(); return }
        val (p2, p1) = if (typed != null) controller.contextBefore(typed) else controller.previousTwo()
        val my = ++suggestSeq
        glide.engine.suggest(typed ?: "", p1, p2, 3) { res -> post(Runnable { if (my == suggestSeq) applySuggestions(res) }) }
    }

    // ------------------------------------------------------------------ R32c: the e-mail addresses in an e-mail box

    /**
     * An e-mail box shows the addresses the person types, best first: all of them while the box is empty, then only those that start with what was typed (and the domains seen behind an @). R34c (owner 2026-11-26): once "name@" is
     * typed, the strip offers the domains: the ones this person uses, then outlook.com, gmail.com, proton.me and the other popular ones, filtered by what is typed of the domain; one tap finishes the address. The same in an ordinary
     * text box as soon as an @ stands inside the word being typed (an address typed into a chat or a note). True when the strip was taken.
     * A password box or an app that asked for no personalised learning still gets the saved addresses (nothing is learned there).
     */
    private fun emailBoxStrip(): Boolean {
        val had = emailShown.isNotEmpty()
        val kind = controller.field.kind
        val here = emailOn && controller.selStart == controller.selEnd && when (kind) {
            FieldKind.EMAIL -> true
            FieldKind.TEXT, FieldKind.SEARCH -> controller.atSignNear()
            else -> false
        }
        if (!here) { emailShown = emptyList(); emailLabels = emptyMap(); if (had) view?.clearCandidates(); return false }
        val prefix = controller.addressPrefix()
        val list = glide.emails.suggest(prefix, 3)
        emailShown = list
        val at = prefix.indexOf('@')
        val labels = LinkedHashMap<String, String>()
        for (a in list) labels[if (at > 0 && a.length > at + 1 && a.regionMatches(0, prefix, 0, at + 1, ignoreCase = true)) a.substring(at + 1) else a] = a
        emailLabels = labels
        if (list.isEmpty()) { if (had) view?.clearCandidates(); return false }
        suggestSeq++                                       // (a word suggestion still on its way for what was typed before the @ must not replace the domains)
        clipShown = null
        view?.showCandidates(labels.keys.toList())
        return true
    }

    /** An address of the strip was tapped in an e-mail box: it takes the place of what was typed of it. */
    private fun pickEmail(address: String) {
        val n = controller.addressPrefix().length
        session.settle(); learner.settle()
        if (controller.pickAddress(n, address)) { emailLearner.picked(address); saveEmailsIfChanged() }
        emailShown = emptyList(); emailLabels = emptyMap()
        view?.setShift(controller.shift)
        refreshSuggestions()
    }

    // ------------------------------------------------------------------ R18a: tap a word that is already there, get other words for it

    /**
     * The cursor stands in (or was put at the edge of) a word that is already in the text, so the strip offers words in its place, the ones whose glide pattern is closest to it ([app.kestrel.core.input.glide.Alternatives]).
     * Typing the end of a word, or starting a new word in front of an old one, is not this: the strip then completes the word and guesses the next. True when this took the strip.
     */
    private fun offerOtherWords(): Boolean {
        val f = controller.field
        if (f.kind != FieldKind.TEXT && f.kind != FieldKind.SEARCH) return false
        val span = controller.wordAround() ?: return false
        val inside = span.left > 0 && span.right > 0
        if (!inside && !userMoved) return false
        val (p2, p1, next) = controller.contextAround(span)
        val my = ++suggestSeq
        glide.engine.alternatives(span.text, p2, p1, next, 3) { res -> post(Runnable { if (my == suggestSeq) applyOtherWords(span.text, res, p1) }) }
        return true
    }

    private fun applyOtherWords(word: String, words: List<String>, prev: String?) {
        if (!suggestOn || controller.lastGlide != null || view?.isGliding == true) return
        if (controller.wordAround()?.text != word) return                                // the text moved on while the engine worked
        latest = null; literalShown = null
        val shown = words.map { shapeLike(word, it, prev) }
        editWord = word; editRaw = shown.zip(words).toMap()
        if (shown.isEmpty()) view?.clearCandidates() else view?.showCandidates(shown)
    }

    /** [w] written like [old] was: ALL CAPITALS after capitals, a capital first letter after one, the way a name is written otherwise. */
    private fun shapeLike(old: String, w: String, prev: String?): String {
        if (old.length > 1 && old.any { it.isUpperCase() } && old.all { !it.isLetter() || it.isUpperCase() }) return w.uppercase()
        if (old.firstOrNull()?.isUpperCase() == true) return if (w == "i" || w.startsWith("i'")) "I" + w.substring(1) else w.replaceFirstChar { it.uppercase() }
        if (w == "i" || w.startsWith("i'")) return "I" + w.substring(1)
        return (if (properOn) glide.engine.caseFormOf(w, prev)?.takeIf { it.lowercase() == w } else null) ?: w
    }

    /** A word of the strip was tapped while it offered other words for the one under the cursor: it replaces that word where it stands (no space), is learned, and its own offers (the old word among them) stay on the strip. */
    private fun replaceTappedWord(shown: String) {
        val span = controller.wordAround()
        if (span == null || span.text != editWord) { editWord = null; refreshSuggestions(); return }
        val (p2, p1, _) = controller.contextAround(span)
        session.settle(); learner.settle()
        if (!controller.replaceWordAround(shown)) { refreshSuggestions(); return }
        if (!controller.wasPasted(span.text)) learner.replaced(span.text, shown, p2, p1)          // (R23b: a correction of text that came from the clipboard teaches nothing)
        editWord = null; userMoved = true; suggestSeq++
        view?.setShift(controller.shift)
        refreshSuggestions()
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
        if (res.typed.isEmpty() && controller.shift == app.kestrel.core.input.editor.ShiftState.ONCE) for (i in strip.indices) strip[i] = strip[i].replaceFirstChar { it.uppercase() }       // the next word starts a sentence: it will be written with its capital
        val clip = if (res.typed.isEmpty() && controller.wordBeforeCursor() == null) clipChipNow() else null
        clipShown = clip
        if (clip != null) { view?.showCandidates(listOf(clipLabel(clip)) + strip.take(2), accent = 0, clip = true); return }          // owner R20a: what was just copied comes first
        val shown = withEmoji(strip, res.typed.ifEmpty { null })
        if (shown.isEmpty()) view?.clearCandidates() else view?.showCandidates(shown)
    }

    // ------------------------------------------------------------------ R20a: the copied text as the first box

    /** The copy to offer now (fresh, not already pasted from the strip, not a password box), or null. */
    private fun clipChipNow(): ClipOffer? {
        if (!clipOn) return null
        val c = clipChipSource() ?: return null
        return if (c.at == clipUsedAt) null else c
    }
    /** One line for the box: the copy with its line breaks and runs of spaces squeezed (the view shortens it to fit). */
    private fun clipLabel(c: ClipOffer): String =
        if (c.sensitive || controller.field.kind == FieldKind.PASSWORD) "\u2022".repeat(minOf(c.text.length, 16))           // a secret (a password manager's copy, or a password box) is shown as dots, and still pastes
        else c.text.trim().replace(Regex("\\s+"), " ").take(200)

    /** Suggestions are off: the strip still offers the copy (and nothing else) where a word starts. */
    private fun showClipOnly() {
        val b = controller.before
        val atWordStart = controller.wordBeforeCursor() == null && controller.selStart == controller.selEnd && (b.isEmpty() || b.last().isWhitespace())
        val clip = if (atWordStart) clipChipNow() else null
        if (clip != null) { clipShown = clip; view?.showCandidates(listOf(clipLabel(clip)), accent = 0, clip = true) }
        else if (clipShown != null) { clipShown = null; view?.clearCandidates() }
    }

    /** The Android clipboard changed (the service noticed a copy): the strip offers it at once when a word starts here. */
    fun onClipboardChanged() { refreshSuggestions() }

    override fun onClipChip() {
        val c = clipShown ?: return
        clipUsedAt = c.at; clipShown = null
        pasteText(c.text)
    }

    override fun onClipChipLong() { dismissClip(); refreshSuggestions() }

    /** The copy that is on the strip is not offered again (typing, deleting, gliding, a long press): a NEW copy, even of the same text, brings it back. */
    private fun dismissClip() { clipShown?.let { clipUsedAt = it.at; clipShown = null } }

    /** R28b: the numbers behind the glide start (Diagnostics only, never what was typed). */
    override fun onTouchNote(kind: Int, a: Int, b: Int, c: Int) {
        val st = controller.stats
        when (kind) {
            KeysView.NOTE_STROKE -> { st.strokes++; st.strokePoints += a; st.strokeMs += b; st.firstMoveMs += c; if (c > st.firstMoveMax) st.firstMoveMax = c }
            KeysView.NOTE_TAKEOVER -> st.takeovers++
            KeysView.NOTE_RESTING_IGNORED -> st.restingIgnored++
            KeysView.NOTE_PADDING_START -> st.paddingStarts++
        }
    }

    override fun onGlideMove(xs: FloatArray, ys: FloatArray, count: Int) {
        if (!glideOn) return
        dismissClip()
        userMoved = false; editWord = null
        suggestSeq++; latest = null                                                        // a glide began: no tap suggestion may land on the strip now
        val (p2, p1) = session.context()
        glide.engine.preview(xs, ys, count, p1, p2) { res -> post(Runnable { if (view?.isGliding == true) session.onPreview(res) }) }
    }

    override fun onGlideEnd(xs: FloatArray, ys: FloatArray, count: Int) = onGlideEnd(xs, ys, FloatArray(0), count)

    override fun onGlideEnd(xs: FloatArray, ys: FloatArray, ts: FloatArray, count: Int) {
        if (!glideOn) return
        session.glideLifted()
        val (p2, p1) = session.context()
        glide.engine.decode(xs, ys, count, p1, p2, ts) { res: GlideResult -> post(Runnable { session.onDecoded(res, p1, p2); view?.setShift(controller.shift) }) }
    }

    override fun onCandidate(word: String) {
        if (word.isEmpty()) return
        emailLabels[word]?.let { if (emailShown.isNotEmpty()) { pickEmail(it); return } }
        if (word in glideNextShown && controller.lastGlide != null && !session.looksLikeFix(glideRaw[word] ?: word)) { pickNextAfterGlide(word); return }
        if (editWord != null && controller.lastGlide == null && editRaw.containsKey(word)) { replaceTappedWord(word); return }
        userMoved = false
        if (EmojiMap.isEmoji(word)) { chooseEmoji(word); return }
        if (controller.lastGlide != null || session.hasCandidates()) session.pick(glideRaw[word] ?: word)        // (the strip still shows a glide's guesses even when an app rewrote the field)
        else if (suggestOn) {
            // R32d: the keyboard lost track of the glide (an app or a late report), the strip shows the next words, and the person taps one of that glide's own guesses: it replaces the glided word
            if (session.replaceLookalike(glideRaw[word] ?: word)) { latest = null; literalShown = null; suggestSeq++; noFixFor = null; view?.setShift(controller.shift); refreshSuggestions(); return }
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

    /** What the strip shows for a glide's guesses -> the word the decoder gave (a name shows with its capital; tapping it picks the decoder's word). */
    private var glideRaw: Map<String, String> = emptyMap()
    /**
     * Owner R20b (2026-11-18): after a glide the strip is [other guess] [NEXT word] [other guess]: the sides replace the word the glide just put in (they are what the finger may have meant, by its pattern), the middle box
     * is the word that probably comes next (the person's own pairs and triples, then the shipped list's), and one tap on it puts it in. The decoder's best guess is already in the text, so it is not shown again. The middle box
     * is empty (nothing drawn, no touch) for the instant it takes the engine to answer, so nothing jumps; with no prediction at all the strip is the plain list of guesses, best first.
     */
    private var glideBest: String = ""
    private var glideAlts: List<String> = emptyList()
    private var glideMore: List<String> = emptyList()
    internal fun glideMoreForTest(): List<String> = glideMore
    private var glideNext: List<String> = emptyList()
    private var glideNextWaiting = false
    /** The next words the glide layout shows (the middle box, and a side box that has no other guess to hold): a tap on exactly one of them is "this word comes next", not a replacement of the glided word. */
    private var glideNextShown: Set<String> = emptySet()

    override fun showCandidates(words: List<String>) {
        val cased = if (properOn) words.map { w -> glide.engine.caseFormOf(w.lowercase(), controller.previousWord())?.takeIf { it.lowercase() == w.lowercase() } ?: w } else words
        val shown = if (controller.capitalizeI) cased.map { InputController.pronounI(it) } else cased                  // (R20c: the strip shows I'm, as the text will)
        // R32d: the decoder's 4th and 5th guesses are written the same way, so a tap on one maps back to the word itself
        val more = session.moreGuesses().let { m -> val c = if (properOn) m.map { w -> glide.engine.caseFormOf(w.lowercase(), controller.previousWord())?.takeIf { it.lowercase() == w.lowercase() } ?: w } else m; if (controller.capitalizeI) c.map { InputController.pronounI(it) } else c }
        val moreRaw = session.moreGuesses()
        glideRaw = if (properOn || controller.capitalizeI) (words.indices.filter { shown[it] != words[it] }.associate { shown[it] to words[it] } + moreRaw.indices.filter { more[it] != moreRaw[it] }.associate { more[it] to moreRaw[it] }) else emptyMap()
        glideNextShown = emptySet()
        if (view?.isGliding == true || controller.lastGlide == null || !suggestOn || !glideNextOn || shown.isEmpty()) { view?.showCandidates(withEmoji(shown, words.firstOrNull())); return }     // (the live guess while the finger is down, or no suggestions: the plain list)
        glideBest = shown[0]; glideAlts = shown.drop(1).take(2); glideMore = more.filter { m -> shown.none { it.equals(m, ignoreCase = true) } }; glideNext = emptyList(); glideNextWaiting = true
        renderGlideStrip()
        val (p2, p1) = controller.previousTwo()
        val my = ++suggestSeq
        glide.engine.suggest("", p1, p2, 4) { res -> post(Runnable { if (my == suggestSeq && controller.lastGlide != null) { glideNext = res.words; glideNextWaiting = false; renderGlideStrip() } }) }
    }

    /**
     * R32d (owner 2026-11-24: "when i press the word and in the recommendations to fix it, sometimes it keeps ave and just adds and behind it"): a box that REPLACES the glided word and a box that ADDS the next word must never
     * be mixed up. The middle box is the only one that adds, and it never holds a word the decoder itself guessed for this glide (any of its five): such a word is what the finger may have meant, so it goes to a side box
     * and a tap replaces. A word the language also expects next AND the shape fits is the best replacement there is, so it goes first. The side boxes never hold a filler "next word": an empty side box stays empty.
     */
    private fun renderGlideStrip() {
        val guesses = (listOf(glideBest) + glideAlts + glideMore).map { it.lowercase() }.toSet()
        // R33: a predicted next word that is spelled like the glided word and far likelier ("and" after the glide "ave") is what the finger probably meant: it stands among the replacements, never as "the next word"
        val fixes = glideNext.filter { it.lowercase() !in guesses && session.looksLikeFix(glideRaw[it] ?: it) }
        val agreeing = (fixes + glideMore.filter { m -> glideNext.any { it.equals(m, ignoreCase = true) } }).distinct()
        val sides = (agreeing + glideAlts + glideMore.filter { it !in agreeing }).distinct().take(2)
        val next = glideNext.filter { it.lowercase() !in guesses && it !in fixes }
        if (next.isEmpty() && !glideNextWaiting) { glideNextShown = emptySet(); view?.showCandidates(listOf(glideBest) + sides); return }          // no prediction: the plain guesses
        val mid = next.firstOrNull() ?: ""
        glideNextShown = if (mid.isEmpty()) emptySet() else setOf(mid)
        view?.showCandidates(listOf(sides.getOrElse(0) { "" }, mid, sides.getOrElse(1) { "" }), accent = 1)                                   // the middle box carries the accent outline: it is the one that adds a word
    }

    /** The middle box was tapped: the glided word stays (and is learned), the predicted word goes in after it, with its space, and counts double like any word taken from the strip. */
    private fun pickNextAfterGlide(word: String) {
        userMoved = false
        session.settle()
        latest = null; literalShown = null; suggestSeq++; glideNextShown = emptySet()
        glide.engine.stats.onStripAdded()
        learner.pick(word)
        noFixFor = null
        view?.setShift(controller.shift)
        refreshSuggestions()
    }
    override fun clearCandidates() { if (!suggestOn) view?.clearCandidates() }   // (with suggestions on, the next answer replaces the strip: no flicker)
    override fun showNoWord() { view?.showNoWord() }
    override fun showNotReady() { view?.showNotice(context.getString(R.string.strip_not_ready)) }

    private companion object {
        const val DOUBLE_TAP_MS = 350L
        const val MAX_FIELD = 6000
        /** A second space this soon after the first makes a full stop. */
        const val DOUBLE_SPACE_MS = 600L
        /** The keys after which an automatic fix may happen: the space and the punctuation that ends a sentence or a clause. */
        val FIX_AT = setOf(" ", ".", ",", "!", "?", ";", ":")
    }
}
