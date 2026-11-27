package app.kestrel.core.input.glide

/**
 * R32c (owner 2026-11-24: "Learn the email addresses i use and include them as suggested words at the top of the keyboard whenever i press an email field"). The e-mail addresses this person TYPES, kept on the device
 * only (never exported, never logged), best first: the ones used most, then the newest. At most [cap]; the one used least goes first when the book is full. Pure Kotlin, the methods are synchronized (the keyboard's UI thread reads it, the settings screen may clear it).
 *
 * What gets in is decided by the callers (only an address typed key by key, never from the clipboard, an autofill or a paste; nothing in password boxes or where the app asked for no personalised learning).
 * Addresses are kept in lower case (domains are case-blind and nobody relies on a capital in the part before the @).
 */
class EmailBook(private val cap: Int = CAP) {
    private class Entry(val address: String, var uses: Int, var last: Long)
    private val items = LinkedHashMap<String, Entry>()
    /** A logical clock: every use is "later" than the one before (no wall clock, so the order is the same in tests and on the phone). */
    private var clock = 0L
    /** Set whenever the book changed since the last [serialize]. */
    @Volatile var dirty = false; private set

    val size: Int @Synchronized get() = items.size
    val isEmpty: Boolean @Synchronized get() = items.isEmpty()

    /** One more use of [address] (a typed or picked address). False (nothing stored) when it is not a plain address. */
    @Synchronized fun learn(address: String, uses: Int = 1): Boolean {
        val a = address.trim().lowercase()
        if (!isAddress(a)) return false
        val e = items[a]
        if (e != null) { e.uses += uses; e.last = ++clock }
        else {
            if (items.size >= cap) { val worst = items.values.minWithOrNull(compareBy<Entry> { it.uses }.thenBy { it.last }); if (worst != null) items.remove(worst.address) }
            items[a] = Entry(a, uses, ++clock)
        }
        dirty = true
        return true
    }

    /** The person removed [address] (Settings): it is gone and will be learned again only if it is typed again. */
    @Synchronized fun forget(address: String): Boolean { val r = items.remove(address.trim().lowercase()) != null; if (r) dirty = true; return r }

    /** Every address is gone. */
    @Synchronized fun clear() { if (items.isNotEmpty()) dirty = true; items.clear() }

    /** Every address, best first. */
    @Synchronized fun addresses(): List<String> = ranked().map { it.address }

    private fun ranked(): List<Entry> = items.values.sortedWith(compareByDescending<Entry> { it.uses }.thenByDescending { it.last })

    /**
     * What to offer for the text [typed] so far in an e-mail box (up to [max], best first): the addresses that start with it (all of them when nothing is typed yet); and once an "@" is there, the part before it with the
     * domains behind it ("bob@" gives bob@outlook.com, bob@gmail.com, bob@proton.me ...): the domains this person uses first (most used first), then the popular ones of [POPULAR_DOMAINS] (R34c, owner 2026-11-26: "after the @ sign, suggest
     * popular email domains"), filtered by what is typed of the domain. The part before the @ keeps the capitals it was typed with. An address that is already typed in full is not offered again.
     */
    @Synchronized fun suggest(typed: String, max: Int = 3): List<String> {
        val raw = typed.trim()
        val t = raw.lowercase()
        if (t.length > MAX_LEN) return emptyList()
        val out = ArrayList<String>(max)
        for (e in ranked()) { if (out.size >= max) break; if (e.address != t && e.address.startsWith(t)) out.add(e.address) }
        val at = t.indexOf('@')
        if (out.size < max && at > 0 && t.indexOf('@', at + 1) < 0) {
            val local = raw.substring(0, at); val domPart = t.substring(at + 1)
            if (LOCAL.matches(local.lowercase())) {
                for (d in domains()) {
                    if (out.size >= max) break
                    if (!d.startsWith(domPart)) continue
                    val cand = "$local@$d"
                    if (cand.lowercase() != t && out.none { it.equals(cand, ignoreCase = true) }) out.add(cand)
                }
            }
        }
        return out
    }

    /** The domains to offer after an @: the ones this person's addresses use (by how often), then the popular ones not yet in the list. */
    private fun domains(): List<String> {
        val own = LinkedHashMap<String, Int>()
        for (e in ranked()) { val d = e.address.substringAfter('@'); own[d] = (own[d] ?: 0) + e.uses }
        val out = ArrayList<String>(own.size + POPULAR_DOMAINS.size)
        for ((d, _) in own.entries.sortedByDescending { it.value }) out.add(d)
        for (d in POPULAR_DOMAINS) if (d !in own) out.add(d)
        return out
    }

    /** One line per address: the address, how often it was used, when (the logical clock), tab separated. */
    @Synchronized fun serialize(): String { dirty = false; return ranked().joinToString("\n") { "${it.address}\t${it.uses}\t${it.last}" } }

    companion object {
        /** The mail services most people use, in the order they are offered after an @ when the person has not used them yet (the owner asked for outlook.com, gmail.com and proton.me first). */
        val POPULAR_DOMAINS: List<String> = listOf("outlook.com", "gmail.com", "proton.me", "hotmail.com", "yahoo.com", "icloud.com", "protonmail.com", "live.com", "aol.com", "msn.com")
        const val CAP = 40
        /** An address longer than this is not kept (the keyboard only reads 64 characters before the cursor). */
        const val MAX_LEN = 60
        private val LOCAL = Regex("[a-z0-9._%+-]{1,40}")
        private val ADDRESS = Regex("[a-z0-9._%+-]{1,40}@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,24}")

        /** [s] (already lower case) is a plain address: letters, digits and ._%+- before the @, a dotted domain with a top level of 2 to 24 letters after it, no dot at either end of the part before the @ and no two dots in a row. */
        fun isAddress(s: String): Boolean {
            if (s.length < 6 || s.length > MAX_LEN) return false
            if (!ADDRESS.matches(s)) return false
            val local = s.substringBefore('@')
            if (local.startsWith('.') || local.endsWith('.') || s.contains("..")) return false
            return true
        }

        /** The book saved by [serialize]; lines that make no sense are dropped, a damaged text gives an empty or shorter book. */
        fun parse(text: String?, cap: Int = CAP): EmailBook {
            val b = EmailBook(cap)
            if (text.isNullOrBlank()) return b
            val rows = ArrayList<Entry>()
            for (line in text.lineSequence()) {
                val p = line.split('\t')
                val a = p.getOrNull(0)?.trim()?.lowercase() ?: continue
                if (!isAddress(a)) continue
                val uses = p.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 1_000_000) ?: 1
                val last = p.getOrNull(2)?.toLongOrNull() ?: 0L
                rows.add(Entry(a, uses, last))
            }
            for (e in rows.sortedBy { it.last }.takeLast(cap)) { b.items[e.address] = e; if (e.last > b.clock) b.clock = e.last }
            b.dirty = false
            return b
        }
    }
}
