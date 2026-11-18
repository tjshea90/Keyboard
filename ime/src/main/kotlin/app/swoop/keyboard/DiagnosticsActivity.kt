package app.swoop.keyboard

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import app.swoop.keyboard.translate.ConnectionProbe

/** KB-105: counters and timings, with a Copy button, for pasting into a chat. Numbers only (see [Diagnostics]). Not exported: opened from the Setup screen. */
class DiagnosticsActivity : Activity() {
    private lateinit var body: TextView
    private lateinit var probeBody: TextView
    /** The finished connection test, for Copy (empty until one was run). */
    private var probeText = ""
    @Volatile private var probing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(settingsTheme(Settings(this).theme))
        super.onCreate(savedInstanceState)
        title = getString(R.string.diag_title)
        val d = resources.displayMetrics.density
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; val p = (16 * d).toInt(); setPadding(p, p, p, p) }
        body = TextView(this).apply { setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); setTextIsSelectable(true); typeface = android.graphics.Typeface.MONOSPACE }
        col.addView(body, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(Button(this).apply { text = getString(R.string.diag_copy); setOnClickListener { copy() } })
        col.addView(Button(this).apply { text = getString(R.string.diag_refresh); setOnClickListener { refresh() } })
        col.addView(Button(this).apply { text = getString(R.string.diag_probe); setOnClickListener { probe() } })
        // R25: the two system pages that decide whether Android lets the keyboard use the network in the background (neither needs a permission)
        col.addView(Button(this).apply { text = getString(R.string.diag_unrestricted); setOnClickListener { openSystemPage(AndroidSettings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS) } })
        col.addView(Button(this).apply { text = getString(R.string.diag_app_settings); setOnClickListener { openSystemPage(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS) } })
        probeBody = TextView(this).apply { setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f); setTextIsSelectable(true); typeface = android.graphics.Typeface.MONOSPACE }
        col.addView(probeBody, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val scroll = ScrollView(this).apply { addView(col) }
        if (Build.VERSION.SDK_INT >= 30) scroll.setOnApplyWindowInsetsListener { v, insets ->
            val b = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()); v.setPadding(b.left, b.top, b.right, b.bottom); insets
        }
        setContentView(scroll)
        // the emulator check on GitHub (net-probe.yml) starts this screen with probe=true: only in a debug build, so no installed keyboard can be made to connect from outside
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 && intent?.getBooleanExtra("probe", false) == true) probe()
    }

    /** Opens a system settings page for THIS app; falls back to the app's own settings page, and says so when the device has neither. */
    private fun openSystemPage(action: String) {
        val data = Uri.parse("package:$packageName")
        try { startActivity(Intent(action, data)) } catch (e: Exception) {
            try { startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, data)) } catch (e2: Exception) { Toast.makeText(this, R.string.diag_no_page, Toast.LENGTH_LONG).show() }
        }
    }

    /**
     * R24a: the staged "Test translation connection" (permission, clock, name lookup, connect, secure handshake, a real answer for the fixed word "hello"),
     * on a background thread, filling in step by step. Contacts only the two translation hosts; the text of the result has no typed text and no addresses.
     */
    private fun probe() {
        if (probing) return
        probing = true
        probeText = ""
        probeBody.text = getString(R.string.diag_probe_running)
        val app = applicationContext
        val debug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        // debug build only (the emulator check of Data Saver, R25): wait first, so the test can send this screen to the background and run the connection test the way the keyboard does
        val delay = if (debug) (intent?.getIntExtra("probe_delay", 0) ?: 0).coerceIn(0, 60) else 0
        Thread({
            if (delay > 0) try { Thread.sleep(delay * 1000L) } catch (_: InterruptedException) { }
            val lines = ArrayList<String>()
            val steps = try {
                ConnectionProbe.run(app) { st ->
                    if (debug) android.util.Log.d("SwoopProbe", st.line())
                    lines.add(st.line())
                    val shown = lines.joinToString("\n")
                    runOnUiThread { probeBody.text = shown }
                }
            } catch (e: RuntimeException) { emptyList() }
            val text = if (steps.isEmpty()) getString(R.string.diag_probe_failed) else ConnectionProbe.format(steps)
            if (debug) android.util.Log.d("SwoopProbe", "done: " + text.substringAfterLast("\n"))
            runOnUiThread { probeText = text; probeBody.text = text; probing = false }
        }, "swoop-probe").also { it.isDaemon = true }.start()
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() { body.text = current() }

    private fun current(): String {
        if (Engine.safeMode) return Diagnostics.text(this, Engine.inputStats, "", false, true)     // safe mode must not even load the engine
        val host = Engine.host(this)
        return Diagnostics.text(this, Engine.inputStats, host.engine.stats.line(), host.engine.ready, false)
    }

    private fun copy() {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Swoop diagnostics", if (probeText.isEmpty()) current() else current() + "\n\n" + probeText))
        Toast.makeText(this, R.string.diag_copied, Toast.LENGTH_SHORT).show()
    }
}
