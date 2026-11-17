package app.kestrel.core.translate

/**
 * The pure state machine of "Enter translates, Enter again sends" (plan 9.3), ported from LinkMeter's TranslateFlow and extended
 * for a keyboard. No Android, no clock of its own: the caller passes `now` (milliseconds, any monotonic origin) where time matters.
 *
 * States: OFF -> (toggle) IDLE -> (Enter, English text) TRANSLATING -> (answer) TRANSLATED -> (Enter) send -> IDLE.
 * A failure leaves the English text exactly as it was, sets [lastError]/[why] and returns to IDLE: the owner's text is never lost.
 *
 * Typical use by the keyboard:
 *   when (flow.enter(fieldText, now)) {
 *       SEND -> perform the normal Enter / editor action, then flow.sent()
 *       TRANSLATE -> val n = flow.start(fieldText, now); translator.translate(fieldText) { r -> ui.post {
 *                        flow.answer(n, currentFieldText, r, now)?.let { replaceFieldWith(it) } ?: showError(flow.why) } }
 *       TOO_LONG, NOTHING -> show [why] (TOO_LONG) or do nothing
 *   }
 *   on every edit the keyboard did not make: if (flow.fieldChanged(text, now)) translator.cancel()
 *
 * @param autoOffMs turn the toggle off by itself after this much inactivity (0 = never), checked by [tick] and [enter].
 * @param editsEndCheck false (plan 9.3, the owner accepted it): an edited Spanish text is still sent by the next Enter. true: any edit
 *        after translating returns to IDLE (the next Enter would translate the edited text again).
 */
class TranslateFlow(var autoOffMs: Long = 0L, private val editsEndCheck: Boolean = false) {
    enum class State { OFF, IDLE, TRANSLATING, TRANSLATED }

    enum class Do {
        /** Nothing to do (a translation is on its way). */
        NOTHING,
        /** Perform the normal Enter / editor action (toggle off, empty or letterless text, or the Spanish was checked). Then call [sent]. */
        SEND,
        /** Translate the field text: call [start]. */
        TRANSLATE,
        /** The text is longer than [Translation.MAX_CHARS]: show [why], change nothing. */
        TOO_LONG
    }

    private var on = false
    private var english: String? = null
    private var spanish: String? = null
    private var asked: String? = null
    private var seq = 0
    private var lastActive = 0L
    private var by = ""
    private var why = ""
    private var error = ErrorCode.NONE

    val state: State
        get() = when {
            !on -> State.OFF
            asked != null -> State.TRANSLATING
            english != null -> State.TRANSLATED
            else -> State.IDLE
        }

    fun isOn(): Boolean = on

    /** The English while its Spanish waits in the field to be checked (Undo puts it back); null otherwise. */
    fun english(): String? = english

    /** Who translated it ("MyMemory", "Google Translate"); "" before anything was translated. */
    fun by(): String = by

    /** Why the last Enter did not translate, a whole sentence; "" when it did or none was asked for. */
    fun why(): String = why

    fun lastError(): ErrorCode = error

    /** The toolbar toggle. Turned off (or on again), everything that was going on is forgotten: a late answer changes nothing. */
    fun setOn(value: Boolean, now: Long = 0L) {
        on = value
        reset()
        seq++
        lastActive = now
    }

    /** Left the field or the app: forget the check and any request in flight; the toggle stays as it is. */
    fun leaveField() {
        reset()
        seq++
    }

    /** True when the inactivity timer ran out; the toggle is then switched off (the caller updates the button). */
    fun tick(now: Long): Boolean {
        if (on && autoOffMs > 0 && now - lastActive >= autoOffMs) {
            setOn(false, now)
            return true
        }
        return false
    }

    /** Enter was pressed with [field] in the field (the whole text, before and after the cursor). */
    fun enter(field: String, now: Long): Do {
        if (tick(now) || !on) return Do.SEND
        lastActive = now
        if (asked != null) return Do.NOTHING
        if (english != null) return Do.SEND // the Spanish was checked (also with a word the owner changed)
        if (field.trim().isEmpty() || !Translation.hasWords(field)) return Do.SEND
        if (field.length > Translation.MAX_CHARS) {
            why = "Too long to translate here. Shorten it or send it as it is."
            error = ErrorCode.TOO_LONG
            return Do.TOO_LONG
        }
        return Do.TRANSLATE
    }

    /** A translation of this English starts; the number to give back with its answer. */
    fun start(englishText: String, now: Long = 0L): Int {
        asked = englishText
        why = ""
        error = ErrorCode.NONE
        lastActive = now
        return ++seq
    }

    /**
     * The answer to request [n]. [field]: what the field holds now. Returns the text to put into the field (the Spanish, replacing all
     * of it), or null to leave the field alone: an older request, a failure ([why]/[lastError] say why) or a field changed meanwhile.
     */
    fun answer(n: Int, field: String, result: TranslateResult, now: Long = 0L): String? {
        val a = asked
        if (n != seq || a == null) return null
        asked = null
        lastActive = now
        if (!result.ok) {
            error = if (result.code == ErrorCode.NONE) ErrorCode.UNEXPECTED else result.code
            why = if (result.why.isEmpty()) "The text could not be translated." else result.why
            return null
        }
        if (field != a) {
            error = ErrorCode.FIELD_CHANGED
            why = "Your text changed while it was being translated. Press Enter to translate it again."
            return null
        }
        english = a
        spanish = result.text
        by = result.by
        why = ""
        error = ErrorCode.NONE
        return result.text
    }

    /** Undo: the English to put back into the field; null when nothing is being checked. */
    fun undo(): String? {
        val e = english
        reset()
        return e
    }

    /** "Send my English": what to send instead (the English while checking, else the field as it is). */
    fun sendEnglish(field: String): String {
        val e = english ?: field
        reset()
        return e
    }

    /** The action was performed. */
    fun sent() {
        reset()
    }

    /**
     * The field changed by something other than this flow (typing, deleting, the app). True when a request in flight is now stale and
     * should be cancelled ([Translator.cancel]). The Spanish the flow itself put in is not a change.
     */
    fun fieldChanged(field: String, now: Long = 0L): Boolean {
        lastActive = now
        if (!on) return false
        if (asked != null) {
            if (field == asked) return false
            asked = null
            seq++ // the answer, if it still comes, is dropped
            return true
        }
        if (english != null && field != spanish) {
            if (field.trim().isEmpty() || editsEndCheck) reset()
        }
        return false
    }

    private fun reset() {
        english = null
        spanish = null
        asked = null
        why = ""
        error = ErrorCode.NONE
    }
}
