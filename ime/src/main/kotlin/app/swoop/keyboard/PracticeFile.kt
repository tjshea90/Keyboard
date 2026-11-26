package app.swoop.keyboard

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** R34a2 / R36: the one place where a practice or training file is written: one plain-text file in Downloads (no picker, no permission), the way the learning export is written. */
object PracticeFile {
    /** "Swoop glide practice 2026-11-26 1702.txt": [what] then the date and time. */
    fun name(what: String, now: Date = Date()): String = "Swoop glide $what " + SimpleDateFormat("yyyy-MM-dd HHmm", Locale.US).format(now) + ".txt"

    /** Writes [text] as [name] into Downloads. Returns null when it worked, else what went wrong in a few words. */
    fun save(ctx: Context, name: String, text: String): String? {
        try {
            val cr = ctx.contentResolver
            val cv = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name); put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS); put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv) ?: throw java.io.IOException("Android would not create the file")
            try {
                (cr.openOutputStream(uri, "w") ?: throw java.io.IOException("Android would not open the file")).use { it.write(text.toByteArray(Charsets.UTF_8)) }
                cr.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) { try { cr.delete(uri, null, null) } catch (_: Exception) { }; throw e }
            return null
        } catch (e: Exception) { return e.message ?: e.javaClass.simpleName }
    }

    /** The app's version name for the file header (nothing else about the person or the device is written). */
    fun version(ctx: Context): String = try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" } catch (_: Exception) { "" }
}
