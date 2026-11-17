package app.swoop.keyboard

import android.content.ClipboardManager
import android.content.Context
import app.kestrel.core.clipboard.ClipboardStore
import java.io.File

/**
 * Where the clipboard history lives: the [ClipboardStore], its private file, and reading what Android's clipboard holds. All on the UI thread except the file write (the Io thread).
 * The history never leaves the phone and is never logged. Android lets the active keyboard read the clipboard; a copy marked sensitive (a password manager sets that) is skipped.
 */
class ClipboardHost(private val context: Context, now: () -> Long = { System.currentTimeMillis() }) {
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

    /** Takes what the Android clipboard holds now into the history. True when the list changed. */
    fun capture(): Boolean {
        ensureLoaded()
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip ?: return false
            if (clip.itemCount == 0) return false
            val sensitive = try { clip.description.extras?.getBoolean(SENSITIVE_EXTRA, false) == true } catch (_: Exception) { false }
            if (sensitive) return false
            val text = clip.getItemAt(0).coerceToText(context)?.toString() ?: return false
            val changed = store.add(text)
            if (changed) save()
            changed
        } catch (_: Exception) { false }          // some Android versions refuse the read when no window has the focus: the history simply does not grow
    }

    companion object { const val SENSITIVE_EXTRA = "android.content.extra.IS_SENSITIVE" }
}
