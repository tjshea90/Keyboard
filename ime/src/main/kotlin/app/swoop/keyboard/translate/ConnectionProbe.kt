package app.swoop.keyboard.translate

import android.content.Context
import android.content.pm.PackageManager
import app.kestrel.core.translate.Http
import app.kestrel.core.translate.SafeReason
import app.kestrel.core.translate.Translation
import app.kestrel.core.translate.TransportException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/** One step of the connection test: its name, whether it worked, how long it took and a short safe note (a [SafeReason], a count, a protocol name). */
class ProbeStep(val name: String, val ok: Boolean, val millis: Long, val note: String) {
    /** `OK   MyMemory: name lookup (12 ms): 2 addresses` */
    fun line(): String = (if (ok) "OK   " else "FAIL ") + name + " (" + millis + " ms)" + (if (note.isEmpty()) "" else ": $note")
}

/**
 * The "Test translation connection" button of the Diagnostics screen (R24a, 2026-11-18: the owner saw "could not be reached (SocketException)"
 * and nothing said which step broke). It walks the same road a translation takes, one stage at a time, for each of the two services:
 * internet permission, device clock, name lookup, TCP connect (IPv4 and IPv6 separately), secure handshake, then a real answer through
 * [HttpsTransport] for ONE fixed test word ([WORD]: never anything the owner typed). Each step carries a [SafeReason]: class name, errno name
 * or a known phrase, never an address, never text. The last line is a [verdict] in plain words.
 *
 * Only the two hosts of [TranslateGuard] are ever contacted. Blocking: call from a background thread only. This is the second file (with
 * [HttpsTransport]) that may open a socket; TranslateGuardTest lists both.
 */
object ConnectionProbe {
    const val WORD = "hello"
    private const val CONNECT_MS = 6000
    private const val HANDSHAKE_MS = 8000
    /** A device clock before this is wrong for certain (the app is newer): certificates then look "not yet valid". */
    private const val MIN_YEAR = 2025

    private fun label(host: String): String = if (host == Translation.HOST_MYMEMORY) "MyMemory" else "Google"

    /** The steps, in order. [onStep] is called as each one finishes (for a screen that fills in). [context] null skips the permission step (JVM tests). */
    fun run(
        context: Context?,
        http: Http = HttpsTransport(),
        hosts: List<String> = listOf(Translation.HOST_MYMEMORY, Translation.HOST_GOOGLE),
        onStep: (ProbeStep) -> Unit = {}
    ): List<ProbeStep> {
        val out = ArrayList<ProbeStep>()
        fun add(s: ProbeStep) { out.add(s); try { onStep(s) } catch (_: RuntimeException) { } }
        for (h in hosts) require(TranslateGuard.HOSTS.contains(h)) { "only the two translation hosts may be tested" }

        if (context != null) {
            val granted = try { context.checkSelfPermission("android.permission.INTERNET") == PackageManager.PERMISSION_GRANTED } catch (e: RuntimeException) { false }
            add(ProbeStep("Internet permission", granted, 0, if (granted) "granted" else "NOT granted to this app"))
        }
        AndroidLimits.inInetGroup()?.let { member ->
            add(ProbeStep("Internet access group", member, 0, if (member) "this process has Android's internet group (inet)" else "this process is NOT in Android's internet group (inet): Android did not give Swoop its internet access"))
        }
        if (context != null) {
            // R25: Data Saver / battery saver / doze / a restricted app: what can make Android refuse a BACKGROUND app (a keyboard is one) the network on purpose
            val f = AndroidLimits.read(context)
            // battery saver and doze count too: on Android's own builds they only make connections time out, but a tablet maker's build can refuse an app's sockets outright while battery saver is on (the owner's tablet, R25c)
            val blocksNetwork = f.dataSaver == AndroidLimits.DataSaver.ON_NOT_ALLOWED || f.backgroundRestricted || f.standbyBucket == "restricted" || f.batterySaver || f.doze
            add(ProbeStep("Android limits", !blocksNetwork, 0, f.note()))
        }
        val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val year = cal.get(java.util.Calendar.YEAR)
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm 'UTC'", Locale.US).also { it.timeZone = TimeZone.getTimeZone("UTC") }
        add(ProbeStep("Device clock", year >= MIN_YEAR, 0, fmt.format(Date()) + (if (year >= MIN_YEAR) "" else " (wrong: secure connections need the right date)")))

        for (host in hosts) {
            val who = label(host)
            val t0 = System.currentTimeMillis()
            val addrs: List<InetAddress> = try {
                val a = InetAddress.getAllByName(host).toList()
                add(ProbeStep("$who: name lookup", true, System.currentTimeMillis() - t0, "${a.size} address(es): ${a.count { it is Inet4Address }} IPv4, ${a.count { it is Inet6Address }} IPv6"))
                a
            } catch (e: Exception) {
                add(ProbeStep("$who: name lookup", false, System.currentTimeMillis() - t0, reason(e)))
                emptyList()
            }
            var reachable: InetAddress? = null
            for ((fam, pick) in listOf<Pair<String, (InetAddress) -> Boolean>>("IPv4" to { it is Inet4Address }, "IPv6" to { it is Inet6Address })) {
                val a = addrs.firstOrNull(pick) ?: continue
                val t = System.currentTimeMillis()
                try {
                    Socket().use { it.connect(InetSocketAddress(a, 443), CONNECT_MS) }
                    add(ProbeStep("$who: connect $fam", true, System.currentTimeMillis() - t, "port 443 reached"))
                    if (reachable == null) reachable = a
                } catch (e: Exception) {
                    val why = reason(e)
                    // no IPv6 route while IPv4 works is normal (many hotspots and the emulators have none): not a failure, translation uses IPv4
                    if (reachable != null && fam == "IPv6" && noRoute(why)) add(ProbeStep("$who: connect $fam", true, System.currentTimeMillis() - t, "not available here ($why): IPv4 is used"))
                    else add(ProbeStep("$who: connect $fam", false, System.currentTimeMillis() - t, why))
                }
            }
            val target = reachable
            if (target != null) {
                val t = System.currentTimeMillis()
                try {
                    val raw = Socket()
                    raw.connect(InetSocketAddress(target, 443), CONNECT_MS)
                    raw.soTimeout = HANDSHAKE_MS
                    val ssl = (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket(raw, host, 443, true) as SSLSocket
                    ssl.use { s -> s.startHandshake(); add(ProbeStep("$who: secure handshake", true, System.currentTimeMillis() - t, s.session.protocol)) }
                } catch (e: Exception) {
                    add(ProbeStep("$who: secure handshake", false, System.currentTimeMillis() - t, reason(e)))
                }
            }
            add(answerStep(who, host, http))
        }
        return out
    }

    /** The real road: one tiny translation of [WORD] through the same transport and parser a translation uses. ok = an answer arrived (even "used up"). */
    internal fun answerStep(who: String, host: String, http: Http): ProbeStep {
        val t = System.currentTimeMillis()
        val way = Translation.Way.TO_MEXICAN
        return try {
            val r = if (host == Translation.HOST_MYMEMORY) {
                val got = http.call(Translation.myMemoryUrl(WORD, way), null)
                got.code to Translation.parseMyMemory(got.code, got.body)
            } else {
                val got = http.call(Translation.googleUrl(way), Translation.googleForm(WORD))
                got.code to Translation.parseGoogle(got.code, got.location, got.body)
            }
            val res = r.second
            ProbeStep("$who: answer", true, System.currentTimeMillis() - t,
                if (res.ok) "HTTP ${r.first}, \"$WORD\" became \"${res.text.trim().take(40)}\"" else "HTTP ${r.first}, not translated: ${res.code}${if (res.busy) " (busy or used up)" else ""}")
        } catch (e: TransportException) {
            ProbeStep("$who: answer", false, System.currentTimeMillis() - t, e.kind.name + (if (e.message.isNullOrEmpty()) "" else ", " + e.message))
        } catch (e: RuntimeException) {
            ProbeStep("$who: answer", false, System.currentTimeMillis() - t, reason(e))
        }
    }

    /** A refusal that says "there is no such route" at once (not a timeout, not a reset): ENETUNREACH / EHOSTUNREACH. */
    internal fun noRoute(reason: String): Boolean = reason.contains("ENETUNREACH") || reason.contains("EHOSTUNREACH") || reason.contains("network unreachable")

    private fun reason(e: Throwable): String = SafeReason.of(e.javaClass.simpleName, e.message)

    /** The steps as text for the screen and the clipboard: one line each, then the verdict. */
    fun format(steps: List<ProbeStep>): String = steps.joinToString("\n") { it.line() } + "\n" + verdict(steps)

    /** What the steps mean, in plain words: the first thing that is wrong, or that translation can work from here. Pure. */
    fun verdict(steps: List<ProbeStep>): String {
        fun bad(prefix: String) = steps.firstOrNull { !it.ok && it.name.contains(prefix) }
        steps.firstOrNull { it.name == "Internet permission" && !it.ok }?.let {
            return "Result: the keyboard has no internet permission. Reinstall Swoop from the Release."
        }
        val limits = steps.firstOrNull { it.name == "Android limits" && !it.ok }
        val blocked = steps.firstOrNull { !it.ok && it.name != "Android limits" && (it.note.contains("EPERM") || it.note.contains("EACCES")) }
        val noGroup = steps.firstOrNull { it.name == "Internet access group" && !it.ok }
        if (noGroup != null) {
            return "Result: Android did not give the Swoop process its internet access (it is not in the internet group), although the permission is declared. This happens after an update that added the permission. Restart the device now. If it still fails afterwards, uninstall Swoop and install it again from the Release (export your learned words first)."
        }
        if (blocked != null) {
            val head = "Result: Android refuses the keyboard a network socket (${blocked.name}: ${SafeReason.errnoOf(blocked.note) ?: "refused"})." +
                (if (blocked.note.contains("socket creation refused")) " It refuses to even CREATE the socket, so this is Android's permission record for Swoop (or a tablet-maker block on the app), not a problem on the network path. Switch the tablet completely off and on again first: a permission added by an update is sometimes not passed on to Android's network layer until then." else "")
            // R25c: measured on an Android 10 emulator: Data Saver = refused connections, battery saver and doze = time-outs, never EPERM; but the owner's tablet gave EPERM with battery saver ON, so a tablet maker's build can be stricter: say so
            if (limits != null && limits.note.contains("battery saver ON")) {
                return head + " Battery saver is ON. On Android's own builds that only makes connections slow, but some tablets cut an app's network completely while it is on (this test screen was in front, so it is not a background rule). Switch Battery saver off (Settings, Battery) and test again. If it still fails: restart the device once, then look for a firewall, VPN or ad-blocking app, a vendor switch that blocks an app's internet (Settings, Apps, Swoop), or a parental-control or school profile."
            }
            return head + " Android's own Data Saver shows as a refused connection and battery saver or doze as time-outs, so this is something else. Restart the device once; then look for a firewall, VPN or ad-blocking app, a vendor setting that blocks an app's internet (Settings, Apps, Swoop: mobile data / network switches), a parental-control or school profile, or Battery saver (switch it off to rule it out)."
        }
        bad("name lookup")?.let { return "Result: the name of ${it.name.substringBefore(':')} could not be looked up: the device is offline, or private DNS / a filter blocks it. Is the phone's mobile data on?" }
        val connects = steps.filter { it.name.contains(": connect ") }
        if (connects.isNotEmpty() && connects.none { it.ok } && connects.any { it.note.contains("ECONNREFUSED") }) return "Result: the connection is REFUSED (ECONNREFUSED), the services themselves never do that. On Android 10 that is what Data Saver does to a background app on a metered link such as a phone's hotspot: open Settings, Network and internet, Data Saver, Unrestricted data and switch Swoop on (the first button below opens it), or turn Data Saver off."
        if (connects.isNotEmpty() && connects.none { it.ok }) return "Result: no connection could be made to the services (${connects.first().note}). Offline, or a firewall blocks them."
        bad("secure handshake")?.let { return "Result: the secure handshake with ${it.name.substringBefore(':')} failed (${it.note}). Check the date and time; a hotspot filter may be cutting it." }
        bad("answer")?.let { return "Result: ${it.name.substringBefore(':')} was reached but the exchange broke (${it.note}). Flaky link: try again." }
        steps.firstOrNull { it.name == "Device clock" && !it.ok }?.let { return "Result: the device date is wrong; fix it." }
        if (limits != null) {
            val fix = if (limits.note.contains("Data Saver ON")) "Open Settings, Network and internet, Data Saver, Unrestricted data and switch Swoop on (the first button below opens it)." else "Switch Battery saver off (Settings, Battery), or allow Swoop under the app's battery settings (the second button below)."
            return "Result: Android limits what Swoop may use (${limits.note}). This test passed because this screen is in front, but the keyboard runs in the background, so translating from it may be refused. $fix"
        }
        val answers = steps.filter { it.name.endsWith(": answer") }
        if (answers.any { it.note.contains("became") }) return "Result: translation can work from here."
        return "Result: the services were reached but did not translate the test word (see the answer lines)."
    }
}
