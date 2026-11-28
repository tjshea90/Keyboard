package app.swoop.keyboard

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import app.kestrel.core.input.glide.EmailBook

/**
 * My e-mail addresses (R34b, owner 2026-11-26: "it still isn't recognizing email fields and suggesting my email addresses at the top"). The keyboard learns the addresses typed by hand, but a new keyboard knows none: here the person types
 * theirs once, and they are offered above the keys in every e-mail box (and finish an address typed after the @). Each address has a way out. Everything stays on this device, in the same private file the learned addresses use.
 */
class EmailsActivity : Activity() {
    private lateinit var st: Settings
    private lateinit var u: Ui
    private lateinit var lists: LinearLayout
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(settingsTheme(Settings(this).theme))
        super.onCreate(savedInstanceState)
        title = getString(R.string.emails_title)
        st = Settings(this)
        u = Ui(this, st.accentColor, st.theme)
        val col = CenteredColumn(this).apply { setPadding(u.dp(16f), u.dp(8f), u.dp(16f), u.dp(40f)) }
        col.addView(u.label(getString(R.string.emails_title), 26f, u.text, true).apply { setPadding(u.dp(4f), u.dp(16f), u.dp(4f), u.dp(4f)) })
        col.addView(u.label(getString(R.string.emails_intro), 14f, u.muted).apply { setPadding(u.dp(4f), 0, u.dp(4f), u.dp(12f)) })

        input = EditText(this).apply {
            hint = getString(R.string.emails_hint); setSingleLine(); imeOptions = EditorInfo.IME_ACTION_DONE
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setTextColor(u.text); setHintTextColor(u.muted); textSize = 17f
            background = u.shape(u.card, 12f, u.stroke, 1f); setPadding(u.dp(14f), u.dp(12f), u.dp(14f), u.dp(12f))
            setOnEditorActionListener { _, _, _ -> add(); true }
        }
        col.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(u.button(getString(R.string.emails_add), filled = true) { add() }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = u.dp(8f) })
        lists = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(lists, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = u.dp(12f) })

        val frame = FrameLayout(this).apply { centered(col) }
        val sv = ScrollView(this).apply { addView(frame); setBackgroundColor(u.bg); isFillViewport = true; clipToPadding = false }
        if (Build.VERSION.SDK_INT >= 30) sv.setOnApplyWindowInsetsListener { v, insets ->
            val b = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime())
            v.setPadding(b.left, b.top, b.right, b.bottom); insets
        }
        setContentView(sv)
        window.statusBarColor = u.bg; window.navigationBarColor = u.bg
        sv.systemUiVisibility = if (u.dark) 0 else (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or (if (Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0))
        render()
    }

    private fun host() = Engine.host(this)

    /** A typed address goes into the book as if it had been typed three times, so it leads the strip from the first day and is not the first to go when the book is full. */
    private fun add() {
        val a = input.text.toString().trim().lowercase()
        if (!EmailBook.isAddress(a)) { Toast.makeText(this, getString(R.string.emails_bad), Toast.LENGTH_SHORT).show(); return }
        host().emails.learn(a, uses = 3); host().saveEmails()
        input.setText("")
        Toast.makeText(this, getString(R.string.emails_added, a), Toast.LENGTH_SHORT).show()
        render()
    }

    private fun remove(a: String) { host().emails.forget(a); host().saveEmails(); render() }

    private fun render() {
        lists.removeAllViews()
        lists.addView(u.sectionTitle(getString(R.string.emails_list_title)))
        val card = u.card()
        val all = host().emails.addresses()
        with(u) {
            if (all.isEmpty()) card.row(note(getString(R.string.emails_none)))
            for (a in all) card.row(addressRow(a))
        }
        lists.addView(card)
    }

    private fun addressRow(address: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL; setPadding(u.dp(16f), u.dp(8f), u.dp(8f), u.dp(8f))
        addView(u.label(address, 17f, u.text), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(u.button(getString(R.string.emails_remove), filled = false) { remove(address) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    override fun onResume() { super.onResume(); render() }
}
