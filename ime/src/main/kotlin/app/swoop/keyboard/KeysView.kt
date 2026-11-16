package app.swoop.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import app.kestrel.core.input.Key
import app.kestrel.core.input.KeyBox
import app.kestrel.core.input.KeySpec
import app.kestrel.core.input.MiniKeyboardGeometry
import app.kestrel.core.input.MiniKeyboardLayout
import app.kestrel.core.input.editor.ShiftState

/**
 * KB-100 walking skeleton of the keys view: it draws the ported English QWERTY pages with the SAME geometry function the glide decoder reads
 * ([MiniKeyboardGeometry.boxes]) and reports taps. KB-102 replaces its insides with the full port of the browser's KeysView (taps, key bubble,
 * Backspace repeat, hold-for-digit, space slide, glide trail); the [Listener] seam stays.
 * One custom View drawn with Canvas (plan 6.7): paints are created once, rectangles are computed on size change, a press invalidates one key.
 */
class KeysView(context: Context) : View(context) {
    interface Listener {
        /** A key was tapped (Shift included; the symbols/letters paging is the view's own business and is not reported). */
        fun onKey(key: Key)
    }

    var listener: Listener? = null
    private val density = resources.displayMetrics.density
    private val gap = 4f * density
    private val pad = 4f * density
    private val rowH = 50f * density

    private var symbols = false
    private var shift = ShiftState.OFF
    private var forField = true
    private var rows: List<List<KeySpec>> = MiniKeyboardLayout.rows(false, false, true)
    private var boxes: List<List<KeyBox>> = emptyList()
    private var pressed: KeyBox? = null
    private var pressedKey: Key? = null

    private val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    private val bg = if (dark) 0xFF202124.toInt() else 0xFFD3D6DB.toInt()
    private val keyFill = if (dark) 0xFF3C4043.toInt() else 0xFFFFFFFF.toInt()
    private val keyFillSpecial = if (dark) 0xFF2D2F31.toInt() else 0xFFB8BDC4.toInt()
    private val keyFillPressed = if (dark) 0xFF6B7280.toInt() else 0xFF9AA3AE.toInt()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (dark) 0xFFE8EAED.toInt() else 0xFF202124.toInt(); textAlign = Paint.Align.CENTER; textSize = 20f * density }
    private val rect = RectF()

    init { contentDescription = context.getString(R.string.app_name) }

    /** The controller owns the shift state; the view only shows it (capitals on the keys, a lit Shift key). */
    fun setShift(state: ShiftState) { if (shift != state) { shift = state; rebuild() } }

    /** The page for the field being edited (KB-104 chooses it from EditorInfo; today every field gets the text-field letters page). */
    fun setForField(value: Boolean) { if (forField != value) { forField = value; rebuild() } }

    private fun rebuild() {
        rows = MiniKeyboardLayout.rows(symbols, shift != ShiftState.OFF, forField)
        boxes = MiniKeyboardGeometry.boxes(rows, width - 2 * pad, height - 2 * pad, gap).map { r -> r.map { KeyBox(it.left + pad, it.top + pad, it.right + pad, it.bottom + pad) } }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (4 * rowH + 3 * gap + 2 * pad).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { rebuild() }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(bg)
        val r = 6f * density
        for ((ri, row) in rows.withIndex()) {
            for ((ci, spec) in row.withIndex()) {
                if (spec.key == null) continue
                val b = boxes[ri][ci]
                rect.set(b.left, b.top, b.right, b.bottom)
                fill.color = when { b === pressed -> keyFillPressed; spec.key is Key.Ch && spec.label.length <= 2 -> keyFill; else -> keyFillSpecial }
                canvas.drawRoundRect(rect, r, r, fill)
                val label = if (spec.key is Key.Shift && shift != ShiftState.OFF) (if (shift == ShiftState.LOCK) "⇪" else "⇧") else spec.label
                text.textSize = (if (label.length > 2) 15f else 20f) * density
                canvas.drawText(label, b.centerX, b.centerY - (text.ascent() + text.descent()) / 2f, text)
            }
        }
    }

    private fun keyAt(x: Float, y: Float): Pair<KeyBox, Key>? {
        for ((ri, row) in boxes.withIndex()) for ((ci, b) in row.withIndex()) {
            val k = rows[ri][ci].key ?: continue
            if (b.contains(x, y)) return b to k
        }
        return null
    }

    @SuppressLint("ClickableViewAccessibility") // KB-305 adds TalkBack key descriptions; this view is not a button
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val hit = keyAt(e.x, e.y)
                pressed = hit?.first; pressedKey = hit?.second
                if (hit != null) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val hit = keyAt(e.x, e.y)
                if (hit?.first !== pressed) { pressed = hit?.first; pressedKey = hit?.second; invalidate() }
            }
            MotionEvent.ACTION_UP -> {
                val key = pressedKey
                pressed = null; pressedKey = null; invalidate()
                if (key != null) press(key)
            }
            MotionEvent.ACTION_CANCEL -> { pressed = null; pressedKey = null; invalidate() }
        }
        return true
    }

    private fun press(key: Key) {
        when (key) {
            Key.ToSymbols -> { symbols = true; rebuild() }
            Key.ToLetters -> { symbols = false; rebuild() }
            else -> listener?.onKey(key)
        }
    }
}
