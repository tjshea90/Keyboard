package app.kestrel.core.clipboard

/** One entry of the clipboard history: the copied [text], when it was copied, and whether the person pinned it (pinned items never expire and are kept when the history is cleared). */
class ClipItem(val text: String, val at: Long, val pinned: Boolean)

/**
 * The keyboard's clipboard history (owner, 2026-11-17: "build a good clipboard into this app modeled after Gboard clipboard"). Like Gboard's:
 *  - everything copied while the keyboard is on screen is kept, newest first; copying the same text again moves it to the top;
 *  - an item the person PINS stays until they delete it; every other item disappears after [ttlMs] (an hour) and the list holds at most [maxItems] unpinned items;
 *  - "clear all" removes everything that is not pinned.
 * Pure Kotlin (the Android clipboard listener and the file are in :ime). The text is the person's own and is never logged: [toString] says only how many items there are.
 * Not thread safe: callers use it from one thread (the UI thread) and hand [serialize] to the writer.
 */
class ClipboardStore(private val now: () -> Long, val maxItems: Int = 30, val ttlMs: Long = 60L * 60L * 1000L) {
    private val list = ArrayList<ClipItem>()   // newest first

    /** Pinned items first (newest first), then the others (newest first). Expired ones are dropped before the list is returned. */
    val items: List<ClipItem> get() { expire(); return list.filter { it.pinned } + list.filter { !it.pinned } }
    val size: Int get() { expire(); return list.size }
    val isEmpty: Boolean get() = size == 0

    /** Adds [text] at the top. False (nothing changed) for blank text and for text longer than [MAX_CHARS]. A copy that is already in the list is moved to the top and keeps its pin. */
    fun add(text: String): Boolean {
        if (text.isBlank() || text.length > MAX_CHARS) return false
        expire()
        val i = list.indexOfFirst { it.text == text }
        val pinned = i >= 0 && list[i].pinned
        if (i == 0 && !pinned) { list[0] = ClipItem(text, now(), false); return true }
        if (i >= 0) list.removeAt(i)
        list.add(0, ClipItem(text, now(), pinned))
        trim()
        return true
    }

    fun pin(text: String, pinned: Boolean): Boolean {
        val i = list.indexOfFirst { it.text == text }
        if (i < 0) return false
        list[i] = ClipItem(text, list[i].at, pinned)
        if (!pinned) trim()
        return true
    }

    fun isPinned(text: String): Boolean = list.any { it.text == text && it.pinned }
    fun remove(text: String): Boolean = list.removeAll { it.text == text }
    fun clearUnpinned() { list.removeAll { !it.pinned } }
    fun clearAll() { list.clear() }

    private fun expire() { val t = now(); list.removeAll { !it.pinned && t - it.at > ttlMs } }
    private fun trim() { var unpinned = 0; val it = list.iterator(); while (it.hasNext()) { val c = it.next(); if (c.pinned) continue; unpinned++; if (unpinned > maxItems) it.remove() } }

    /** The history as text for the private file (one item per line, newest first). Parse it back with [load]. */
    fun serialize(): String {
        val sb = StringBuilder(HEADER).append('\n')
        for (c in list) sb.append(if (c.pinned) 'P' else 'N').append(' ').append(c.at).append(' ').append(escape(c.text)).append('\n')
        return sb.toString()
    }

    /** Replaces the history by the one in [text] (what [serialize] wrote). Anything unreadable is skipped; a file that is not ours leaves the history empty. */
    fun load(text: String?) {
        list.clear()
        if (text == null) return
        val lines = text.split('\n')
        if (lines.firstOrNull() != HEADER) return
        for (line in lines.drop(1)) {
            if (line.length < 5) continue
            val pinned = line[0] == 'P'
            if (line[0] != 'P' && line[0] != 'N') continue
            val sp = line.indexOf(' ', 2); if (sp < 0) continue
            val at = line.substring(2, sp).toLongOrNull() ?: continue
            val t = unescape(line.substring(sp + 1))
            if (t.isNotBlank() && t.length <= MAX_CHARS) list.add(ClipItem(t, at, pinned))
        }
        list.sortByDescending { it.at }
        expire(); trim()
    }

    override fun toString() = "ClipboardStore(${list.size} items)"

    companion object {
        const val HEADER = "SWOOP-CLIPBOARD 1"
        /** A copy longer than this is not kept (a whole document is not a clip). */
        const val MAX_CHARS = 20_000
        fun escape(s: String): String { val sb = StringBuilder(s.length + 8); for (c in s) when (c) { '\\' -> sb.append("\\\\"); '\n' -> sb.append("\\n"); '\r' -> sb.append("\\r"); else -> sb.append(c) }; return sb.toString() }
        fun unescape(s: String): String {
            val sb = StringBuilder(s.length); var i = 0
            while (i < s.length) { val c = s[i]; if (c == '\\' && i + 1 < s.length) { when (s[i + 1]) { 'n' -> sb.append('\n'); 'r' -> sb.append('\r'); else -> sb.append(s[i + 1]) }; i += 2 } else { sb.append(c); i++ } }
            return sb.toString()
        }
    }
}
