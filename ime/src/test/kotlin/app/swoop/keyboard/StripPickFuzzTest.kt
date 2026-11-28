package app.swoop.keyboard

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import app.kestrel.core.input.Key
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.Random
import java.util.concurrent.Executor

/**
 * R40-3 (owner 2026-11-28: "i glide a word and the keyboard gets it wrong so i delete half the word and press the correct word which is suggested, but it leaves the half typed word and simply adds the correct
 * word to it"). A seeded fuzz through the REAL presenter, keys view, glide engine (the shipped word list) and a real EditText: random glides, tapped letters, Backspaces, spaces and strip taps. The one thing
 * that must NEVER happen: a strip word tapped while letters of a word stand before the cursor ends up glued behind those letters ("acce" + "and" -> "acceand ").
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class StripPickFuzzTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val dp get() = app.resources.displayMetrics.density
    private val words: ByteArray by lazy { File("src/main/assets/glide/en.klx").readBytes() }

    private inner class Rig {
        val act: Activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val et = EditText(act)
        val info = EditorInfo()
        val view = KeysView(app)
        val host = GlideHost(app, Executor { it.run() }, Executor { it.run() }) { words }
        val presenter: KeyboardPresenter
        val ic: android.view.inputmethod.InputConnection
        init {
            File(app.filesDir, "glide_personal.txt").delete(); File(app.filesDir, "glide_habit.txt").delete(); File(app.filesDir, "own_words.txt").delete()
            app.getSharedPreferences(Prefs.FILE, 0).edit().clear().putBoolean(Prefs.GLIDE, true).putBoolean(Prefs.SUGGEST, true).putBoolean(Prefs.AUTOFIX, false).putBoolean(Prefs.LEARN, true).putBoolean(Prefs.AUTOCAPS, false).commit()
            val type = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            et.inputType = type; et.imeOptions = 0
            act.setContentView(et)
            info.inputType = type
            val base = et.onCreateInputConnection(info)!!
            ic = object : android.view.inputmethod.InputConnectionWrapper(base, true) {
                override fun sendKeyEvent(event: android.view.KeyEvent): Boolean {
                    if (event.keyCode == android.view.KeyEvent.KEYCODE_DEL && event.action == android.view.KeyEvent.ACTION_DOWN) return deleteSurroundingText(1, 0)
                    return true
                }
            }
            presenter = KeyboardPresenter(app, { ic }, { host }, { it.run() })
            presenter.view = view
            presenter.startInput(info)
            view.measure(View.MeasureSpec.makeMeasureSpec((564 * dp).toInt(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            presenter.prepareGlide()
        }
        fun report() { presenter.onUpdateSelection(et.selectionStart, et.selectionEnd, BaseInputConnection.getComposingSpanStart(et.text), BaseInputConnection.getComposingSpanEnd(et.text)) }
        fun ev(a: Int, x: Float, y: Float) { val t = SystemClock.uptimeMillis(); view.dispatchTouchEvent(MotionEvent.obtain(t, t, a, x, y, 0)); report() }
        fun tap(label: String) { val (x, y) = view.centerOf(label)!!; ev(MotionEvent.ACTION_DOWN, x, y); ev(MotionEvent.ACTION_UP, x, y) }
        fun tapLetter(c: String) { tap(if (view.centerOf(c) != null) c else c.uppercase()) }
        fun glide(word: String) {
            val pts = ArrayList<Pair<Float, Float>>()
            val cs = word.map { view.centerOf(it.toString()) ?: view.centerOf(it.uppercase())!! }
            for (i in 0 until cs.size - 1) { val (x0, y0) = cs[i]; val (x1, y1) = cs[i + 1]; val n = Math.max(2, (Math.hypot((x1 - x0).toDouble(), (y1 - y0).toDouble()) / (4 * dp)).toInt()); for (k in 0 until n) pts.add((x0 + (x1 - x0) * k / n) to (y0 + (y1 - y0) * k / n)) }
            pts.add(cs.last())
            ev(MotionEvent.ACTION_DOWN, pts[0].first, pts[0].second)
            for (p in pts.drop(1).dropLast(1)) ev(MotionEvent.ACTION_MOVE, p.first, p.second)
            ev(MotionEvent.ACTION_UP, pts.last().first, pts.last().second)
        }
        val text: String get() = et.text.toString()
        val cursor: Int get() = et.selectionStart
        fun tapChip(i: Int): Boolean { val c = view.chipCenterForTest(i) ?: return false; ev(MotionEvent.ACTION_DOWN, c.first, c.second); ev(MotionEvent.ACTION_UP, c.first, c.second); return true }
    }

    private val vocab = listOf("and", "the", "for", "make", "access", "there", "would", "about", "which", "people", "because", "something", "tomorrow", "hello", "world", "going", "think", "right")

    @Test fun aStripWordNeverLandsBehindTheLettersItWasMeantToReplace() {
        val n = (System.getenv("R40_FUZZ_N")?.toIntOrNull() ?: 60)
        val bad = ArrayList<String>()
        for (seed in 1..n) {
            val rnd = Random(seed.toLong() * 7919L)
            val r = Rig()
            val trace = StringBuilder()
            val steps = 5 + rnd.nextInt(9)
            for (st in 0 until steps) {
                when (rnd.nextInt(10)) {
                    0, 1, 2 -> { val w = vocab[rnd.nextInt(vocab.size)]; trace.append("glide($w) "); r.glide(w) }
                    3, 4 -> { val w = vocab[rnd.nextInt(vocab.size)]; val k = 1 + rnd.nextInt(Math.min(4, w.length)); trace.append("type(${w.take(k)}) "); for (c in w.take(k)) r.tapLetter(c.toString()) }
                    5, 6, 7 -> { val k = 1 + rnd.nextInt(5); trace.append("bs x$k "); repeat(k) { r.tap("⌫") } }
                    8 -> { trace.append("space "); r.tap("space") }
                    else -> {
                        val shown = r.view.candidatesForTest()
                        if (shown.isEmpty()) continue
                        val i = rnd.nextInt(shown.size)
                        val before = r.text; val cur = r.cursor
                        val head = before.substring(0, cur.coerceIn(0, before.length))
                        val stem = head.takeLastWhile { it.isLetter() || it == '\'' }                  // the letters of a word standing right before the cursor
                        val cand = shown[i].trim('“', '”')
                        if (!r.tapChip(i)) continue
                        trace.append("strip[$i]='${shown[i]}' (stem='$stem') -> '${r.text}' ")
                        val after = r.text
                        // a pick with letters before the cursor REPLACES them (or, for a word that starts with them, completes them): they must not stand in front of the picked word
                        val glued = stem.length >= 1 && cand.isNotEmpty() && !cand.lowercase().startsWith(stem.lowercase()) && after.lowercase().contains(stem.lowercase() + cand.lowercase()) && !before.lowercase().contains(stem.lowercase() + cand.lowercase())
                        if (glued) { bad.add("seed $seed: $trace"); break }
                    }
                }
            }
        }
        println("FUZZ stripPick: ${bad.size} bad of $n")
        for (b in bad.take(8)) println("FUZZ BAD $b")
        assertTrue("strip words landed behind half-typed letters in ${bad.size} runs: ${bad.take(3)}", bad.isEmpty())
    }
}
