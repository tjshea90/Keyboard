package app.swoop.keyboard.translate

import app.kestrel.core.translate.Translation

/**
 * The host allowlist (plan 9.2 rule 3). The keyboard may talk to exactly two hosts, over HTTPS, on the default port, with no user
 * info in the address. [HttpsTransport] checks every address with [allows] BEFORE a connection is opened, so a bug that builds another
 * address fails closed instead of sending anything.
 */
object TranslateGuard {
    val HOSTS: Set<String> = setOf(Translation.HOST_MYMEMORY, Translation.HOST_GOOGLE)

    private const val PREFIX = "https://"

    /** The host of an https address, or null when the address is not a plain https address. */
    fun hostOf(url: String): String? {
        if (!url.startsWith(PREFIX, ignoreCase = false)) return null
        val rest = url.substring(PREFIX.length)
        var end = rest.length
        for (i in rest.indices) {
            val c = rest[i]
            if (c == '/' || c == '?' || c == '#') { end = i; break }
        }
        val host = rest.substring(0, end)
        if (host.isEmpty() || host.indexOf('@') >= 0 || host.indexOf(':') >= 0 || host.indexOf('\\') >= 0) return null
        return host
    }

    fun allows(url: String): Boolean {
        val h = hostOf(url) ?: return false
        return HOSTS.contains(h)
    }
}
