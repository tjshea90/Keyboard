package app.kestrel.core.translate

/**
 * A SHORT, SAFE reason for a failed connection: the exception class name plus, only when the message holds one, a known errno name
 * (EPERM, ECONNRESET, ...) or a known harmless phrase ("connection reset", "certificate not trusted", ...). Everything else in a message
 * is dropped, because an exception message can hold the address and the address holds the text being translated. Pure: the transport
 * hands it the class name and the message.
 *
 * Why it exists: 2026-11-18 the owner saw "could not be reached (SocketException)" and the class name alone cannot tell a blocked
 * socket (EPERM) from a dropped connection (ECONNRESET) from a closed one: the next report must say which.
 */
object SafeReason {
    /** Errno names that can appear in a socket failure. An allowlist: nothing else is ever copied out of a message. */
    private val ERRNO = setOf(
        "EPERM", "EACCES", "ECONNRESET", "ECONNABORTED", "ECONNREFUSED", "EPIPE", "ETIMEDOUT", "ENETUNREACH", "EHOSTUNREACH", "ENETDOWN",
        "ENOTCONN", "EADDRNOTAVAIL", "EAFNOSUPPORT", "EAGAIN", "EINTR", "EIO", "EBADF", "ENOTSOCK", "ENOBUFS", "EMFILE", "ENFILE", "ENOMEM",
        "EPROTO", "EPROTONOSUPPORT", "ENETRESET", "EADDRINUSE"
    )

    /** Lower-case fragment of a message to the words that are kept for it (first match wins, so the specific ones come first). */
    private val PHRASES = listOf(
        "software caused connection abort" to "connection aborted",
        "connection reset" to "connection reset",
        "connection abort" to "connection aborted",
        "broken pipe" to "broken pipe",
        "socket closed" to "socket closed",
        "unexpected end of stream" to "cut off mid-answer",
        "trust anchor" to "certificate not trusted",
        "chain validation" to "certificate not trusted",
        "not yet valid" to "certificate date wrong (check the clock)",
        "notbefore" to "certificate date wrong (check the clock)",
        "expired" to "certificate date wrong (check the clock)",
        "certificate" to "certificate problem",
        "handshake" to "secure handshake failed",
        "network is unreachable" to "network unreachable",
        "connection refused" to "connection refused",
        "timed out" to "timed out",
        "operation not permitted" to "not permitted",
        "permission denied" to "permission denied"
    )

    private val WORD = Regex("[A-Z][A-Z0-9]{2,}")

    /** The errno names found in [message] (allowlisted), at most one. */
    fun errnoOf(message: String?): String? {
        if (message == null || message.isEmpty()) return null
        for (m in WORD.findAll(message)) if (m.value in ERRNO) return m.value
        return null
    }

    /** e.g. `SocketException (ECONNRESET, connection reset)`, `SSLHandshakeException (certificate not trusted)`, or just `SocketException`. */
    fun of(className: String, message: String?): String {
        val parts = ArrayList<String>(2)
        val errno = errnoOf(message)
        if (errno != null) parts.add(errno)
        if (message != null && message.isNotEmpty()) {
            val low = message.lowercase()
            for ((frag, words) in PHRASES) if (low.contains(frag)) { parts.add(words); break }
        }
        val name = cleanName(className)
        return if (parts.isEmpty()) name else name + " (" + parts.joinToString(", ") + ")"
    }

    private fun cleanName(n: String): String {
        val sb = StringBuilder()
        for (c in n) if (c.isLetterOrDigit() || c == '_') sb.append(c)
        return if (sb.isEmpty()) "Exception" else sb.toString().take(40)
    }
}
