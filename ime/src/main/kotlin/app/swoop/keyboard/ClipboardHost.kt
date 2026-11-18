package app.swoop.keyboard

import android.content.ClipboardManager
import android.content.Context
import app.kestrel.core.clipboard.ClipItem
import app.kestrel.core.clipboard.ClipboardStore
import java.io.File

/**
 * Where the clipboard history lives: the [ClipboardStore], its private file, and reading what Android's clipboard holds. All on the UI thread except the file write (the Io thread).
 * The history never leaves the phone and is never logged. Android lets the active keyboard read the clipboard; a copy marked sensitive (a password manager sets that) is skipped.
 */
class ClipboardHost(private val context: Context, private val now: () -> Long = { System.currentTimeMillis() }) {
    val store = ClipboardStore(now)
    private val file = File(context.filesDir, "clipboard.txt")
    private var loaded = false

    /** Reads the saved history once (a few kilobytes at most). */
    fun ensureLoaded() { if (loaded) return; loaded = true; try { store.load(if (file.isFile) file.readText() else null) } catch (_: Exception) { } }

    /** Writes the history in the background. */
    fun save() {
        val text = store.serialize()
        Io.exec {
            try {
                val tmp = File(file.parentFile, file.name + ".tmp"); tmp.writeText(text)
                if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
            } catch (_: Exception) { }
        }
    }

    /**
     * The newest thing copied (owner R20a: the first box of the strip offers it), kept in memory only, whether or not the history is on; null before anything was copied or when the copy was marked sensitive. [ClipItem.at] is when
     * the keyboard noticed the copy (a copy of the same text again is a new copy only when [fromCopy] says it was a real copy event).
     */
    var latest: ClipItem? = null; private set
    /** The newest copy was marked sensitive (a password manager sets that): it may be offered masked and pasted, but it is never put in the history. */
    var latestSensitive = false; private set

    /** The newest copy if it is no older than [ttlMs], with whether it is sensitive; null when there is none or it is too old. */
    fun latestFresh(ttlMs: Long): ClipOffer? { val c = latest ?: return null; return if (now() - c.at <= ttlMs) ClipOffer(c.text, c.at, latestSensitive) else null }

    /**
     * Takes what the Android clipboard holds now: the newest copy ([latest]) always, the history only when [history] is on and the copy is not sensitive. The copy's time is the clip's own timestamp (API 26), so a copy made
     * while the keyboard was not running keeps its real age and the same text copied again is a new copy. True when the history list changed.
     */
    fun capture(history: Boolean = true): Boolean {
        ensureLoaded()
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip ?: return false
            if (clip.itemCount == 0) return false
            val sensitive = try { clip.description.extras?.getBoolean(SENSITIVE_EXTRA, false) == true } catch (_: Exception) { false }
            val text = clip.getItemAt(0).coerceToText(context)?.toString() ?: return false
            if (text.isBlank()) return false
            val stamp = try { clip.description.timestamp } catch (_: Exception) { 0L }
            latest = ClipItem(text, if (stamp > 0L) minOf(stamp, now()) else now(), false); latestSensitive = sensitive
            if (sensitive || !history) return false                                          // a password manager's copy is never kept in the history
            val changed = store.add(text)
            if (changed) save()
            changed
        } catch (_: Exception) { false }          // some Android versions refuse the read when no window has the focus: the history simply does not grow
    }

    companion object { const val SENSITIVE_EXTRA = "android.content.extra.IS_SENSITIVE" }
}

/** What the strip may offer as its first box (owner R20a): the copy, when it was made (the clip's own timestamp), and whether it is sensitive (shown as dots, still pasteable). */
class ClipOffer(val text: String, val at: Long, val sensitive: Boolean)
