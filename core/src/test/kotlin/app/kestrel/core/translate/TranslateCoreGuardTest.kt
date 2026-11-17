package app.kestrel.core.translate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The translation code in :core must stay pure: no sockets, no java.net, exactly two host names. */
class TranslateCoreGuardTest {
    private fun sources(): List<File> {
        val dir = File("src/main/kotlin/app/kestrel/core/translate")
        assertTrue("run from the :core module dir: " + dir.absolutePath.length, dir.isDirectory)
        return dir.listFiles { f -> f.name.endsWith(".kt") }!!.toList()
    }

    @Test fun noNetworkClassesInCore() {
        val banned = listOf("java.net", "javax.net", "android.", "okhttp", "HttpURLConnection", "Socket", "URLConnection")
        for (f in sources()) {
            val text = f.readText()
            for (b in banned) assertFalse(f.name + " mentions " + b, text.contains(b))
        }
    }

    @Test fun onlyTheTwoHostsAreEverNamed() {
        val url = Regex("https?://[^\\s\"'/)]+")
        val hosts = HashSet<String>()
        for (f in sources()) {
            val text = f.readText()
            assertFalse(f.name + " has a cleartext URL", text.contains("http://"))
            for (m in url.findAll(text)) hosts.add(m.value.removePrefix("https://"))
        }
        // the constants HOST_* hold the names; the URLs are built from them
        assertEquals(setOf<String>(), hosts)
        assertEquals("api.mymemory.translated.net", Translation.HOST_MYMEMORY)
        assertEquals("translate.googleapis.com", Translation.HOST_GOOGLE)
    }
}
