package app.swoop.keyboard

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import app.kestrel.core.input.editor.FieldKind

/**
 * The launcher screen and the keyboard's settings (method.xml settingsActivity). Top to bottom: where Swoop stands (switched on? chosen?), a live copy of the keyboard that follows every change made below it,
 * then the settings in groups: Typing, Learning, Layout and look, Feel, and Help. Programmatic Views only (no XML, no AndroidX); the building blocks are in [Ui]. Changes are saved the moment they are made and
 * reach the real keyboard the next time a text box is tapped.
 */
class SetupActivity : Activity() {
    private lateinit var st: Settings
    private lateinit var ui: Ui
    private lateinit var flow: KeyboardFlow
    private var scroll: ScrollView? = null
    private var preview: KeyboardRoot? = null
    private var hero: LinearLayout? = null
    private var learnedLine: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.setup_title)
        st = Settings(this)
        flow = KeyboardFlow(this, Engine.host(this), object : KeyboardFlow.Host {
            override fun say(message: String) { Toast.makeText(this@SetupActivity, message, Toast.LENGTH_LONG).show() }
            override fun ask(title: String, message: String, yes: String, no: String, result: (Boolean) -> Unit) {
                AlertDialog.Builder(this@SetupActivity).setTitle(title).setMessage(message)
                    .setPositiveButton(yes) { _, _ -> result(true) }.setNegativeButton(no) { _, _ -> result(false) }.show()
            }
        })
        build()
    }

    /** (Re)builds the whole screen: the first time, and again when the colours change. */
    private fun build() {
        val keepAt = scroll?.scrollY ?: 0
        ui = Ui(this, st.accentColor, st.theme)
        val u = ui
        val col = CenteredColumn(this).apply { setPadding(u.dp(16f), u.dp(8f), u.dp(16f), u.dp(40f)) }

        // ---- where Swoop stands
        val h = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        hero = h
        col.addView(h, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = u.dp(8f) })

        // ---- the live keyboard
        col.addView(u.sectionTitle("Try it"))
        val tryCard = u.card()
        tryCard.addView(u.note("This is the real keyboard, drawn with your settings. Tap, slide over the letters to see the trail, and change anything below to watch it change here."))
        val root = KeyboardRoot(this).also { r -> r.keys.setGlideEnabled(true); r.keys.setField(FieldKind.TEXT, "⏎"); r.keys.applyOptions(st) }
        preview = root
        tryCard.addView(root, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = u.dp(6f); rightMargin = u.dp(6f); bottomMargin = u.dp(6f) })
        with(u) {
            tryCard.row(EditText(this@SetupActivity).apply {
                hint = "Tap here to type with your real keyboard"; setHintTextColor(muted); setTextColor(text); minLines = 2
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                background = null; setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
            })
        }
        col.addView(tryCard)

        // ---- typing
        col.addView(u.sectionTitle("Typing"))
        val typing = u.card()
        with(u) {
            typing.row(switchRow("Glide typing", "Slide your finger over the letters to type a word.", st.glide) { change(Prefs.GLIDE, it) })
            typing.row(switchRow("Glide trail", "A glowing line follows your finger while you glide.", st.trail) { change(Prefs.TRAIL, it) })
            typing.row(switchRow("Word suggestions", "Words appear above the keys while you tap, even with glide typing off. Tap one to use it.", st.suggest) { change(Prefs.SUGGEST, it) })
            typing.row(switchRow("Fix typos automatically", "Swaps an obvious typo for the right word when you press space. Backspace right away puts your word back, and it is never changed again.", st.autoFix) { change(Prefs.AUTOFIX, it) })
            typing.row(switchRow("Capital I", "Types i, i'm and i'll as I, I'm and I'll.", st.capitalizeI) { change(Prefs.CAPI, it) })
            typing.row(switchRow("Capital letters", "Starts each sentence with a capital, where the app asks for it.", st.autoCaps) { change(Prefs.AUTOCAPS, it) })
            typing.row(switchRow("Space after punctuation", "Adds the space for you when a letter follows a comma, full stop, ? or !. Not in addresses, numbers or abbreviations.", st.autoSpace) { change(Prefs.AUTOSPACE, it) })
            typing.row(switchRow("Double space for a full stop", "Press space twice to end a sentence with a full stop.", st.doubleSpace) { change(Prefs.DOUBLESPACE, it) })
            typing.row(switchRow("Slide on the space bar", "Move the text cursor by sliding your finger along the space bar.", st.spaceSlide) { change(Prefs.SPACESLIDE, it) })
        }
        col.addView(typing)

        // ---- learning
        col.addView(u.sectionTitle("Learning"))
        val learn = u.card()
        with(u) {
            learn.row(switchRow("Learn from my typing", "Remembers your words, the words you use together and the corrections you make, in every app. It stays on this device.", st.learn) { change(Prefs.LEARN, it) })
            val line = label("", 13f, accent, true).apply { setPadding(dp(16f), dp(12f), dp(16f), dp(12f)); text = "Reading what it knows…" }
            learnedLine = line
            learn.row(line)
            learn.row(note("Nothing is learned in password boxes, or in apps that ask for private typing."))
            learn.row(actionRow("Export what it learned", "Saves one plain-text file in Downloads. It lists your words: keep it private.") { flow.exportNow() })
            learn.row(actionRow("Import what it learned", "Adds a file to what is learned, including the file the Kestrel browser exported.") { flow.importFromFile() })
            learn.row(actionRow("Forget what it learned", "Deletes your words, pairs and corrections from this device.", danger = true) { flow.forgetNow(); refreshLearned(delay = true) })
        }
        col.addView(learn)

        // ---- layout and look
        col.addView(u.sectionTitle("Layout and look"))
        val look = u.card()
        with(u) {
            look.row(switchRow("Number row", "Keep the digits 1 to 0 above the letters at all times.", st.numberRow) { change(Prefs.NUMBERROW, it) })
            look.row(choiceRow("Key height", null, Settings.KEY_HEIGHTS, st.keyHeight) { change(Prefs.KEYHEIGHT, it) })
            look.row(choiceRow("Colours", "Automatic follows your phone's light or dark setting.", Settings.THEMES, st.theme) { change(Prefs.THEME, it, rebuild = true) })
            look.row(accentRow("Accent colour", "The glide trail, the lit keys and this screen.", Settings.ACCENTS, st.accent) { change(Prefs.ACCENT, it, rebuild = true) })
        }
        col.addView(look)

        // ---- feel
        col.addView(u.sectionTitle("Feel"))
        val feel = u.card()
        with(u) {
            feel.row(switchRow("Key pop-up", "Shows the letter above your finger while you press a key.", st.keyPreview) { change(Prefs.PREVIEW, it) })
            feel.row(switchRow("Vibrate on key press", null, st.haptics) { change(Prefs.HAPTIC, it) })
            feel.row(switchRow("Sound on key press", "Uses your phone's key click sound; it is silent when the phone's touch sounds are off.", st.sound) { change(Prefs.SOUND, it) })
        }
        col.addView(feel)

        // ---- help
        col.addView(u.sectionTitle("Help"))
        val help = u.card()
        with(u) {
            help.row(actionRow("Go back to my previous keyboard", "Opens Android's keyboard picker. Your old keyboard stays installed.") { chooseKeyboard() })
            help.row(actionRow("Diagnostics", "Numbers only, nothing you typed. Copy them into the chat when something is wrong.") { startActivity(Intent(this@SetupActivity, DiagnosticsActivity::class.java)) })
            help.row(actionRow("Reset settings", "Puts every switch on this screen back as it was at install. What the keyboard learned stays.", danger = true) {
                AlertDialog.Builder(this@SetupActivity).setTitle("Reset settings?").setMessage("Every switch goes back to its install setting. Learned words are kept.")
                    .setPositiveButton("Reset") { _, _ -> st.reset(); build() }.setNegativeButton("Cancel", null).show()
            })
            help.row(note("If the buttons at the top do not open the right screen: Settings > System > Languages and input > On-screen keyboard > Manage keyboards, switch Swoop on; then tap in any text box and use the keyboard icon at the bottom right to pick Swoop."))
            help.row(note("Swoop ${versionName()}. No accounts, no ads, no tracking, and no internet access: what you type never leaves this device unless you export it yourself."))
        }
        col.addView(help)

        val frame = FrameLayout(this).apply { centered(col) }
        val sv = ScrollView(this).apply { addView(frame); setBackgroundColor(u.bg); isFillViewport = true; clipToPadding = false }
        scroll = sv
        // Android 15+ draws apps edge to edge: keep the content out from under the system bars (API 29 never needs this).
        if (Build.VERSION.SDK_INT >= 30) sv.setOnApplyWindowInsetsListener { v, insets ->
            val b = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            v.setPadding(b.left, b.top, b.right, b.bottom); insets
        }
        setContentView(sv)
        window.statusBarColor = u.bg; window.navigationBarColor = u.bg
        sv.systemUiVisibility = if (u.dark) 0 else (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or (if (Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0))
        sv.post { sv.scrollTo(0, keepAt) }
        refreshState(); refreshLearned()
    }

    private fun change(key: String, v: Boolean) { st.set(key, v); preview?.keys?.applyOptions(st); if (key == Prefs.GLIDE) preview?.keys?.setGlideEnabled(true) }
    private fun change(key: String, v: Int, rebuild: Boolean = false) { st.set(key, v); if (rebuild) build() else preview?.keys?.applyOptions(st) }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!flow.onResult(requestCode, resultCode, data)) super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() { super.onResume(); refreshState(); refreshLearned() }

    /** The card at the top: the one next step, or "ready". */
    private fun refreshState() {
        val h = hero ?: return
        val u = ui
        h.removeAllViews()
        val enabled = isEnabled(this); val current = isCurrent(this)
        val dark = (u.accent and 0x00FFFFFF).let { c -> Color.rgb((Color.red(c) * 0.45f).toInt(), (Color.green(c) * 0.45f).toInt(), (Color.blue(c) * 0.45f).toInt()) }
        h.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(u.accent, dark)).apply { cornerRadius = u.dp(22f).toFloat() }
        h.setPadding(u.dp(22f), u.dp(22f), u.dp(22f), u.dp(22f))
        h.addView(u.label("Swoop", 30f, Color.WHITE, true))
        h.addView(u.label("Glide. Tap. It learns you.", 15f, 0xDDFFFFFF.toInt()).apply { setPadding(0, u.dp(4f), 0, u.dp(14f)) })
        fun action(text: String, run: () -> Unit) = TextView(this).apply {
            this.text = text; setTextColor(u.accent); textSize = 16f; typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            gravity = Gravity.CENTER; minHeight = u.dp(52f); background = u.shape(Color.WHITE, 14f)
            isClickable = true; isFocusable = true; setOnClickListener { run() }
        }
        when {
            !enabled -> {
                h.addView(u.label("Step 1 of 2: switch Swoop on", 16f, Color.WHITE, true))
                h.addView(u.label("Android only uses a keyboard after you switch it on. Your old keyboard stays installed.", 14f, 0xDDFFFFFF.toInt()).apply { setPadding(0, u.dp(4f), 0, u.dp(14f)) })
                h.addView(action("Open keyboard settings") { openKeyboardSettings() }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
            !current -> {
                h.addView(u.label("Step 2 of 2: choose Swoop", 16f, Color.WHITE, true))
                h.addView(u.label("Pick Swoop from the list. You can switch back the same way, any time.", 14f, 0xDDFFFFFF.toInt()).apply { setPadding(0, u.dp(4f), 0, u.dp(14f)) })
                h.addView(action("Choose keyboard") { chooseKeyboard() }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
            else -> {
                h.addView(u.label("✓  Swoop is your keyboard", 17f, Color.WHITE, true))
                h.addView(u.label("Tap any text box to use it. Everything below applies the next time the keyboard opens.", 14f, 0xDDFFFFFF.toInt()).apply { setPadding(0, u.dp(4f), 0, 0) })
            }
        }
    }

    /** "It knows N words ...", read from the saved memory off the UI thread. */
    private fun refreshLearned(delay: Boolean = false) {
        val line = learnedLine ?: return
        Io.exec {
            if (delay) try { Thread.sleep(600) } catch (_: Exception) { }
            val s = try { Engine.host(this).learnedSummary() } catch (_: Exception) { "" }
            Io.ui { if (!isDestroyed) line.text = if (s.startsWith("Nothing")) s else "It knows $s" }
        }
    }

    private fun versionName(): String = try { packageManager.getPackageInfo(packageName, 0).versionName ?: "" } catch (_: Exception) { "" }

    private fun openKeyboardSettings() {
        try { startActivity(Intent(AndroidSettings.ACTION_INPUT_METHOD_SETTINGS)) } catch (_: Exception) { /* the written fallback steps are on this screen */ }
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
            AndroidSettings.Secure.getString(c.contentResolver, AndroidSettings.Secure.DEFAULT_INPUT_METHOD)?.startsWith(c.packageName + "/") == true
        } catch (_: Exception) { false }
    }
}
