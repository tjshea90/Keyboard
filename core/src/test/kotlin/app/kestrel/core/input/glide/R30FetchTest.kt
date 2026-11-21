package app.kestrel.core.input.glide

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * R30: test-side download of the OPEN trained swipe encoder (MIT, see research/R30-trained-shape-model.md) into the git-ignored scratch folder core/build/r30/.
 * Nothing is committed from here: the weights file is added to the repository only after its licence file has been read and the golden-vector test passes.
 * Skipped unless R30_FETCH=1. Extra files: R30_URLS="name=url,name=url" (default list below).
 */
class R30FetchTest {
    private val base = "https://huggingface.co/xxparthparekhxx/compose-keyboard-swipe-encoder/resolve/main/"

    private fun get(url: String, to: File): String {
        var u = url
        for (hop in 0 until 6) {
            val c = URL(u).openConnection() as HttpURLConnection
            c.connectTimeout = 30_000; c.readTimeout = 120_000; c.instanceFollowRedirects = false
            c.setRequestProperty("User-Agent", "swoop-eval")
            val code = c.responseCode
            if (code in 301..308) { u = URL(URL(u), c.getHeaderField("Location")).toString(); c.disconnect(); continue }
            check(code == 200) { "HTTP $code for $u" }
            val md = MessageDigest.getInstance("SHA-256")
            to.parentFile?.mkdirs()
            var n = 0L
            c.inputStream.use { i -> to.outputStream().use { o -> val b = ByteArray(65536); while (true) { val r = i.read(b); if (r < 0) break; o.write(b, 0, r); md.update(b, 0, r); n += r } } }
            c.disconnect()
            return "$n bytes sha256=" + md.digest().joinToString("") { "%02x".format(it) }
        }
        error("too many redirects for $url")
    }

    @Test fun fetchOpenEncoder() {
        assumeTrue("set R30_FETCH=1", System.getenv("R30_FETCH") == "1")
        val dir = File("build/r30")
        val wanted = (System.getenv("R30_URLS") ?: "swipe_encoder.bin=${base}swipe_encoder.bin,swipe_words.txt=${base}swipe_words.txt,swipe_reference.bin=https://raw.githubusercontent.com/xxparthparekhxx/keyboard/master/app/src/test/resources/swipe_reference.bin").split(',')
        for (w in wanted) {
            val name = w.substringBefore('='); val url = w.substringAfter('=')
            val f = File(dir, name)
            val line = try { get(url, f) } catch (e: Exception) { "FAILED: " + e.message }
            println("R30 fetch $name <- $url : $line")
        }
    }
}
