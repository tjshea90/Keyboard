package app.swoop.keyboard.translate

import app.kestrel.core.translate.Translation
import app.kestrel.core.translate.TranslationEngine
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * The REAL services (MyMemory, Google) through the real [HttpsTransport] and the real engine, on a JVM. SKIPPED unless SWOOP_REAL_SERVICES=1, so no
 * normal test run ever touches the network. Run by the dispatch-only workflow `service-check.yml` (GitHub runner, direct internet) and by hand:
 *   SWOOP_REAL_SERVICES=1 ./gradlew :ime:testDebugUnitTest --tests '*RealServicesCheck*'
 * Sends fixed test sentences only (nothing anybody typed). Writes what happened to build/service-check.txt (or $SWOOP_REAL_OUT) for the job summary.
 */
class RealServicesCheck {
    @Test fun theRealServicesTranslateThroughTheRealEngine() {
        assumeTrue("set SWOOP_REAL_SERVICES=1 to call the real services", System.getenv("SWOOP_REAL_SERVICES") == "1")
        val out = StringBuilder()
        val log = ArrayList<String>()
        // 1 the staged probe (permission skipped: no Android here). Raw DNS / TCP / TLS steps need direct internet (a runner has it; a proxied sandbox may not).
        val steps = ConnectionProbe.run(null)
        out.appendLine("## Staged probe (JVM, the same code as the Diagnostics button)").appendLine(ConnectionProbe.format(steps)).appendLine()
        // 2 the engine end to end, both directions, with the real transport
        val engine = TranslationEngine(HttpsTransport()) { s, ms, r -> log.add("$s, $ms ms, $r") }
        var worked = 0
        for ((text, way) in listOf(
            "Where is the bus stop?" to Translation.Way.TO_MEXICAN,
            "I will rent a car and call you when I get there." to Translation.Way.TO_MEXICAN,
            "Donde esta la parada del autobus?" to Translation.Way.TO_ENGLISH
        )) {
            val r = engine.translate(text, way)
            out.appendLine("- ${way.label}: " + (if (r.ok) "OK by ${r.by}: ${r.text}" else "FAILED (${r.code}): ${r.why}"))
            if (r.ok) worked++
        }
        out.appendLine().appendLine("## Connection log (service, ms, result)")
        for (l in log) out.appendLine("- $l")
        val file = File(System.getenv("SWOOP_REAL_OUT") ?: "build/service-check.txt")
        file.parentFile?.mkdirs()
        file.writeText(out.toString())
        println(out)
        assertTrue("no translation worked: see " + file.path, worked > 0)
    }
}
