package app.kestrel.core.translate

/** Why a translation did not work, as a code the UI can switch on (the sentence for the owner is [TranslateResult.why]). */
enum class ErrorCode {
    NONE,
    /** Nothing to translate (empty, or no letters in it). */
    NOTHING_TO_TRANSLATE,
    /** More than [Translation.MAX_CHARS] characters. */
    TOO_LONG,
    /** No route to the services: the phone's hotspot or mobile data is off. */
    NO_INTERNET,
    /** A service did not answer in time. */
    TIMEOUT,
    /** Used up for today, or asks this connection to wait (HTTP 429/503, MyMemory warning, Google's picture puzzle). */
    BUSY,
    /** A service answered with an error or a refusal, or could not be reached for a reason that fits no other code. */
    SERVICE_ERROR,
    /** The connection was made and then dropped (reset, aborted, closed): a flaky hotspot or a middlebox. */
    DROPPED,
    /** The connection was refused (ECONNREFUSED): usually Data Saver stopping a background app (reproduced on Android 10), else a firewall. */
    REFUSED,
    /** Android refused the socket (EPERM / EACCES): permission, data saver or a firewall app. */
    BLOCKED,
    /** The secure handshake failed: certificate, wrong date on the device, or a middlebox. */
    SECURE,
    /** An answer that could not be read, was empty or was too big. */
    BAD_ANSWER,
    /** The field changed while the text was being translated (set by [TranslateFlow]). */
    FIELD_CHANGED,
    /** The request was cancelled (field changed, toggle off): the caller normally never sees this. */
    CANCELLED,
    /** Anything else (a bug); the text is never lost. */
    UNEXPECTED
}

/** What a translation came to. */
class TranslateResult private constructor(
    val ok: Boolean,
    /** The translation (ok) or "". */
    val text: String,
    /** Why it did not work, a whole sentence ("" when ok). */
    val why: String,
    /** The service is used up for today or asks this connection to wait: the other service may still answer. */
    val busy: Boolean,
    /** Who translated it, for the screen ("MyMemory", "Google Translate"). */
    val by: String,
    val code: ErrorCode,
    /** Friendly-tu guard: the Spanish still reads formal (usted) after the second service was asked too. Counter material only. */
    val formal: Boolean = false
) {
    fun withFormal(f: Boolean): TranslateResult = TranslateResult(ok, text, why, busy, by, code, f)

    override fun toString(): String = if (ok) "ok(by=$by)" else "failed($code)" // never the words

    companion object {
        fun ok(text: String, by: String): TranslateResult = TranslateResult(true, text, "", false, by, ErrorCode.NONE)

        fun failed(why: String, busy: Boolean, by: String, code: ErrorCode): TranslateResult =
            TranslateResult(false, "", why, busy, by, code)
    }
}
