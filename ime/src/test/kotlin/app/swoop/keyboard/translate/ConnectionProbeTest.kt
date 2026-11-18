package app.swoop.keyboard.translate

import app.kestrel.core.translate.Http
import app.kestrel.core.translate.HttpReply
import app.kestrel.core.translate.TransportException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** R24a: the staged connection test. No test here opens a socket: the answer stage gets a fake Http, the rest is pure formatting. */
class ConnectionProbeTest {
    private class Fake(val f: (String, String?) -> HttpReply) : Http { override fun call(url: String, form: String?) = f(url, form) }

    private val mm = "{\"responseData\":{\"translatedText\":\"hola\"},\"responseStatus\":200}"

    @Test fun theAnswerStepShowsTheFixedWordAndWhatItBecame() {
        val s = ConnectionProbe.answerStep("MyMemory", "api.mymemory.translated.net", Fake { u, f -> assertTrue(u.startsWith("https://api.mymemory.translated.net/")); assertEquals(null, f); HttpReply(200, null, mm) })
        assertTrue(s.line(), s.ok); assertTrue(s.line(), s.line().contains("\"hello\" became \"hola\""))
    }

    @Test fun theGoogleAnswerStepPostsTheFixedWordOnly() {
        var sent: String? = null
        val s = ConnectionProbe.answerStep("Google", "translate.googleapis.com", Fake { _, f -> sent = f; HttpReply(200, null, "[[[\"hola\",\"hello\",null,null,10]],null,\"en\"]") })
        assertEquals("q=hello", sent); assertTrue(s.line(), s.ok)
    }

    @Test fun aUsedUpServiceStillCountsAsReached() {
        val s = ConnectionProbe.answerStep("MyMemory", "api.mymemory.translated.net", Fake { _, _ -> HttpReply(429, null, "") })
        assertTrue(s.ok); assertTrue(s.line(), s.line().contains("busy or used up"))
    }

    @Test fun aBrokenExchangeIsAFailedStepWithItsKindAndReason() {
        val s = ConnectionProbe.answerStep("MyMemory", "api.mymemory.translated.net", Fake { _, _ -> throw TransportException(TransportException.Kind.DROPPED, "SocketException (connection reset)") })
        assertFalse(s.ok); assertEquals("FAIL MyMemory: answer (", s.line().substring(0, 23)); assertTrue(s.line(), s.line().contains("DROPPED, SocketException (connection reset)"))
    }

    @Test fun onlyTheTwoTranslationHostsMayBeTested() {
        try { ConnectionProbe.run(null, Fake { _, _ -> HttpReply(200, null, "") }, listOf("example.com")); fail() } catch (e: IllegalArgumentException) { }
    }

    @Test fun withNoHostsOnlyTheClockIsChecked() {
        val steps = ConnectionProbe.run(null, Fake { _, _ -> throw AssertionError("no call") }, emptyList())
        assertEquals(listOf("Device clock"), steps.map { it.name })
        assertTrue(steps[0].line(), steps[0].ok)
    }

    @Test fun noIpv6RouteIsNotAFailureButATimeoutOrAResetIs() {
        assertTrue(ConnectionProbe.noRoute("ConnectException (ENETUNREACH, network unreachable)"))
        assertTrue(ConnectionProbe.noRoute("NoRouteToHostException (EHOSTUNREACH)"))
        assertFalse(ConnectionProbe.noRoute("SocketTimeoutException (timed out)"))
        assertFalse(ConnectionProbe.noRoute("SocketException (ECONNRESET, connection reset)"))
        assertFalse(ConnectionProbe.noRoute("ConnectException (EPERM, not permitted)"))
    }

    private fun st(n: String, ok: Boolean, note: String = "") = ProbeStep(n, ok, 5, note)

    private val healthy = listOf(
        st("Internet permission", true, "granted"), st("Device clock", true), st("MyMemory: name lookup", true, "2 address(es)"), st("MyMemory: connect IPv4", true),
        st("MyMemory: secure handshake", true, "TLSv1.3"), st("MyMemory: answer", true, "HTTP 200, \"hello\" became \"hola\""), st("Google: answer", true, "HTTP 200, \"hello\" became \"hola\"")
    )

    @Test fun theVerdictNamesTheFirstThingThatIsWrong() {
        assertEquals("Result: translation can work from here.", ConnectionProbe.verdict(healthy))
        assertTrue(ConnectionProbe.verdict(healthy.map { if (it.name == "Internet permission") st(it.name, false, "NOT granted") else it }).contains("no internet permission"))
        val eperm = healthy + st("Google: connect IPv4", false, "SocketException (EPERM, not permitted)")
        assertTrue(ConnectionProbe.verdict(eperm), ConnectionProbe.verdict(eperm).contains("refuses the keyboard a network socket"))
        assertTrue(ConnectionProbe.verdict(listOf(st("Device clock", true), st("MyMemory: name lookup", false, "UnknownHostException"))).contains("could not be looked up"))
        assertTrue(ConnectionProbe.verdict(listOf(st("MyMemory: name lookup", true), st("MyMemory: connect IPv4", false, "ConnectException (ECONNREFUSED)"), st("MyMemory: connect IPv6", false, "ConnectException"))).contains("REFUSED (ECONNREFUSED)"))
        assertTrue(ConnectionProbe.verdict(listOf(st("MyMemory: name lookup", true), st("MyMemory: connect IPv4", false, "SocketTimeoutException"))).contains("no connection could be made"))
        assertTrue(ConnectionProbe.verdict(healthy + st("Google: secure handshake", false, "SSLHandshakeException (certificate not trusted)")).contains("secure handshake with Google failed"))
        assertTrue(ConnectionProbe.verdict(listOf(st("MyMemory: answer", false, "DROPPED, SocketException (connection reset)"))).contains("exchange broke"))
        assertTrue(ConnectionProbe.verdict(listOf(st("Device clock", false, "1970"))).contains("date is wrong"))
    }

    @Test fun aBlockedSocketWithDataSaverOnNamesDataSaverAndTheFix() {
        val limit = st("Android limits", false, "Data Saver ON and Swoop is NOT on the Unrestricted data list, standby bucket active")
        val v = ConnectionProbe.verdict(healthy + limit + st("MyMemory: connect IPv4", false, "ConnectException (EPERM, not permitted)"))
        assertTrue(v, v.contains("refuses the keyboard a network socket (MyMemory: connect IPv4: EPERM)"))
        assertTrue(v, v.contains("Data Saver ON") && v.contains("Unrestricted data"))
        val plain = ConnectionProbe.verdict(healthy + st("Google: connect IPv4", false, "ConnectException (EACCES, permission denied)"))
        assertTrue(plain, plain.contains("EACCES") && !plain.contains("The cause is on this device"))
    }

    @Test fun aLimitAloneIsAWarningThatTheKeyboardMayBeRefused() {
        val v = ConnectionProbe.verdict(healthy + st("Android limits", false, "Data Saver ON and Swoop is NOT on the Unrestricted data list"))
        assertTrue(v, v.startsWith("Result: Android limits what Swoop may use in the background"))
        assertTrue(v, v.contains("Unrestricted data"))
    }

    @Test fun theFactsAreReadAsWords() {
        val on = AndroidLimits.Facts(AndroidLimits.DataSaver.ON_NOT_ALLOWED, false, false, false, "active")
        assertTrue(on.limits); assertTrue(on.note(), on.note().contains("Data Saver ON and Swoop is NOT on the Unrestricted data list"))
        val ok = AndroidLimits.Facts(AndroidLimits.DataSaver.ON_ALLOWED, false, false, false, "active")
        assertFalse(ok.limits); assertTrue(ok.note(), ok.note().contains("on the Unrestricted data list"))
        assertFalse(AndroidLimits.Facts(AndroidLimits.DataSaver.OFF, false, false, false, "working set").limits)
        assertTrue(AndroidLimits.Facts(AndroidLimits.DataSaver.OFF, true, false, false, "active").note().contains("battery saver ON"))
        assertTrue(AndroidLimits.Facts(AndroidLimits.DataSaver.OFF, false, false, true, "restricted").limits)
    }

    @Test fun theTextHasOneLinePerStepAndTheVerdictLast() {
        val t = ConnectionProbe.format(healthy)
        val lines = t.split("\n")
        assertEquals(healthy.size + 1, lines.size)
        assertTrue(lines.last().startsWith("Result:"))
        assertTrue(lines[2], lines[2].startsWith("OK   MyMemory: name lookup (5 ms)"))
    }

    @Test fun theConnectionLogKeepsTheLastTwentyAndNeverWords() {
        ConnectionLog.clear()
        assertEquals("", ConnectionLog.text())
        for (i in 1..25) ConnectionLog.add("translate (MyMemory)", i.toLong(), "failed (DROPPED: SocketException)")
        val lines = ConnectionLog.text().split("\n")
        assertEquals(20, lines.size)
        assertTrue(lines.first(), lines.first().contains(" 6 ms "))
        assertTrue(lines.last(), lines.last().contains(" 25 ms ") && lines.last().contains("DROPPED"))
        ConnectionLog.clear()
    }
}
