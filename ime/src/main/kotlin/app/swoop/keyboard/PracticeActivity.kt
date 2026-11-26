package app.swoop.keyboard

import android.app.Activity
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
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
import app.kestrel.core.input.glide.GlidePractice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Glide practice (R34a2, owner 2026-11-26: "It still gets a lot of glide predictions wrong"): public data cannot say how glides look on THIS tablet, so the person glides 40 words that are shown one at a time, with this keyboard,
 * in a box that learns nothing. The keyboard hands over each glide ([Practice]) while this screen is open and only for this box: the path of the finger and the words offered, kept with the word that was asked for. The screen shows
 * how many were right and which words were missed, and can save one plain-text file into Downloads for the person to ATTACH to the chat (never pasted: it holds finger paths). Nothing typed anywhere else is ever recorded.
 */
class PracticeActivity : Activity(), Practice.Sink {
    private lateinit var st: Settings
    private lateinit var u: Ui
    private lateinit var wordLabel: TextView
    private lateinit var progressLabel: TextView
    private lateinit var resultBox: LinearLayout
    private lateinit var input: EditText
    private var practice = GlidePractice()
    private var keys: GlideKeys? = null
    private var viewW = 0
    private var viewH = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(settingsTheme(Settings(this).theme))
        super.onCreate(savedInstanceState)
        title = getString(R.string.practice_title)
        st = Settings(this)
        u = Ui(this, st.accentColor, st.theme)
        val col = CenteredColumn(this).apply { setPadding(u.dp(16f), u.dp(8f), u.dp(16f), u.dp(40f)) }
        col.addView(u.label(getString(R.string.practice_title), 26f, u.text, true).apply { setPadding(u.dp(4f), u.dp(16f), u.dp(4f), u.dp(4f)) })
        col.addView(u.label(getString(R.string.practice_intro), 14f, u.muted).apply { setPadding(u.dp(4f), 0, u.dp(4f), u.dp(12f)) })
        progressLabel = u.label("", 13f, u.muted, true).apply { setPadding(u.dp(4f), u.dp(4f), u.dp(4f), 0) }
        col.addView(progressLabel)
        wordLabel = u.label("", 40f, u.accent, true).apply { setPadding(u.dp(4f), u.dp(4f), u.dp(4f), u.dp(8f)) }
        col.addView(wordLabel)
        input = EditText(this).apply {
            hint = getString(R.string.practice_hint); setSingleLine(); imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING or EditorInfo.IME_ACTION_NONE
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            privateImeOptions = Practice.OPTION                                                    // the keyboard hands over glides of THIS box only
            setTextColor(u.text); setHintTextColor(u.muted); textSize = 20f
            background = u.shape(u.card, 12f, u.stroke, 1f); setPadding(u.dp(14f), u.dp(12f), u.dp(14f), u.dp(12f))
        }
        col.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(u.button(getString(R.string.practice_skip), filled = false) { if (!practice.done) { practice.skip(); input.setText(""); render() } })
        buttons.addView(u.button(getString(R.string.practice_finish), filled = false) { practice.finishNow(); render() }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { leftMargin = u.dp(12f) })
        col.addView(buttons, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = u.dp(8f) })
        resultBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(resultBox, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = u.dp(12f) })

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

    override fun onResume() {
        super.onResume()
        Practice.sink = this
        input.requestFocus()
        input.post { try { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(input, 0) } catch (_: Exception) { } }
    }

    override fun onPause() { if (Practice.sink === this) Practice.sink = null; super.onPause() }

    /** A glide of the practice box: kept with the word that was asked for; the box is emptied and the next word shown. */
    override fun glide(xs: FloatArray, ys: FloatArray, ts: FloatArray, n: Int, offered: List<String>, keys: GlideKeys?, viewW: Int, viewH: Int) {
        if (practice.done) return
        if (keys != null) { this.keys = keys; this.viewW = viewW; this.viewH = viewH }
        practice.record(xs, ys, ts, n, offered)
        input.setText("")
        render()
    }

    private fun render() {
        resultBox.removeAllViews()
        if (!practice.done) {
            progressLabel.text = getString(R.string.practice_progress, practice.index + 1, practice.total)
            wordLabel.text = practice.current ?: ""
            return
        }
        progressLabel.text = getString(R.string.practice_done)
        wordLabel.text = "✓"
        val n = practice.answered
        val card = u.card()
        with(u) {
            if (n == 0) card.row(note(getString(R.string.practice_nothing)))
            else {
                card.row(label(getString(R.string.practice_score, practice.firstRight(), n, 100 * practice.firstRight() / n, practice.inThree()), 17f, text, true).apply { setPadding(dp(16f), dp(12f), dp(16f), dp(4f)) })
                val miss = practice.misses()
                card.row(note(if (miss.isEmpty()) getString(R.string.practice_no_misses) else getString(R.string.practice_misses, miss.joinToString(", ") { it.first + " → " + it.second })))
                card.row(note(getString(R.string.practice_attach)))
                card.row(actionRow(getString(R.string.practice_save), getString(R.string.practice_save_note)) { save() })
            }
            card.row(actionRow(getString(R.string.practice_again), null) { practice = GlidePractice(); input.setText(""); render() })
        }
        resultBox.addView(card)
    }

    /** One plain-text file in Downloads (no picker, no permission), the way the learning export is written. */
    private fun save() {
        val k = keys
        if (k == null || viewW <= 0) { Toast.makeText(this, getString(R.string.practice_nothing), Toast.LENGTH_SHORT).show(); return }
        val version = try { packageManager.getPackageInfo(packageName, 0).versionName ?: "" } catch (_: Exception) { "" }
        val text = practice.serialize(k, viewW, viewH, resources.displayMetrics.density, version)
        val name = "Swoop glide practice " + SimpleDateFormat("yyyy-MM-dd HHmm", Locale.US).format(Date()) + ".txt"
        try {
            val cr = contentResolver
            val cv = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name); put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS); put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv) ?: throw java.io.IOException("Android would not create the file")
            try {
                (cr.openOutputStream(uri, "w") ?: throw java.io.IOException("Android would not open the file")).use { it.write(text.toByteArray(Charsets.UTF_8)) }
                cr.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) { try { cr.delete(uri, null, null) } catch (_: Exception) { }; throw e }
            Toast.makeText(this, getString(R.string.practice_saved, name), Toast.LENGTH_LONG).show()
        } catch (e: Exception) { Toast.makeText(this, getString(R.string.practice_save_failed, e.message ?: e.javaClass.simpleName), Toast.LENGTH_LONG).show() }
    }
}
