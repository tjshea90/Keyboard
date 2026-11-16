package app.kestrel.core.input.editor

enum class ShiftState { OFF, ONCE, LOCK }

/**
 * What the last glide put into the editor: [text] is everything it inserted (a space before the word when one was needed, the word, a space after it),
 * [word] the word as shown (capitalised when shift was on). One Backspace removes all of [text]; a candidate from the strip replaces it.
 */
class GlideTail(val text: String, val word: String)

/**
 * The input state machine (plan 6.4), the replacement of MiniKeyboardModel for a SYSTEM keyboard. The app's editor owns the text; this keeps only
 * a short window of it (about 64 units before the cursor, 16 after), where the cursor is, the word being tapped (composing), the last glide and
 * the shift state. Pure Kotlin on [EditorBridge], so it is tested with a fake editor and a fuzzer instead of a device.
 *
 * Three rules keep it honest:
 *  1. PREDICT, THEN VALIDATE. After each own edit it predicts where the selection will be and remembers that in [pending]. Android reports every
 *     selection change back through onUpdateSelection; a report that matches a prediction is "ours". Anything else means the user or the app
 *     changed the text: composing, glide and anything else that assumed the old text is dropped and the window is re-read. (Updates arrive
 *     late, batched or merged, so several predictions may be outstanding and one report may confirm several.)
 *  2. VERIFY BEFORE REPLACING. Removing or replacing the last glide first reads the text before the cursor and refuses when it is not what was
 *     put there. A wrong belief therefore costs a missed shortcut, never someone else's text.
 *  3. NEVER ASSUME SUCCESS. Every editor call can fail; a failure re-reads the editor and forgets what was assumed.
 *
 * All offsets are UTF-16 units. Single-threaded: call from the keyboard's UI thread only.
 */
class InputController(private val ed: EditorBridge) {
    var field: FieldInfo = FieldInfo(); private set
    /** The text before the cursor (at most [WINDOW_BEFORE] units; the composing word, if any, is at its end). */
    var before = ""; private set
    /** The text after the cursor (at most [WINDOW_AFTER] units). */
    var after = ""; private set
    /** The selection as the editor reported it and as edits moved it; -1 = unknown. */
    var selStart = -1; private set
    var selEnd = -1; private set
    /** The word being tapped, shown to the app as composing text; null when none. */
    var composing: String? = null; private set
    var lastGlide: GlideTail? = null; private set
    var shift = ShiftState.OFF; private set

    private val pending = Pending()
    /** The editor may still hold a composing region this keyboard lost track of (a failed call, a new session): it is closed before the next edit. */
    private var composingUnknown = false

    // ------------------------------------------------------------------------------------------------------------ session

    /** A new field got the focus (onStartInput / onStartInputView). Forgets everything about the previous one. */
    fun startInput(info: FieldInfo) {
        field = info
        composing = null; lastGlide = null; shift = ShiftState.OFF; composingUnknown = true
        pending.clear()
        selStart = info.initialSelStart; selEnd = info.initialSelEnd
        refresh()
    }

    /** The field loses the focus (onFinishInput): the word being tapped stays in the text as typed. */
    fun finishInput() { endComposing(); lastGlide = null; pending.clear() }

    /**
     * onUpdateSelection. [candStart]/[candEnd] are the editor's composing region (-1 when none). True when the change was caused by this keyboard's
     * own edits, false when the user or the app moved or changed the text (everything assumed about the old text was dropped).
     */
    fun onSelectionUpdate(newSelStart: Int, newSelEnd: Int, candStart: Int = -1, candEnd: Int = -1): Boolean {
        if (candStart >= 0 && composing == null) composingUnknown = true           // the editor shows a composing word this keyboard does not know about
        if (pending.matchAndDrop(newSelStart, newSelEnd)) { selStart = newSelStart; selEnd = newSelEnd; return true }
        // the same selection as believed: usually only the composing region changed; but an app that replaced the text and left the cursor where it was looks the same, so with no own edit outstanding the window is checked
        if (newSelStart == selStart && newSelEnd == selEnd && (!pending.isEmpty() || windowStillTrue())) return true
        if (selStart < 0) { selStart = newSelStart; selEnd = newSelEnd; refresh(); return true } // the first report after an unknown state is the truth (and the text may have changed with it)
        // somebody else moved the cursor or edited the text
        if (composing != null) ed.finishComposing()
        composing = null; lastGlide = null
        pending.clear()
        selStart = newSelStart; selEnd = newSelEnd
        refresh()
        return false
    }

    // ------------------------------------------------------------------------------------------------------------ typing

    fun tapShift() { shift = if (shift == ShiftState.OFF) ShiftState.ONCE else ShiftState.OFF }
    fun lockShift() { shift = ShiftState.LOCK }
    fun setShift(s: ShiftState) { shift = s }

    /** A key was tapped whose label is [key]: a letter, a digit, punctuation, ".com". Letters follow the shift state. */
    fun type(key: String) {
        if (key.isEmpty()) return
        settle()
        val t = if (shift != ShiftState.OFF && isSingleCodePoint(key)) key.uppercase() else key
        if (isLetter(t) || (composing != null && isWordConnector(t))) {
            if (field.composingAllowed) { typeComposing(t); return }
        }
        endComposing()
        val g = lastGlide
        // right after a glide, ".", ",", "/", ".com" ... close the word instead of standing after the space the glide added ("hello" ".com" = "hello.com")
        if (g != null && key in CLOSES_WORD && g.text.endsWith(" ") && before.endsWith(" ") && selStart == selEnd) {
            ed.beginBatch()
            val ok = ed.deleteSurrounding(1, 0)
            if (ok) { before = before.dropLast(1); moveSel(-1) }
            commit(t)
            ed.endBatch()
            if (!ok) resync()
        } else commit(t)
        lastGlide = null
        if (shift == ShiftState.ONCE) shift = ShiftState.OFF
        ensureWindow()
    }

    fun space() { type(" ") }

    /** One press of Backspace. A tapped word loses its last letter; the word a glide just added goes whole; otherwise the editor deletes (selection or one character). */
    fun backspace() {
        settle()
        val c = composing
        if (c != null) {
            val shorter = dropLastCodePoint(c)
            val removed = c.length - shorter.length
            if (shorter.isEmpty()) { val ok = ed.setComposingText("") ; ed.finishComposing(); composing = null; if (!ok) { resync(); return } }
            else { if (!ed.setComposingText(shorter)) { resync(); return }; composing = shorter }
            before = before.dropLast(removed)
            moveSel(-removed)
            lastGlide = null
            ensureWindow()
            return
        }
        val g = lastGlide
        if (g != null && selStart == selEnd && verifyBefore(g.text)) {
            ed.beginBatch()
            val ok = ed.deleteSurrounding(g.text.length, 0)
            ed.endBatch()
            lastGlide = null
            if (!ok) { resync(); return }
            before = before.dropLast(g.text.length)
            moveSel(-g.text.length)
            ensureWindow()
            return
        }
        lastGlide = null
        if (selStart != selEnd) {                       // a selection: the editor deletes it, the cursor ends where the selection began
            if (!ed.sendKey(KeyCodes.DEL)) { resync(); return }
            selEnd = selStart; pending.push(selStart, selEnd)
        } else {
            val n = lastCodePointLength(before)
            if (!ed.sendKey(KeyCodes.DEL)) { resync(); return }
            before = before.dropLast(n)
            moveSel(-n)
        }
        ensureWindow()
    }

    /** Enter: the field's own action when it has one, else a real Enter key. The text after it is unknown, so the window is re-read. */
    fun enter() {
        settle()
        endComposing(); lastGlide = null
        val a = field.imeAction
        val useAction = !field.noEnterAction && a != ImeActions.UNSPECIFIED && a != ImeActions.NONE
        val ok = if (useAction) ed.performAction(a) else ed.sendKey(KeyCodes.ENTER)
        if (!ok) { resync(); return }
        if (shift == ShiftState.ONCE) shift = ShiftState.OFF
        refresh(); selStart = -1; selEnd = -1; pending.clear()   // the next selection report is the truth
    }

    /** A glide ended on [word] (already ranked by the decoder). It is inserted at the cursor with a space after it, and one before it when the text does not end in a space. */
    fun commitGlide(word: String) {
        if (word.isEmpty()) return
        settle()
        endComposing()
        val w = when (shift) { ShiftState.LOCK -> word.uppercase(); ShiftState.ONCE -> word.replaceFirstChar { it.uppercase() }; ShiftState.OFF -> word }
        val needsLead = before.isNotEmpty() && !before.last().isWhitespace() && before.last() !in NO_SPACE_AFTER
        val tail = (if (needsLead) " " else "") + w + " "
        if (!commit(tail)) return
        lastGlide = GlideTail(tail, w)
        if (shift == ShiftState.ONCE) shift = ShiftState.OFF
        ensureWindow()
    }

    /** A candidate from the strip replaces the word the last glide put in. False (nothing changed) when the last thing was not a glide or the text is no longer there. */
    fun swapGlide(word: String): Boolean {
        val g = lastGlide ?: return false
        if (selStart != selEnd || !verifyBefore(g.text)) { lastGlide = null; return false }
        val lead = if (g.text.startsWith(" ")) " " else ""
        val shown = g.word
        val w = if (shown.length > 1 && shown.all { it.isUpperCase() }) word.uppercase() else if (shown.firstOrNull()?.isUpperCase() == true) word.replaceFirstChar { it.uppercase() } else word
        val newTail = lead + w + " "
        ed.beginBatch()
        val deleted = ed.deleteSurrounding(g.text.length, 0)
        var ok = deleted
        if (deleted) {
            before = before.dropLast(g.text.length); moveSel(-g.text.length)
            ok = commit(newTail)
        }
        ed.endBatch()
        if (!ok) { lastGlide = null; resync(); return false }
        lastGlide = GlideTail(newTail, w)
        ensureWindow()
        return true
    }

    /** Moves the cursor [delta] characters (negative = left), e.g. from sliding a finger over the space key. A tapped word is finished first. */
    fun moveCursor(delta: Int) {
        if (delta == 0) return
        settle()
        endComposing(); lastGlide = null
        val left = delta < 0
        val steps = minOf(Math.abs(delta), 64)
        var predicted = selStart >= 0 && selStart == selEnd   // a selection collapses to one end and the selected text joins the window: re-read instead of predicting
        for (i in 0 until steps) {
            if (!ed.sendKey(if (left) KeyCodes.DPAD_LEFT else KeyCodes.DPAD_RIGHT)) { resync(); return }
            if (!predicted) continue
            if (left) {
                if (before.isEmpty()) { if (selStart != 0) predicted = false; continue }
                val n = lastCodePointLength(before)
                after = (before.takeLast(n) + after).take(WINDOW_AFTER); before = before.dropLast(n); if (selStart >= 0) selStart -= n
            } else {
                if (after.isEmpty()) { predicted = false; continue }       // we cannot tell whether the field ends here
                val n = firstCodePointLength(after)
                before = (before + after.take(n)).takeLast(WINDOW_BEFORE); after = after.drop(n); if (selStart >= 0) selStart += n
            }
        }
        if (selStart >= 0 && predicted) { selEnd = selStart; pending.push(selStart, selEnd); ensureWindow() }
        else { selStart = -1; selEnd = -1; pending.clear(); refresh() }
    }

    /** The word before the cursor in the window, lowercase, for the language (null when there is none or it is not a plain word). */
    fun previousWord(): String? {
        val tok = before.trimEnd().substringAfterLast(' ')
        if (tok.isEmpty() || tok.length > 24 || !tok.all { it.isLetter() || it == '\'' }) return null
        return tok.lowercase()
    }

    // ------------------------------------------------------------------------------------------------------------ internals

    private fun typeComposing(t: String) {
        val old = composing ?: ""
        val newC = old + t
        if (!ed.setComposingText(newC)) { resync(); return }
        composing = newC
        before = (before + t).takeLast(WINDOW_BEFORE)
        moveSel(t.length)
        lastGlide = null
        if (shift == ShiftState.ONCE) shift = ShiftState.OFF
    }

    private fun endComposing() {
        if (composing != null) { ed.finishComposing(); composing = null }
    }

    /** Inserts [text] at the cursor (replacing a selection) and updates what is believed. */
    private fun commit(text: String): Boolean {
        if (!ed.commitText(text)) { resync(); return false }
        before = (before + text).takeLast(WINDOW_BEFORE)
        if (selStart >= 0) { selStart += text.length; selEnd = selStart; pending.push(selStart, selEnd) }
        return true
    }

    /** The cursor moved by [delta] units because of an own edit. */
    private fun moveSel(delta: Int) {
        if (selStart < 0) return
        selStart = maxOf(0, selStart + delta); selEnd = selStart
        pending.push(selStart, selEnd)
    }

    /** A cheap look at the editor: the few units around the cursor are still what the window says. */
    private fun windowStillTrue(): Boolean {
        val b = before.takeLast(16); val a = after.take(8)
        return ed.textBefore(b.length) == b && ed.textAfter(a.length) == a
    }

    private fun verifyBefore(tail: String): Boolean = ed.textBefore(tail.length) == tail && before.endsWith(tail)

    private fun refresh() { before = ed.textBefore(WINDOW_BEFORE) ?: ""; after = ed.textAfter(WINDOW_AFTER) ?: "" }

    /** Something failed or surprised us: forget every assumption and re-read the editor. */
    private fun resync() {
        composing = null; lastGlide = null; pending.clear(); composingUnknown = true
        selStart = -1; selEnd = -1
        refresh()
    }

    /** Called before every edit: closes a composing region the keyboard lost track of, so an edit can never replace somebody's composing word by accident. */
    private fun settle() {
        if (composingUnknown && composing == null && ed.finishComposing()) composingUnknown = false
    }

    /** Edits shorten the window from the front; when text exists before it, read it again. */
    private fun ensureWindow() {
        if (before.length < MIN_BEFORE && (selStart < 0 || selStart > before.length)) before = ed.textBefore(WINDOW_BEFORE) ?: before
    }

    private fun isLetter(s: String): Boolean = isSingleCodePoint(s) && Character.isLetter(s.codePointAt(0))
    private fun isWordConnector(s: String): Boolean = s == "'" || (isSingleCodePoint(s) && Character.isDigit(s.codePointAt(0)))
    private fun isSingleCodePoint(s: String) = s.isNotEmpty() && s.codePointCount(0, s.length) == 1

    /** The selections this keyboard expects the editor to report back for its own edits, oldest first (a small ring: reports can arrive late or merged). */
    private class Pending {
        private val a = IntArray(CAP); private val b = IntArray(CAP)
        private var head = 0; private var size = 0
        fun clear() { size = 0 }
        fun isEmpty() = size == 0
        fun push(s: Int, e: Int) {
            if (size == CAP) { head = (head + 1) % CAP; size-- }
            val i = (head + size) % CAP; a[i] = s; b[i] = e; size++
        }
        /** True when (s, e) is one of the expected selections; that one and every older one are forgotten. */
        fun matchAndDrop(s: Int, e: Int): Boolean {
            for (k in size - 1 downTo 0) {
                val i = (head + k) % CAP
                if (a[i] == s && b[i] == e) { head = (head + k + 1) % CAP; size -= k + 1; return true }
            }
            return false
        }
        companion object { const val CAP = 24 }
    }

    companion object {
        const val WINDOW_BEFORE = 64
        const val WINDOW_AFTER = 16
        const val MIN_BEFORE = 16
        /** Keys that join the word before them: right after a glide they take away the space the glide added. */
        val CLOSES_WORD: Set<String> = setOf(".", ",", "?", "!", ":", ";", "/", ".com", ")", "'", "-", "_", "@", "%")
        /** A glide right after one of these starts the word without a space before it. */
        const val NO_SPACE_AFTER = "([{“‘/@#"

        fun dropLastCodePoint(s: String): String = if (s.isEmpty()) s else s.dropLast(lastCodePointLength(s))
        fun lastCodePointLength(s: String): Int =
            if (s.length >= 2 && Character.isLowSurrogate(s[s.length - 1]) && Character.isHighSurrogate(s[s.length - 2])) 2 else if (s.isEmpty()) 0 else 1
        fun firstCodePointLength(s: String): Int =
            if (s.length >= 2 && Character.isHighSurrogate(s[0]) && Character.isLowSurrogate(s[1])) 2 else if (s.isEmpty()) 0 else 1
    }
}
