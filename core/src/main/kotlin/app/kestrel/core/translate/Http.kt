package app.kestrel.core.translate

/** What one HTTP exchange gave back. [location] is the Location header of a redirect (never followed), or null. */
class HttpReply(val code: Int, val location: String?, val body: String)

/** Why a transport could not get an answer at all. */
class TransportException(val kind: Kind, message: String? = null) : Exception(message) {
    enum class Kind {
        NO_INTERNET, TIMEOUT, TOO_BIG,
        /** The connection was made and then dropped (reset, aborted, closed, cut off): a flaky hotspot. One quick retry is worth it. */
        DROPPED,
        /** Android refused the socket itself (EPERM / EACCES): the keyboard may not use the network (permission, data saver, firewall). */
        BLOCKED,
        /** The secure handshake failed (certificate, wrong clock, middlebox). */
        SECURE,
        OTHER
    }
}

/**
 * The one door to the network, implemented by app.swoop.keyboard.translate.HttpsTransport (the ONLY code that opens a socket) and by
 * fakes in tests. [form] null means GET, else POST with this application/x-www-form-urlencoded body.
 */
interface Http {
    @Throws(TransportException::class)
    fun call(url: String, form: String?): HttpReply

    /** Abort a call that is in flight (cancel). Fakes may ignore it. */
    fun abort() {}
}
