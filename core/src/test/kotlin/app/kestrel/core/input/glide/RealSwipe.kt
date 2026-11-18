package app.kestrel.core.input.glide

import app.kestrel.core.translate.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * R24b: REAL swipes, for the first time. The FUTO Swipe corpus (MIT licence, 1.04 million swipes from thousands of donors, one JSON object per line with the touch points
 * {t, x, y} normalised to 0..1, the canvas size, the target word and its sentence). This is TEST-SIDE tooling only: nothing of it ships, the data is never committed
 * (CLAUDE.md: no replay logs in the repository; the file goes to a build/scratch folder or a runner's temp folder).
 *
 * [fetch] reads the first bytes of the file over HTTPS (a Range request, so a few MB give a few thousand swipes) and keeps whole lines only. [read] parses them.
 */
object RealSwipe {
    const val DEV_URL = "https://huggingface.co/datasets/futo-org/swipe.futo.org/resolve/main/dev.jsonl"

    class Swipe(val word: String, val sentence: String, val wordIdx: Int, val canvasW: Float, val canvasH: Float, val orientation: String, val xs: FloatArray, val ys: FloatArray, val ts: FloatArray)

    /** Downloads about [bytes] from the start of [url] into [to] (whole lines only). Returns the number of bytes kept. */
    fun fetch(url: String, bytes: Long, to: File): Long {
        to.parentFile?.mkdirs()
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 30_000; c.readTimeout = 60_000; c.instanceFollowRedirects = true
        c.setRequestProperty("Range", "bytes=0-" + (bytes - 1))
        c.setRequestProperty("User-Agent", "swoop-eval")
        val code = c.responseCode
        check(code == 200 || code == 206) { "HTTP $code" }
        val all = c.inputStream.use { it.readNBytes(bytes.toInt()) }
        c.disconnect()
        val cut = all.lastIndexOf('\n'.code.toByte())
        check(cut > 0) { "no complete line in $bytes bytes" }
        to.writeBytes(all.copyOf(cut + 1))
        return (cut + 1).toLong()
    }

    @Suppress("UNCHECKED_CAST")
    fun read(file: File, max: Int = Int.MAX_VALUE): List<Swipe> {
        val out = ArrayList<Swipe>()
        file.bufferedReader().useLines { lines ->
            for (line in lines) {
                if (out.size >= max) break
                if (line.isBlank()) continue
                val o = try { Json.parseObject(line) } catch (e: Exception) { continue }
                val data = o["data"] as? List<Any?> ?: continue
                if (data.size < 3) continue
                val xs = FloatArray(data.size); val ys = FloatArray(data.size); val ts = FloatArray(data.size)
                for ((i, p) in data.withIndex()) { val m = p as Map<String, Any?>; xs[i] = (m["x"] as Number).toFloat(); ys[i] = (m["y"] as Number).toFloat(); ts[i] = (m["t"] as Number).toFloat() }
                out.add(Swipe(
                    (o["word"] as? String) ?: continue, (o["sentence"] as? String) ?: "", (o["word_idx"] as? Number)?.toInt() ?: -1,
                    (o["canvas_width"] as? Number)?.toFloat() ?: 0f, (o["canvas_height"] as? Number)?.toFloat() ?: 0f, (o["orientation"] as? String) ?: "", xs, ys, ts
                ))
            }
        }
        return out
    }

    /** The word as the decoder knows it: lower case letters only (apostrophes, hyphens, digits and punctuation dropped). Null when nothing is left. */
    fun letters(word: String): String? { val s = word.lowercase().filter { it in 'a'..'z' }; return s.ifEmpty { null } }
}
