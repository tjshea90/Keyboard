package app.swoop.keyboard.translate

import app.kestrel.core.translate.ErrorCode
import app.kestrel.core.translate.Http
import app.kestrel.core.translate.TranslateLog
import app.kestrel.core.translate.Translation
import app.kestrel.core.translate.TranslationEngine
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** What a translation came to (see app.kestrel.core.translate.TranslateResult: ok, text, why, code, by, formal). */
typealias TranslateResult = app.kestrel.core.translate.TranslateResult

/**
 * Translates a text over the internet: the keyboard's only network entry point (plan 9.2). MyMemory first, Google when MyMemory is
 * used up, busy or out of reach, then the Mexican safety layer and the friendly-tu guard (all pure, in :core).
 *
 * One question at a time on one daemon thread that goes away when idle, never the UI thread. Every request has an increasing id:
 * [cancel] (or a newer [translate]) makes the older one stale; its answer is DROPPED (no callback) and its connection is aborted.
 * The text is never logged; the optional [TranslateLog] gets service, milliseconds and result only.
 *
 * Callers: create it only when the owner turned translate on; call [translate] only through the translate gate.
 */
class Translator internal constructor(private val http: Http, log: TranslateLog?) {
    /** The normal way to make one. [log] is the connection log (service, time, result; never the words). */
    constructor(log: TranslateLog? = null) : this(HttpsTransport(), log)

    private val engine = TranslationEngine(http, log)
    private val latest = AtomicLong(0)

    private val pool = ThreadPoolExecutor(1, 1, 20, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>()) { r ->
        Thread(r, "swoop-translate").also { it.isDaemon = true }
    }.also { it.allowCoreThreadTimeOut(true) }

    /** English to Mexican Spanish. [done] runs on a BACKGROUND thread, only for the newest request, only if it was not cancelled. */
    fun translate(text: String, done: (TranslateResult) -> Unit): Long = translate(text, Translation.Way.TO_MEXICAN, done)

    /** [way] TO_ENGLISH is for a Spanish message the owner chose to translate (plan 9.4). Returns the request id. */
    fun translate(text: String, way: Translation.Way, done: (TranslateResult) -> Unit): Long {
        val id = latest.incrementAndGet()
        http.abort() // an older request still on the wire is stale now
        pool.execute {
            if (latest.get() != id) return@execute
            val result: TranslateResult = try {
                engine.translate(text, way) { latest.get() != id }
            } catch (e: RuntimeException) { // an escape from this thread would end the app
                TranslateResult.failed("The text could not be translated.", false, "", ErrorCode.UNEXPECTED)
            }
            if (latest.get() != id) return@execute // cancelled while it ran: drop the answer
            try {
                done(result)
            } catch (e: RuntimeException) {
                // the caller's callback failed; never let it end the app, and never log (the result holds the words)
            }
        }
        return id
    }

    /** Field changed, toggle off, keyboard closed: forget the request in flight. Safe to call at any time. */
    fun cancel() {
        latest.incrementAndGet()
        http.abort()
    }
}
