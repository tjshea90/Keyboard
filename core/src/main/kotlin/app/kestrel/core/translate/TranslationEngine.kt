package app.kestrel.core.translate

/** One line for the connection log: service, milliseconds, how it ended. Never the words. */
fun interface TranslateLog {
    fun add(service: String, millis: Long, result: String)
}

/**
 * The whole translation method of LinkMeter, pure apart from the [Http] it is given: MyMemory first, Google when MyMemory is used up,
 * busy or unreachable, then the Mexican safety layer on every translation into Spanish and the friendly-tu guard (plan 9.6).
 * Blocking: call it on a background thread only. [cancelled] is polled between pieces and services.
 */
class TranslationEngine(
    private val http: Http,
    private val log: TranslateLog? = null,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    fun translate(text: String, way: Translation.Way, cancelled: () -> Boolean = { false }): TranslateResult {
        if (text.trim().isEmpty() || !Translation.hasWords(text)) {
            return TranslateResult.failed("There is nothing to translate.", false, "", ErrorCode.NOTHING_TO_TRANSLATE)
        }
        if (text.length > Translation.MAX_CHARS) {
            return TranslateResult.failed("Too long to translate here. Shorten it or send it as it is.", false, "", ErrorCode.TOO_LONG)
        }
        var a = myMemory(text, way, cancelled)
        if (!a.ok && !cancelled()) {
            val g = google(text, way, cancelled)
            a = if (g.ok) g else both(a, g)
        } else if (a.ok && way == Translation.Way.TO_MEXICAN && FriendlyTu.looksFormal(text, a.text) && !cancelled()) {
            // friendly tu: the answer talks to the other person as "usted"; ask the other service once
            val g = google(text, way, cancelled)
            if (g.ok && !FriendlyTu.looksFormal(text, g.text)) a = g
        }
        if (cancelled()) return TranslateResult.failed("Cancelled.", false, "", ErrorCode.CANCELLED)
        if (a.ok && way == Translation.Way.TO_MEXICAN) {
            val fixed = Translation.toMexican(a.text)
            val formal = FriendlyTu.looksFormal(text, fixed)
            a = TranslateResult.ok(fixed, a.by).withFormal(formal)
        }
        return a
    }

    /** Neither service answered: the reason that helps most. */
    private fun both(mm: TranslateResult, g: TranslateResult): TranslateResult {
        if (g.why == mm.why) return g // the same for both: no internet, no time
        if (mm.busy && g.busy) {
            return TranslateResult.failed(
                "Both free translation services are busy or used up for today. Try again later, or send your English.", true, "", ErrorCode.BUSY
            )
        }
        val code = if (mm.code == ErrorCode.NO_INTERNET || g.code == ErrorCode.NO_INTERNET) ErrorCode.NO_INTERNET else g.code
        return TranslateResult.failed(mm.why + " " + g.why, mm.busy || g.busy, "", code)
    }

    private fun myMemory(text: String, way: Translation.Way, cancelled: () -> Boolean): TranslateResult {
        val out = StringBuilder()
        for (piece in Translation.pieces(text, Translation.MAX_PIECE_BYTES)) {
            if (cancelled()) return cancelledResult()
            if (!Translation.hasWords(piece)) {
                out.append(piece)
                continue
            }
            val t = Translation.trimmed(piece)
            val t0 = clock()
            val a = ask("MyMemory", Translation.MYMEMORY, t0) {
                val got = http.call(Translation.myMemoryUrl(t[1], way), null)
                got.code to Translation.parseMyMemory(got.code, got.body)
            }
            if (!a.ok) return a
            out.append(t[0]).append(a.text.trim()).append(t[2])
        }
        return TranslateResult.ok(out.toString(), Translation.MYMEMORY)
    }

    private fun google(text: String, way: Translation.Way, cancelled: () -> Boolean): TranslateResult {
        val asked = if (way == Translation.Way.TO_MEXICAN) Translation.expandShorthand(text) else text
        val out = StringBuilder()
        for (piece in Translation.pieces(asked, Translation.MAX_GOOGLE_BYTES)) {
            if (cancelled()) return cancelledResult()
            if (!Translation.hasWords(piece)) {
                out.append(piece)
                continue
            }
            val t = Translation.trimmed(piece)
            val t0 = clock()
            val a = ask("Google Translate", Translation.GOOGLE, t0) {
                val got = http.call(Translation.googleUrl(way), Translation.googleForm(t[1]))
                got.code to Translation.parseGoogle(got.code, got.location, got.body)
            }
            if (!a.ok) return a
            out.append(t[0]).append(a.text.trim()).append(t[2])
        }
        return TranslateResult.ok(out.toString(), Translation.GOOGLE)
    }

    private fun cancelledResult() = TranslateResult.failed("Cancelled.", false, "", ErrorCode.CANCELLED)

    private fun ask(service: String, by: String, t0: Long, call: () -> Pair<Int, TranslateResult>): TranslateResult {
        var code = -1
        var detail = ""
        val a: TranslateResult = try {
            val (c, r) = call()
            code = c
            r
        } catch (e: TransportException) {
            detail = e.kind.name + (if (e.message.isNullOrEmpty()) "" else ": " + e.message)
            failure(e, by)
        } catch (e: RuntimeException) { // an escape from the worker thread would end the app
            detail = "UNEXPECTED: " + SafeReason.of(e.javaClass.simpleName, null)
            TranslateResult.failed("The text could not be translated (" + e.javaClass.simpleName + ").", false, by, ErrorCode.UNEXPECTED)
        }
        val result = if (a.ok) "ok" else if (code >= 400) "answered $code" else if (a.busy) "busy or used up" else if (detail.isEmpty()) "failed" else "failed ($detail)"
        log?.add("translate ($service)", clock() - t0, result)
        return a
    }

    /** A failure to reach a service, in words: WHICH step broke and what to try. The reason is [SafeReason]-clean already (class name, errno, known phrase). */
    private fun failure(e: TransportException, by: String): TranslateResult {
        val m = e.message
        val why = if (m == null || m.isEmpty()) "" else " ($m)"
        return when (e.kind) {
            TransportException.Kind.NO_INTERNET -> TranslateResult.failed(
                "The tablet is not on the internet, so nothing could be translated. It gets the internet from the phone's hotspot: is the phone's mobile data on?",
                false, by, ErrorCode.NO_INTERNET
            )
            TransportException.Kind.TIMEOUT -> TranslateResult.failed("The translation service did not answer in time.", false, by, ErrorCode.TIMEOUT)
            TransportException.Kind.TOO_BIG -> TranslateResult.failed("The translation service sent too much.", false, by, ErrorCode.BAD_ANSWER)
            TransportException.Kind.DROPPED -> TranslateResult.failed(
                "The connection broke while translating$why. The hotspot may be flaky: try again. Diagnostics has a connection test.", false, by, ErrorCode.DROPPED
            )
            TransportException.Kind.REFUSED -> TranslateResult.failed(
                "The connection was refused$why. On Android that is usually Data Saver: Settings, Network and internet, Data Saver, Unrestricted data, switch Swoop on (Diagnostics has a button for it).",
                false, by, ErrorCode.REFUSED
            )
            TransportException.Kind.BLOCKED -> TranslateResult.failed(
                "Android is not letting the keyboard use the network$why. Restart the device once; if it stays, open Diagnostics and tap Test translation connection (a firewall, VPN or vendor setting may be blocking Swoop).",
                false, by, ErrorCode.BLOCKED
            )
            TransportException.Kind.SECURE -> TranslateResult.failed(
                "A secure connection to the translation service could not be set up$why. Check the date and time on the device.", false, by, ErrorCode.SECURE
            )
            TransportException.Kind.OTHER -> TranslateResult.failed(
                "The translation service could not be reached$why. Diagnostics has a connection test.", false, by, ErrorCode.SERVICE_ERROR
            )
        }
    }
}
