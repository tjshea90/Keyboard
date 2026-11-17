package app.swoop.keyboard

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.Toast
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * The launcher screen and the keyboard's settings screen (method.xml settingsActivity): switch Swoop on, choose it, try it. Programmatic Views
 * only (no XML inflation, no AndroidX). Later tasks add Diagnostics (KB-105), Export/Import (KB-103/205) and "go back to my previous keyboard" (KB-106).
 */
class SetupActivity : Activity() {
    private lateinit var enabledLine: TextView
    private lateinit var currentLine: TextView
    private lateinit var flow: KeyboardFlow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.setup_title)
        val d = resources.displayMetrics.density
        fun text(sp: Float, s: CharSequence, bold: Boolean = false) = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); this.text = s; if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(0, (8 * d).toInt(), 0, (4 * d).toInt())
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; val p = (16 * d).toInt(); setPadding(p, p, p, p) }
        col.addView(text(16f, getString(R.string.setup_intro)))
        col.addView(text(18f, getString(R.string.setup_step1), true))
        col.addView(Button(this).apply { text = getString(R.string.setup_step1_button); setOnClickListener { openKeyboardSettings() } })
        enabledLine = text(15f, "").also { col.addView(it) }
        col.addView(text(18f, getString(R.string.setup_step2), true))
        col.addView(Button(this).apply { text = getString(R.string.setup_step2_button); setOnClickListener { chooseKeyboard() } })
        currentLine = text(15f, "").also { col.addView(it) }
        col.addView(text(18f, getString(R.string.setup_tryit), true))
        col.addView(EditText(this).apply { hint = getString(R.string.setup_tryit_hint); minLines = 2 }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val prefs = getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        col.addView(Switch(this).apply {
            text = getString(R.string.setup_glide_switch); isChecked = prefs.getBoolean(Prefs.GLIDE, true)
            setOnCheckedChangeListener { _: CompoundButton, on: Boolean -> prefs.edit().putBoolean(Prefs.GLIDE, on).apply() }
            setPadding(0, (12 * d).toInt(), 0, (12 * d).toInt())
        })
        flow = KeyboardFlow(this, Engine.host(this), object : KeyboardFlow.Host {
            override fun say(message: String) { Toast.makeText(this@SetupActivity, message, Toast.LENGTH_LONG).show() }
            override fun ask(title: String, message: String, yes: String, no: String, result: (Boolean) -> Unit) {
                AlertDialog.Builder(this@SetupActivity).setTitle(title).setMessage(message)
                    .setPositiveButton(yes) { _, _ -> result(true) }.setNegativeButton(no) { _, _ -> result(false) }.show()
            }
        })
        col.addView(text(18f, getString(R.string.setup_learning_title), true))
        col.addView(text(14f, getString(R.string.setup_learning_text)))
        col.addView(Button(this).apply { text = getString(R.string.setup_export); setOnClickListener { flow.exportNow() } })
        col.addView(Button(this).apply { text = getString(R.string.setup_import); setOnClickListener { flow.importFromFile() } })
        col.addView(Button(this).apply { text = getString(R.string.setup_diag_button); setOnClickListener { startActivity(Intent(this@SetupActivity, DiagnosticsActivity::class.java)) } })
        col.addView(text(14f, getString(R.string.setup_fallback)))
        col.addView(text(14f, getString(R.string.setup_back)))
        val scroll = ScrollView(this).apply { addView(col) }
        // Android 15+ draws apps edge to edge: keep the content out from under the system bars (API 29 never needs this).
        if (Build.VERSION.SDK_INT >= 30) scroll.setOnApplyWindowInsetsListener { v, insets ->
            val b = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            v.setPadding(b.left, b.top, b.right, b.bottom); insets
        }
        setContentView(scroll)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!flow.onResult(requestCode, resultCode, data)) super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() { super.onResume(); refreshState() }

    private fun refreshState() {
        enabledLine.setText(if (isEnabled(this)) R.string.setup_state_enabled_yes else R.string.setup_state_enabled_no)
        currentLine.setText(if (isCurrent(this)) R.string.setup_state_current_yes else R.string.setup_state_current_no)
    }

    private fun openKeyboardSettings() {
        try { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) } catch (_: Exception) { /* the written fallback steps are on this screen */ }
    }

    private fun chooseKeyboard() {
        try { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() } catch (_: Exception) { }
    }

    companion object {
        /** True when Swoop's service is in the list of keyboards the owner switched on. */
        fun isEnabled(c: Context): Boolean = try {
            (c.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).enabledInputMethodList.any { it.packageName == c.packageName }
        } catch (_: Exception) { false }

        /** True when Swoop is the keyboard Android uses right now. */
        fun isCurrent(c: Context): Boolean = try {
            Settings.Secure.getString(c.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)?.startsWith(c.packageName + "/") == true
        } catch (_: Exception) { false }
    }
}
