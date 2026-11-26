package app.swoop.keyboard

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import app.kestrel.core.input.glide.GlideKeys
import app.kestrel.core.input.glide.GlideTraining

/**
 * Glide training (R36, owner 2026-11-26: "Can you make a much longer glide training that I can glide full paragraphs and problematic words several times and the keyboard learns how I glide?"): the long sibling of
 * [PracticeActivity]. The person glides everyday paragraphs word by word and then short pages of look-alike words, each word several times ([GlideTraining] is the script). The text box holds the words written so far, so
 * the keyboard decodes every glide with the real words before it, as in a message. Each glide is kept with the word that was asked for (same private file as the drill, format 2) and, while the switch is on, handed
 * back to the engine as a lesson ([Practice.Sink.glide] returns the asked word; [GlideTeaching] says what is learned: the hand, never the words). After every page the score of that page shows; the last page repeats the
 * words that went wrong together with what was typed instead. Only the prompted words are kept, and only while this screen is open.
 */
class TrainingActivity : Activity(), Practice.Sink {
    private lateinit var st: Settings
    private lateinit var u: Ui
    private lateinit var scroll: ScrollView
    private lateinit var body: LinearLayout
    private lateinit var input: EditText
    private var training: GlideTraining? = null
    private var teaching = true
    private var ownWords = ""
    private var taught = 0
    private var saved = false
    private var keys: GlideKeys? = null
    private var viewW = 0
    private var viewH = 0
    private var shown = -1
    private var pageText: TextView? = null
    private var progress: TextView? = null
    private var wordLine: TextView? = null

    internal val trainingForTest: GlideTraining? get() = training

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(settingsTheme(Settings(this).theme))
        super.onCreate(savedInstanceState)
        title = getString(R.string.training_title)
        st = Settings(this)
        u = Ui(this, st.accentColor, st.theme)
        val col = CenteredColumn(this).apply { setPadding(u.dp(16f), u.dp(8f), u.dp(16f), u.dp(40f)) }
        col.addView(u.label(getString(R.string.training_title), 26f, u.text, true).apply { setPadding(u.dp(4f), u.dp(16f), u.dp(4f), u.dp(4f)) })
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        input = EditText(this).apply {
            hint = getString(R.string.training_hint); setSingleLine(); imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or EditorInfo.IME_ACTION_NONE
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            privateImeOptions = Practice.OPTION                                                    // the keyboard hands over glides of THIS box only
            setTextColor(u.text); setHintTextColor(u.muted); textSize = 20f
            background = u.shape(u.card, 12f, u.stroke, 1f); setPadding(u.dp(14f), u.dp(12f), u.dp(14f), u.dp(12f))
        }
        val frame = FrameLayout(this).apply { centered(col) }
        scroll = ScrollView(this).apply { addView(frame); setBackgroundColor(u.bg); isFillViewport = true; clipToPadding = false }
        if (Build.VERSION.SDK_INT >= 30) scroll.setOnApplyWindowInsetsListener { v, insets ->
            val b = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime())
            v.setPadding(b.left, b.top, b.right, b.bottom); insets
        }
        setContentView(scroll)
        window.statusBarColor = u.bg; window.navigationBarColor = u.bg
        scroll.systemUiVisibility = if (u.dark) 0 else (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or (if (Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0))
        if (Build.VERSION.SDK_INT >= 33) onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT) { confirmLeave() }
        render()
    }

    override fun onResume() {
        super.onResume()
        Practice.sink = this
        focusBox()
    }

    override fun onPause() { if (Practice.sink === this) Practice.sink = null; super.onPause() }

    /** Android 12 and older (the tablet): the back key. Android 13 and newer (the phone) call the callback registered in [onCreate] instead, predictive back included. */
    @Suppress("OVERRIDE_DEPRECATION")
    @android.annotation.SuppressLint("GestureBackNavigation")
    override fun onBackPressed() { confirmLeave() }

    /** Leaving with glides that were not saved asks first (twenty minutes of gliding must not be lost by a slip of the thumb). */
    private fun confirmLeave() {
        val t = training
        if (t == null || t.answered < 10 || saved) { finish(); return }
        AlertDialog.Builder(this).setTitle("Leave without saving?").setMessage("${t.answered} glides were recorded and not saved to a file yet.")
            .setPositiveButton("Save and leave") { _, _ -> save(); finish() }.setNegativeButton("Leave", { _, _ -> finish() }).setNeutralButton("Stay", null).show()
    }

    private fun focusBox() {
        if (input.parent == null || input.visibility != View.VISIBLE) return
        input.requestFocus()
        input.post { try { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(input, 0) } catch (_: Exception) { } }
    }

    // ------------------------------------------------------------------ the keyboard's side

    /** A glide of the box: kept with the word that was asked for (and the words the keyboard decoded it with); the next word is shown. Returns the asked word when the keyboard should learn from this glide. */
    override fun glide(xs: FloatArray, ys: FloatArray, ts: FloatArray, n: Int, offered: List<String>, keys: GlideKeys?, viewW: Int, viewH: Int, prev: String?, prev2: String?): String? {
        val t = training ?: return null
        if (t.done || t.atBreak) return null
        if (keys != null) { this.keys = keys; this.viewW = viewW; this.viewH = viewH }
        val asked = t.record(xs, ys, ts, n, offered, prev, prev2) ?: return null
        saved = false
        if (teaching) taught++
        afterAnswer()
        return if (teaching) asked else null
    }

    private fun afterAnswer() {
        val t = training ?: return
        if (t.done || t.atBreak) { input.setText(""); render(); return }
        setBox(t.boxText())
        updatePage()
    }

    private fun setBox(text: String) { input.setText(text); input.setSelection(input.text.length) }

    // ------------------------------------------------------------------ what is shown

    private fun stateOf(): Int { val t = training; return if (t == null) S_START else if (t.done) S_END else if (t.atBreak) S_BREAK else S_PAGE }

    private fun render() {
        val state = stateOf()
        if (state == S_PAGE && shown == S_PAGE && pageText != null) { updatePage(); return }
        shown = state
        pageText = null; progress = null; wordLine = null
        (input.parent as? ViewGroup)?.removeView(input)
        body.removeAllViews()
        when (state) {
            S_START -> buildStart()
            S_PAGE -> buildPage()
            S_BREAK -> buildBreak()
            else -> buildEnd()
        }
        scroll.post { scroll.scrollTo(0, 0) }
        if (state == S_PAGE) focusBox()
    }

    private fun buildStart() {
        val card = u.card()
        with(u) {
            card.row(note(getString(R.string.training_intro)))
            card.row(switchRow(getString(R.string.training_learn_title), getString(R.string.training_learn_note), teaching) { teaching = it })
            val own = LinearLayout(this@TrainingActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16f), dp(12f), dp(16f), dp(12f)) }
            own.addView(label(getString(R.string.training_own_title), 16f, text, true))
            val et = EditText(this@TrainingActivity).apply {
                hint = getString(R.string.training_own_hint); setSingleLine(); setText(ownWords); setTextColor(u.text); setHintTextColor(u.muted); textSize = 16f
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                background = u.shape(u.card, 10f, u.stroke, 1f); setPadding(u.dp(12f), u.dp(8f), u.dp(12f), u.dp(8f))
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun afterTextChanged(s: Editable?) { ownWords = s?.toString() ?: "" }
                })
            }
            own.addView(et, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8f) })
            own.addView(note(getString(R.string.training_own_note)).apply { setPadding(0, dp(8f), 0, 0) })
            card.row(own)
        }
        body.addView(card)
        body.addView(u.button(getString(R.string.training_start)) { start() }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = u.dp(16f) })
    }

    private fun start() {
        training = GlideTraining(extraWords = GlideTraining.cleanWords(ownWords))
        taught = 0; saved = false; keys = null
        input.setText("")
        render()
    }

    private fun buildPage() {
        val t = training ?: return
        progress = u.label("", 13f, u.muted, true).apply { setPadding(u.dp(4f), u.dp(4f), u.dp(4f), 0) }
        wordLine = u.label("", 13f, u.muted).apply { setPadding(u.dp(4f), 0, u.dp(4f), u.dp(8f)) }
        body.addView(progress); body.addView(wordLine)
        val card = u.card()
        pageText = TextView(this).apply { textSize = 20f; setLineSpacing(u.dp(4f).toFloat(), 1f); setTextColor(u.text); setPadding(u.dp(16f), u.dp(14f), u.dp(16f), u.dp(14f)) }
        with(u) { card.row(pageText!!, divider = false) }
        body.addView(card)
        body.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = u.dp(12f) })
        input.visibility = View.VISIBLE
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(u.button(getString(R.string.training_skip), filled = false) { training?.let { it.skip(); afterAnswer() } })
        buttons.addView(u.button(getString(R.string.training_stop), filled = false) { training?.finishNow(); input.setText(""); render() }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = u.dp(12f) })
        body.addView(buttons, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = u.dp(8f) })
        setBox(t.boxText())
        updatePage()
    }

    /** The paragraph or drill list with every word coloured by how it went and the one to glide now marked; the counters above it. */
    private fun updatePage() {
        val t = training ?: return
        val pg = t.page ?: return
        progress?.text = getString(R.string.training_page, t.pageIndex + 1, t.pageCount, pg.title)
        wordLine?.text = getString(R.string.training_word, t.pagePos + 1, pg.prompts, t.answered)
        val sb = SpannableStringBuilder()
        var curStart = -1
        val good = if (u.dark) 0xFF81C784.toInt() else 0xFF2E7D32.toInt()
        val bad = if (u.dark) 0xFFEF9A9A.toInt() else 0xFFC62828.toInt()
        val near = if (u.dark) 0xFFFFB74D.toInt() else 0xFFEF6C00.toInt()
        val sep = if (pg.kind == GlideTraining.Kind.TEXT) " " else "   "
        for ((i, tok) in pg.tokens.withIndex()) {
            val s = sb.length
            sb.append(tok.shown)
            val e = sb.length
            val color = when (tok.state) {
                GlideTraining.RIGHT -> good; GlideTraining.NEAR -> near; GlideTraining.MISS -> bad
                GlideTraining.SKIPPED, GlideTraining.FREE -> u.muted
                else -> u.text
            }
            if (i == t.tokenIndex) {
                curStart = s
                sb.setSpan(BackgroundColorSpan(u.accent), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(ForegroundColorSpan(u.onAccent), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(StyleSpan(android.graphics.Typeface.BOLD), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                sb.setSpan(ForegroundColorSpan(color), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (tok.state == GlideTraining.MISS || tok.state == GlideTraining.NEAR) sb.setSpan(StyleSpan(android.graphics.Typeface.BOLD), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            sb.append(sep)
        }
        val tv = pageText ?: return
        tv.text = sb
        if (curStart >= 0) tv.post {
            val l = tv.layout ?: return@post
            val line = l.getLineForOffset(curStart)
            tv.requestRectangleOnScreen(Rect(0, l.getLineTop(line) - u.dp(24f), tv.width, l.getLineBottom(line) + u.dp(72f)), false)
        }
    }

    private fun scoreLine(right: Int, n: Int, three: Int, page: Boolean): String =
        getString(if (page) R.string.training_page_score else R.string.training_total, right, n, if (n > 0) 100 * right / n else 0, three)

    private fun missText(list: List<Pair<String, String>>): String =
        list.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(12).joinToString(", ") { it.key.first + " → " + it.key.second.ifEmpty { "?" } + (if (it.value > 1) " ×" + it.value else "") }

    private fun buildBreak() {
        val t = training ?: return
        val p = t.breakPage
        val sc = t.score(p)
        val card = u.card()
        with(u) {
            card.row(label(getString(R.string.training_page_done, p + 1), 20f, text, true).apply { setPadding(dp(16f), dp(14f), dp(16f), dp(2f)) })
            if (sc.answered == 0) card.row(note(getString(R.string.practice_nothing)))
            else {
                card.row(label(scoreLine(sc.right, sc.answered, sc.inThree, true), 16f, text).apply { setPadding(dp(16f), dp(6f), dp(16f), dp(6f)) })
                val tag = "p${p + 1}"
                val miss = t.practice.entries.filter { it.tag == tag && it.got.firstOrNull() != it.word }.map { it.word to (it.got.firstOrNull() ?: "") }
                if (miss.isNotEmpty()) card.row(note(getString(R.string.practice_misses, missText(miss))))
            }
            val nextPage = t.pages.getOrNull(t.pageIndex)
            if (nextPage != null && nextPage.kind == GlideTraining.Kind.MISSED) card.row(note(getString(R.string.training_missed_page)))
            if (t.answered > 0) card.row(actionRow(getString(R.string.training_save_now), getString(R.string.practice_save_note)) { save() })
        }
        body.addView(card)
        body.addView(u.button(getString(R.string.training_next)) { training?.continueNext(); setBox(training?.boxText() ?: ""); render() }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = u.dp(16f) })
        body.addView(u.button(getString(R.string.training_stop), filled = false) { training?.finishNow(); render() }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = u.dp(8f) })
    }

    private fun buildEnd() {
        val t = training ?: return
        val n = t.answered
        val card = u.card()
        with(u) {
            card.row(label(getString(R.string.training_all_done), 20f, text, true).apply { setPadding(dp(16f), dp(14f), dp(16f), dp(2f)) })
            if (n == 0) card.row(note(getString(R.string.practice_nothing)))
            else {
                val tot = t.totalScore()
                card.row(label(scoreLine(tot.right, tot.answered, tot.inThree, false), 16f, text).apply { setPadding(dp(16f), dp(6f), dp(16f), dp(6f)) })
                card.row(note(if (teaching) getString(R.string.training_taught, taught) else getString(R.string.training_not_taught)))
                for (i in 0 until t.pageCount) { val s = t.score(i); if (s.answered > 0) card.row(note(getString(R.string.training_per_page, i + 1, s.right, s.answered))) }
                val miss = t.practice.misses()
                card.row(note(if (miss.isEmpty()) getString(R.string.practice_no_misses) else getString(R.string.practice_misses, missText(miss))))
                card.row(note(getString(R.string.practice_attach)))
                card.row(actionRow(getString(R.string.practice_save), getString(R.string.practice_save_note)) { save() })
            }
            card.row(actionRow(getString(R.string.training_restart), null) { training = null; input.setText(""); render() })
        }
        body.addView(card)
    }

    /** One plain-text file in Downloads (no picker, no permission), the way the learning export is written. */
    private fun save() {
        val t = training
        val k = keys
        if (t == null || k == null || viewW <= 0 || t.answered == 0) { Toast.makeText(this, getString(R.string.practice_nothing), Toast.LENGTH_SHORT).show(); return }
        val text = t.serialize(k, viewW, viewH, resources.displayMetrics.density, PracticeFile.version(this), teaching)
        val name = PracticeFile.name("training")
        val err = PracticeFile.save(this, name, text)
        if (err == null) { saved = true; Toast.makeText(this, getString(R.string.practice_saved, name), Toast.LENGTH_LONG).show() }
        else Toast.makeText(this, getString(R.string.practice_save_failed, err), Toast.LENGTH_LONG).show()
    }

    private companion object { const val S_START = 0; const val S_PAGE = 1; const val S_BREAK = 2; const val S_END = 3 }
}
