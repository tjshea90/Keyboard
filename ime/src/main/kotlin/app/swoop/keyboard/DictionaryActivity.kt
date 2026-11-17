package app.swoop.keyboard

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import app.kestrel.core.input.glide.UserDictionary

/**
 * My dictionary (owner: "put in the settings a dictionary, which i can add or remove words from. Any added words will be predicted if i glide their pattern. Any removed words will never be predicted").
 * One search box: type a word (or the start of one) and the commonest words that begin with it are listed, each with a button to take it out of the keyboard for good; a word the keyboard does not
 * know gets an "Add" button. Below: the words added and the words removed, each with a way back. Every change reaches the glide engine at once and is saved in a private file.
 */
class DictionaryActivity : Activity() {
    private lateinit var st: Settings
    private lateinit var u: Ui
    private lateinit var results: LinearLayout
    private lateinit var lists: LinearLayout
    private lateinit var input: EditText
    private var added: List<String> = emptyList()
    private var blocked: List<String> = emptyList()
    private var seq = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(settingsTheme(Settings(this).theme))
        super.onCreate(savedInstanceState)
        title = getString(R.string.dict_title)
        st = Settings(this)
        u = Ui(this, st.accentColor, st.theme)
        val col = CenteredColumn(this).apply { setPadding(u.dp(16f), u.dp(8f), u.dp(16f), u.dp(40f)) }
        col.addView(u.label(getString(R.string.dict_title), 26f, u.text, true).apply { setPadding(u.dp(4f), u.dp(16f), u.dp(4f), u.dp(4f)) })
        col.addView(u.label(getString(R.string.dict_intro), 14f, u.muted).apply { setPadding(u.dp(4f), 0, u.dp(4f), u.dp(12f)) })

        input = EditText(this).apply {
            hint = getString(R.string.dict_hint); setSingleLine(); imeOptions = EditorInfo.IME_ACTION_DONE
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD   // (a plain box: no learning about the dictionary search itself)
            setTextColor(u.text); setHintTextColor(u.muted); textSize = 17f
            background = u.shape(u.card, 12f, u.stroke, 1f); setPadding(u.dp(14f), u.dp(12f), u.dp(14f), u.dp(12f))
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { search(s?.toString() ?: "") }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        }
        col.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(results, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = u.dp(8f) })
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
        Engine.host(this).engine.ensureListLoaded { refreshLists() }
    }

    private fun engine() = Engine.host(this).engine

    private fun refreshLists() { engine().dictionarySnapshot { a, b -> Io.ui { added = a; blocked = b; renderLists(); search(input.text.toString()) } } }

    private fun renderLists() {
        lists.removeAllViews()
        lists.addView(u.sectionTitle(getString(R.string.dict_added_title)))
        val ac = u.card()
        with(u) {
            if (added.isEmpty()) ac.row(note(getString(R.string.dict_added_none)))
            for (w in added) ac.row(wordRow(w, getString(R.string.dict_remove_added)) { engine().removeDictionaryWord(w) { Io.ui { refreshLists() } } })
        }
        lists.addView(ac)
        lists.addView(u.sectionTitle(getString(R.string.dict_blocked_title)))
        val bc = u.card()
        with(u) {
            if (blocked.isEmpty()) bc.row(note(getString(R.string.dict_blocked_none)))
            for (w in blocked) bc.row(wordRow(w, getString(R.string.dict_put_back)) { engine().unblockWord(w) { Io.ui { refreshLists() } } })
        }
        lists.addView(bc)
    }

    private fun wordRow(word: String, action: String, onClick: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL; setPadding(u.dp(16f), u.dp(8f), u.dp(8f), u.dp(8f))
        addView(u.label(word, 17f, u.text), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(u.button(action, filled = false, onClick = onClick), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun search(text: String) {
        val q = text.trim().lowercase()
        val my = ++seq
        if (q.isEmpty()) { results.removeAllViews(); return }
        engine().lookup(q, 8) { known, words ->
            Io.ui {
                if (my != seq) return@ui
                results.removeAllViews()
                val card = u.card()
                with(u) {
                    val exact = UserDictionary.norm(q)
                    if (exact == null) card.row(note(getString(R.string.dict_letters_only)))
                    else {
                        val isBlocked = exact in blocked; val isAdded = exact in added
                        when {
                            isBlocked -> card.row(wordRow(exact, getString(R.string.dict_put_back)) { engine().unblockWord(exact) { Io.ui { refreshLists() } } })
                            isAdded -> card.row(wordRow(exact, getString(R.string.dict_remove_added)) { engine().removeDictionaryWord(exact) { Io.ui { refreshLists() } } })
                            known -> card.row(wordRow(exact, getString(R.string.dict_remove)) { block(exact) })
                            else -> card.row(wordRow(exact, getString(R.string.dict_add)) { add(exact) })
                        }
                        for (w in words) if (w != exact && w !in blocked) card.row(wordRow(w, getString(R.string.dict_remove)) { block(w) })
                    }
                }
                results.addView(card)
            }
        }
    }

    private fun add(w: String) { engine().addDictionaryWord(w) { r -> Io.ui { if (r != UserDictionary.Result.OK) Toast.makeText(this, getString(R.string.dict_cannot_add), Toast.LENGTH_SHORT).show(); refreshLists() } } }
    private fun block(w: String) { engine().blockWord(w) { Io.ui { Toast.makeText(this, getString(R.string.dict_removed, w), Toast.LENGTH_SHORT).show(); refreshLists() } } }

    override fun onResume() { super.onResume(); refreshLists() }
}
