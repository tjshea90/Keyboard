package app.swoop.keyboard

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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

/** KB-105: counters and timings, with a Copy button, for pasting into a chat. Numbers only (see [Diagnostics]). Not exported: opened from the Setup screen. */
class DiagnosticsActivity : Activity() {
    private lateinit var body: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.diag_title)
        val d = resources.displayMetrics.density
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; val p = (16 * d).toInt(); setPadding(p, p, p, p) }
        body = TextView(this).apply { setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f); setTextIsSelectable(true); typeface = android.graphics.Typeface.MONOSPACE }
        col.addView(body, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(Button(this).apply { text = getString(R.string.diag_copy); setOnClickListener { copy() } })
        col.addView(Button(this).apply { text = getString(R.string.diag_refresh); setOnClickListener { refresh() } })
        val scroll = ScrollView(this).apply { addView(col) }
        if (Build.VERSION.SDK_INT >= 30) scroll.setOnApplyWindowInsetsListener { v, insets ->
            val b = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()); v.setPadding(b.left, b.top, b.right, b.bottom); insets
        }
        setContentView(scroll)
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() { body.text = current() }

    private fun current(): String {
        if (Engine.safeMode) return Diagnostics.text(this, Engine.inputStats, "", false, true)     // safe mode must not even load the engine
        val host = Engine.host(this)
        return Diagnostics.text(this, Engine.inputStats, host.engine.stats.line(), host.engine.ready, false)
    }

    private fun copy() {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Swoop diagnostics", current()))
        Toast.makeText(this, R.string.diag_copied, Toast.LENGTH_SHORT).show()
    }
}
