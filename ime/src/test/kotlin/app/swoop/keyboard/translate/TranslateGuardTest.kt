package app.swoop.keyboard.translate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guard rails 2, 3 and 5 of plan 9.2 for the translate package: no other URL than the two hosts, no network class outside translate/,
 * INTERNET is the only permission, cleartext traffic stays off. (The dex scan and the APK permission fixture run in CI.)
 */
class TranslateGuardTest {
    private val translateDir = File("src/main/kotlin/app/swoop/keyboard/translate")
    private val srcRoot = File("src/main/kotlin")

    private fun kt(dir: File): List<File> = dir.walkTopDown().filter { it.isFile && it.name.endsWith(".kt") }.toList()

    @Test fun theTranslatePackageNamesOnlyTheTwoHostsAndNoCleartext() {
        assertTrue(translateDir.absolutePath, translateDir.isDirectory)
        val hostLike = Regex("\\b[a-z0-9-]+(?:\\.[a-z0-9-]+)*\\.(?:com|net|org|io|dev|info|xyz|app)\\b")
        val found = HashSet<String>()
        for (f in kt(translateDir)) {
            val text = f.readText()
            assertFalse(f.name + " has a cleartext address", text.contains("http://"))
            for (m in Regex("https://([^\\s\"'/)]+)").findAll(text)) found.add(m.groupValues[1])
            val noImports = text.lines().filterNot { it.startsWith("import ") || it.startsWith("package ") }.joinToString("\n")
            for (m in hostLike.findAll(noImports)) {
                val v = m.value
                if (v.startsWith("java.") || v.startsWith("javax.") || v.startsWith("app.swoop.") || v.startsWith("app.kestrel.")) continue
                found.add(v)
            }
        }
        assertTrue(found.toString(), TranslateGuard.HOSTS.containsAll(found))
    }

    @Test fun noNetworkCodeOutsideTheTranslatePackage() {
        val banned = listOf("java.net", "javax.net", "android.net", "okhttp", "HttpURLConnection", "URLConnection", "Socket(", "https://", "http://")
        for (f in kt(srcRoot)) {
            if (f.absolutePath.startsWith(translateDir.absolutePath)) continue
            val text = f.readText()
            for (b in banned) assertFalse(f.path + " mentions " + b, text.contains(b))
        }
    }

    @Test fun onlyTheTransportOpensConnections() {
        for (f in kt(translateDir)) {
            if (f.name == "HttpsTransport.kt") continue
            val t = f.readText()
            assertFalse(f.name, t.contains("openConnection") || t.contains("java.net"))
        }
    }

    @Test fun nothingIsEverLoggedInTheTranslatePackage() {
        for (f in kt(translateDir)) {
            val t = f.readText()
            assertFalse(f.name, t.contains("android.util.Log") || t.contains("println(") || t.contains("printStackTrace"))
        }
    }

    @Test fun manifestHasInternetOnlyAndCleartextOff() {
        val m = File("src/main/AndroidManifest.xml").readText()
        val perms = Regex("<uses-permission[^>]*android:name=\"([^\"]+)\"").findAll(m).map { it.groupValues[1] }.toList()
        assertEquals(listOf("android.permission.INTERNET"), perms)
        assertTrue(m.contains("android:usesCleartextTraffic=\"false\""))
        assertFalse(m.contains("ACCESS_NETWORK_STATE"))
        assertFalse(m.contains("usesCleartextTraffic=\"true\""))
    }

    @Test fun permissionFixtureMatchesTheManifest() {
        val lines = File("../tools/expected-permissions.txt").readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        assertEquals(listOf("android.permission.INTERNET"), lines)
    }
}
