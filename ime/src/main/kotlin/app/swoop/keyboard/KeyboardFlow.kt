package app.swoop.keyboard

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import app.kestrel.core.input.glide.LearnedExport
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Export / Import of what the keyboard learned (ported from the Kestrel browser's KeyboardFlow; the file format is core's LearnedExport, docs/KEYBOARD-EXPORT.md, and the
 * keyboard keeps reading format v1 forever so the browser's file imports here). Export: ONE tap writes a plain-text file into Downloads (shared storage, no picker, no
 * permission). Import: a file the owner picks with the system file picker, size-bounded, confirmed in words before anything changes. The file is the owner's private typing
 * history: it is never logged, never uploaded, never put in the repository.
 */
class KeyboardFlow(private val a: Activity, private val glide: GlideHost, private val host: Host) {
    interface Host {
        fun say(message: String)
        fun ask(title: String, message: String, yes: String, no: String, result: (Boolean) -> Unit)
    }

    /** Export now: one file in Downloads. */
    fun exportNow() {
        Io.exec {
            glide.engine.exportText(glide.habitOrNull(), glide.own) { text ->
                if (text.isBlank()) { Io.ui { host.say(a.getString(R.string.flow_not_ready)) }; return@exportText }
                Io.exec {
                    val message = try {
                        val name = save(text)
                        val s = LearnedExport.read(text)?.summary
                        a.getString(R.string.flow_saved, name) + (if (s != null) ": " + s.describe() else "") + a.getString(R.string.flow_keep_private)
                    } catch (e: Exception) { a.getString(R.string.flow_save_failed, e.message ?: e.javaClass.simpleName) }
                    Io.ui { host.say(message) }
                }
            }
        }
    }

    /** Writes [text] as a new file in the Downloads folder and returns the name Android gave it. */
    private fun save(text: String): String {
        val cr = a.contentResolver
        val cv = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName())
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv) ?: throw IOException("Android would not create the file")
        try {
            (cr.openOutputStream(uri, "w") ?: throw IOException("Android would not open the file")).use { it.write(text.toByteArray(Charsets.UTF_8)) }
            cr.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            try { cr.delete(uri, null, null) } catch (x: Exception) { /* nothing left to clean */ }
            throw e
        }
        return try { cr.query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } } catch (e: Exception) { null } ?: fileName()
    }

    /** Import from a file the person picks. */
    fun importFromFile() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "application/octet-stream"))
        }
        try { a.startActivityForResult(i, REQ_IMPORT) } catch (e: Exception) { host.say(a.getString(R.string.flow_no_picker)) }
    }

    /** The activity forwards its onActivityResult here; true when the result was ours. */
    fun onResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != REQ_IMPORT) return false
        val uri = data?.data
        if (resultCode != Activity.RESULT_OK || uri == null) return true
        val cr = a.contentResolver
        Io.exec {
            val read: Any = try {
                val bytes = (cr.openInputStream(uri) ?: throw IOException("no stream")).use { readBounded(it, LearnedExport.MAX_CHARS) }
                val text = String(bytes, Charsets.UTF_8)
                if (LearnedExport.read(text) == null) a.getString(R.string.flow_not_export) else text
            } catch (e: TooBig) { a.getString(R.string.flow_too_big) } catch (e: Exception) { a.getString(R.string.flow_read_failed) }
            Io.ui { if (read is String && read.startsWith(LearnedExport.MAGIC)) confirm(read) else host.say(read.toString()) }
        }
        return true
    }

    private fun confirm(text: String) {
        val s = LearnedExport.read(text)?.summary ?: run { host.say(a.getString(R.string.flow_not_export)); return }
        host.ask(a.getString(R.string.flow_import_title), a.getString(R.string.flow_import_message, s.describe()), a.getString(R.string.flow_import_yes), a.getString(R.string.flow_import_no)) { ok ->
            if (ok) Io.exec { importNow(text) }
        }
    }

    /** Adds an export's contents to what the keyboard learned (any thread). */
    fun importNow(text: String) {
        glide.engine.importText(text, false, glide.own, { h -> glide.saveHabit(h) }) { sum ->
            if (sum != null) glide.saveOwn(glide.own.serialize())
            Io.ui { host.say(if (sum == null) a.getString(R.string.flow_import_failed) else a.getString(R.string.flow_imported, sum.describe())) }
        }
    }

    /** Forget everything that was learned, after the person said yes. */
    fun forgetNow() {
        host.ask("Forget what the keyboard learned?", "Your words, word pairs, corrections and thumb habit are deleted from this device. Settings stay. This cannot be undone (an export file you made earlier can be imported again).", "Forget", "Cancel") { ok ->
            if (ok) Io.exec { glide.forgetEverything { Io.ui { host.say("Forgotten. The keyboard starts learning again from now.") } } }
        }
    }

    class TooBig : IOException()

    companion object {
        const val REQ_IMPORT = 7352
        fun fileName(now: Long = System.currentTimeMillis()): String = "Swoop keyboard learning " + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now)) + ".txt"
        fun readBounded(s: InputStream, max: Int): ByteArray {
            val out = java.io.ByteArrayOutputStream(64 * 1024)
            val buf = ByteArray(32 * 1024)
            while (true) {
                val n = s.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                if (out.size() > max) throw TooBig()
            }
            return out.toByteArray()
        }
    }
}
