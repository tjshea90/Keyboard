package app.swoop.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.TextPaint
import android.text.TextUtils
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import app.kestrel.core.input.ImeLayout
import app.kestrel.core.input.Key
import app.kestrel.core.input.KeyBox
import app.kestrel.core.input.KeySpec
import app.kestrel.core.input.MiniKeyboardGeometry
import app.kestrel.core.input.Page
import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.ShiftState
import app.kestrel.core.input.glide.GlideKeys

/**
 * The keys, drawn and hit-tested by ONE view (no view per key; plan 6.7), ported from the browser's KeysView and widened to a full-width system keyboard.
 *  - the SAME [MiniKeyboardGeometry] function lays out the rows for drawing, touching and (KB-103) the glide decoder, so they cannot disagree;
 *  - a touch between two keys types the nearest key of that row; a thumb that drifts a little off a letter still types the letter it went down on;
 *  - the key under the finger is shown large above it; Backspace repeats; holding a top-row letter types its digit; sliding over the space key moves the cursor;
 *  - a second finger that goes down while the first still holds a letter types the first letter at once (fast two-thumb typing).
 * The view only REPORTS ([Listener]); what a key does to the text is decided by InputController (:core). Pages (letters / symbols) are the view's own business.
 */
class KeysView(context: Context) : View(context) {
    interface Listener {
        /** A key was tapped (Shift included; the symbols / letters paging is the view's own business and is not reported). */
        fun onKey(key: Key)
        /** The finger slid over the space key: the text cursor moves this many characters (negative = left). */
        fun onCursor(steps: Int)
        /** A glide is in progress: [count] touch points so far (the arrays are the view's own: copy what you need before returning). Called at most every [PREVIEW_MS] ms. */
        fun onGlideMove(xs: FloatArray, ys: FloatArray, count: Int) {}
        /** A glide ended: the whole path (copied). The strip stays empty until the owner calls [showCandidates]. */
        fun onGlideEnd(xs: FloatArray, ys: FloatArray, count: Int) {}
        /** A guess in the strip was tapped. */
        fun onCandidate(word: String) {}
    }

    var listener: Listener? = null
    /** Called once, after the keys were drawn for the first time (the crash-loop guard counts that start as healthy). */
    var onFirstDraw: (() -> Unit)? = null
    private var drewOnce = false

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density
    private val gap = dp(3f)
    private val pad = dp(3f)
    private val handler = Handler(Looper.getMainLooper())

    /**
     * The layer above the keys (a transparent sibling view): everything that changes while a finger is down - the lit key, the key bubble, the glide trail, the strip of guesses -
     * is drawn there, so a press, a trail segment or the bubble going away redraws a tiny layer instead of every key of the keyboard (the tablet's GPU is weak). Null when the keys
     * view stands alone (tests): it then draws the dynamic things itself.
     */
    var overlay: View? = null
    private fun dyn() { val o = overlay; if (o != null) o.invalidate() else invalidate() }

    // ---- options (the settings screen writes them, the presenter hands them over)
    var numberRow = true; set(v) { if (field != v) { field = v; rebuildRows(); requestLayout() } }
    var keyPreview = true
    var hapticsOn = true
    var trailOn = true
    /** 1.0 = normal key height; the settings offer small / normal / large. */
    var keyHeightScale = 1f; set(v) { if (field != v) { field = v; rectsW = -1; requestLayout(); invalidate() } }
    var accent = 0xFF1F6FEB.toInt(); set(v) { field = v; applyPalette(); invalidate(); dyn() }

    // ---- what is shown
    private var page = Page.LETTERS
    private var shift = ShiftState.OFF
    private var kind = FieldKind.TEXT
    private var actionLabel = "⏎"
    private var rows: List<List<KeySpec>> = ImeLayout.rows(page, false, kind, actionLabel, true)
    private var rects: List<List<KeyBox>> = emptyList()
    private var rectsW = -1; private var rectsH = -1

    // ---- palette (light / dark follows the system; KB-305 adds themes)
    private var dark = false
    private var cBg = 0; private var cKey = 0; private var cSpecial = 0; private var cLit = 0; private var cText = 0; private var cMuted = 0; private var cAccent = 0; private var cAccentText = 0; private var cBubble = 0
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT }
    private val box = RectF()

    // ---- touch state (one pointer is tracked; see ACTION_POINTER_DOWN)
    private var activeId = -1
    private var down: KeySpec? = null           // the key the finger went down on (or slid to, for keys that are not letters)
    private var pressedLetter: KeySpec? = null  // a letter: what a tap that drifts a little still types
    private var litKey: KeySpec? = null
    private var holdAlt: String? = null         // the finger stayed on a top-row letter: its digit
    private var bubbleKey: KeySpec? = null; private var bubbleText = ""
    private var spaceDown = false; private var sliding = false; private var slideStartX = 0f; private var slideAnchorX = 0f
    private var downX = 0f; private var downY = 0f

    // ---- glide (KB-103)
    private var glideEnabled = false
    private var suggestionsEnabled = false
    private var armed = false                   // the finger went down on a letter: it may turn into a glide
    private var gliding = false
    private var glideDown: KeySpec? = null
    private var gx = FloatArray(256); private var gy = FloatArray(256); private var gn = 0
    private var fadeStart = 0L
    private var lastPreviewAt = 0L; private var lastPreviewN = 0
    private var cands: List<String> = emptyList()
    private var noWordUntil = 0L
    private var stripDown = -1
    private val trail = Path()
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val fadeStep = object : Runnable {
        override fun run() { if (SystemClock.uptimeMillis() - fadeStart < TRAIL_FADE_MS) { dyn(); postOnAnimation(this) } else { gn = 0; fadeStart = 0L; dyn() } }
    }
    private val noWordOff = Runnable { noWordUntil = 0L; dyn() }

    private val unlight = Runnable { litKey = null; dyn() }
    private val repeat = object : Runnable { override fun run() { down?.key?.let { listener?.onKey(it) }; handler.postDelayed(this, REPEAT_MS) } }
    private val hold = Runnable { holdFired() }

    init {
        contentDescription = context.getString(R.string.app_name)
        applyPalette()
    }

    // ------------------------------------------------------------------ what the service tells the view

    /** The controller owns the shift state; the view only shows it (capitals on the keys, a lit Shift key). */
    fun setShift(state: ShiftState) { if (shift != state) { shift = state; rebuildRows() } }

    /** The field being edited: which bottom row / keypad, and what the action key says. */
    fun setField(newKind: FieldKind, newActionLabel: String) {
        if (kind == newKind && actionLabel == newActionLabel) return
        kind = newKind; actionLabel = newActionLabel; page = Page.LETTERS; rebuildRows()
    }

    /** Glide typing on or off (off in password fields: the guesses would show what is being typed). Switching it changes the height: the strip of guesses sits above the keys. */
    fun setGlideEnabled(on: Boolean) { if (glideEnabled != on) { glideEnabled = on; if (!on) { cands = emptyList(); gn = 0 }; rectsW = -1; requestLayout(); invalidate() } }

    /** Word suggestions while tapping on or off: the strip above the keys is there for them even when glide typing is off. */
    fun setSuggestionsEnabled(on: Boolean) { if (suggestionsEnabled != on) { suggestionsEnabled = on; if (!on && !glideEnabled) { cands = emptyList(); gn = 0 }; rectsW = -1; requestLayout(); invalidate() } }

    fun showCandidates(words: List<String>) { cands = words; noWordUntil = 0L; dyn() }
    fun clearCandidates() { if (cands.isNotEmpty() || noWordUntil != 0L) { cands = emptyList(); noWordUntil = 0L; dyn() } }
    fun showNoWord() { cands = emptyList(); noWordUntil = SystemClock.uptimeMillis() + NO_WORD_MS; handler.removeCallbacks(noWordOff); handler.postDelayed(noWordOff, NO_WORD_MS); dyn() }
    val isGliding: Boolean get() = gliding
    private fun stripPx() = if (glideEnabled || suggestionsEnabled) dp(STRIP_H_DP) else 0f

    /** Everything in flight stops (the keyboard closed or the field changed). */
    fun reset() {
        handler.removeCallbacks(repeat); handler.removeCallbacks(unlight); handler.removeCallbacks(hold); handler.removeCallbacks(noWordOff); removeCallbacks(fadeStep)
        armed = false; gliding = false; gn = 0; fadeStart = 0L; cands = emptyList(); noWordUntil = 0L; stripDown = -1
        activeId = -1; down = null; pressedLetter = null; litKey = null; holdAlt = null; bubbleKey = null; spaceDown = false; sliding = false
        invalidate()
    }

    private fun rebuildRows() {
        rows = ImeLayout.rows(page, shift != ShiftState.OFF, kind, actionLabel, numberRow)
        rectsW = -1
        invalidate()
    }

    private fun applyPalette() {
        dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        cBg = if (dark) 0xFF1B1D20.toInt() else 0xFFD3D6DB.toInt()
        cKey = if (dark) 0xFF3C4043.toInt() else 0xFFFFFFFF.toInt()
        cSpecial = if (dark) 0xFF2B2E31.toInt() else 0xFFB9BEC6.toInt()
        cLit = if (dark) 0xFF6B7280.toInt() else 0xFF9AA3AE.toInt()
        cText = if (dark) 0xFFE8EAED.toInt() else 0xFF202124.toInt()
        cMuted = if (dark) 0xFF9AA0A6.toInt() else 0xFF5F6368.toInt()
        cAccent = accent; cAccentText = 0xFFFFFFFF.toInt()
        cBubble = if (dark) 0xFF0C161D.toInt() else 0xFFFFFFFF.toInt()
    }

    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); applyPalette(); invalidate() }

    // ------------------------------------------------------------------ layout

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        // a key is about as tall as a letter key is wide (a thumb-sized target), within sensible bounds on a phone and on the tablet
        val rowH = ((w - 2 * pad) / 10f).coerceIn(dp(40f), dp(52f)) * keyHeightScale
        val h = (stripPx() + rowH * ImeLayout.rowWeights(rows).sum() + (rows.size - 1) * gap + 2 * pad).toInt()
        setMeasuredDimension(w, h)
    }

    private fun layoutKeys() {
        if (rectsW == width && rectsH == height && rects.isNotEmpty()) return
        rectsW = width; rectsH = height
        rects = MiniKeyboardGeometry.boxes(rows, width - 2 * pad, height - 2 * pad - stripPx(), gap, 0f, ImeLayout.rowWeights(rows)).map { r -> r.map { KeyBox(it.left + pad, it.top + pad + stripPx(), it.right + pad, it.bottom + pad + stripPx()) } }
    }

    /** The 26 letter keys' centres as laid out now (the same function that draws them), for the glide decoder (KB-103). Null before the first layout. */
    fun glideKeys(): GlideKeys? {
        if (width <= 0 || height <= 0) return null
        val letters = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT, "⏎", numberRow)
        val b = MiniKeyboardGeometry.boxes(letters, width - 2 * pad, height - 2 * pad - stripPx(), gap, 0f, ImeLayout.rowWeights(letters)).map { r -> r.map { KeyBox(it.left + pad, it.top + pad + stripPx(), it.right + pad, it.bottom + pad + stripPx()) } }
        return try { GlideKeys.from(letters, b) } catch (e: IllegalArgumentException) { null }
    }

    // ------------------------------------------------------------------ drawing

    override fun onDraw(c: Canvas) {
        layoutKeys()
        c.drawColor(cBg)
        if (!drewOnce) { drewOnce = true; handler.post { onFirstDraw?.invoke() } }
        for ((i, row) in rows.withIndex()) for ((j, k) in row.withIndex()) { if (k.key != null) drawKey(c, k, rects[i][j], false, i == 0) }
        if (overlay == null) drawOverlay(c)
    }

    /** The dynamic layer: the lit key, the strip of guesses, the key bubble and the glide trail. Called by the overlay view (or by [onDraw] when there is none). */
    fun drawOverlay(c: Canvas) {
        layoutKeys()
        val lit = litKey
        if (lit != null) { val kb = boxOf(lit); if (kb != null) drawKey(c, lit, kb, true, rows[0].contains(lit)) }
        drawStrip(c)
        if (!gliding && keyPreview) drawBubble(c)
        if (trailOn) drawTrail(c)
    }

    private fun drawKey(c: Canvas, k: KeySpec, kb: KeyBox, lit: Boolean, topRow: Boolean) {
        val key = k.key ?: return
        val r = dp(6f)
        box.set(kb.left, kb.top, kb.right, kb.bottom)
        val special = key !is Key.Ch || k.label.length > 1
        fill.color = when {
            lit -> cLit
            key === Key.Go -> cAccent
            key === Key.Shift && shift == ShiftState.LOCK -> cAccent
            key === Key.Shift && shift == ShiftState.ONCE -> cMuted
            special -> cSpecial
            else -> cKey
        }
        c.drawRoundRect(box, r, r, fill)
        text.color = if (key === Key.Go || (key === Key.Shift && shift == ShiftState.LOCK)) cAccentText else cText
        if (key === Key.NextKeyboard) { drawGlobe(c, box); return }
        val label = if (key === Key.Shift) (if (shift == ShiftState.LOCK) "⇪" else "⇧") else k.label
        text.textSize = (kb.bottom - kb.top) * (if (label.length > 1) 0.30f else if (topRow && ImeLayout.hasNumberRow(rows)) 0.52f else 0.44f)
        val fm = text.fontMetrics
        c.drawText(label, box.centerX(), box.centerY() - (fm.ascent + fm.descent) / 2f, text)
        if (!numberRow && topRow && page == Page.LETTERS && kind != FieldKind.NUMBER && kind != FieldKind.PHONE && label.length == 1) {   // without the number row: the digit a held top-row letter types, small in its corner
            val d = TOP_ROW.indexOf(label.lowercase())
            if (d >= 0) {
                text.color = cMuted; text.textSize = (kb.bottom - kb.top) * 0.22f
                val hf = text.fontMetrics
                c.drawText(DIGITS.substring(d, d + 1), box.right - dp(7f), box.top + dp(3f) - hf.ascent, text)
            }
        }
    }

    /** The globe on the next-keyboard key: a circle, an equator and two meridians (no font symbol: the tablet's fonts may lack one). */
    private fun drawGlobe(c: Canvas, b: RectF) {
        val r = Math.min(b.width(), b.height()) * 0.24f
        line.color = cText; line.strokeWidth = dp(1.6f)
        c.drawCircle(b.centerX(), b.centerY(), r, line)
        c.drawLine(b.centerX() - r, b.centerY(), b.centerX() + r, b.centerY(), line)
        box2.set(b.centerX() - r * 0.45f, b.centerY() - r, b.centerX() + r * 0.45f, b.centerY() + r); c.drawOval(box2, line)
        c.drawLine(b.centerX(), b.centerY() - r, b.centerX(), b.centerY() + r, line)
    }
    private val box2 = RectF()

    private fun drawBubble(c: Canvas) {
        val k = bubbleKey ?: return
        val kb = boxOf(k) ?: return
        val room = kb.top - dp(2f)
        if (room < dp(30f)) return                    // the top row has no room above it until the candidate strip exists (KB-103)
        val w = Math.max((kb.right - kb.left) * 1.35f, dp(44f)); val h = Math.min(dp(52f), room)
        var left = (kb.left + kb.right) / 2f - w / 2f; if (left < 0f) left = 0f; if (left + w > width) left = width - w
        box.set(left, kb.top - dp(2f) - h, left + w, kb.top - dp(2f))
        val r = dp(8f)
        fill.color = cBubble; c.drawRoundRect(box, r, r, fill)
        line.strokeWidth = dp(1.2f); line.color = cAccent; c.drawRoundRect(box, r, r, line)
        text.color = cText; text.textSize = h * 0.6f
        val fm = text.fontMetrics
        c.drawText(bubbleText, box.centerX(), box.centerY() - (fm.ascent + fm.descent) / 2f, text)
    }

    private fun chipBoxes(): List<RectF> {
        val h = stripPx(); val n = cands.size
        if (h <= 0f || n == 0 || width <= 0) return emptyList()
        val cw = (width - 2 * pad - gap * (n - 1)) / n
        return List(n) { i -> RectF(pad + i * (cw + gap), pad, pad + i * (cw + gap) + cw, h - gap) }
    }
    private fun chipAt(x: Float, y: Float): Int {
        if (y >= stripPx() + pad || cands.isEmpty()) return -1
        for ((i, r) in chipBoxes().withIndex()) if (x >= r.left - gap / 2 && x < r.right + gap / 2) return i
        return -1
    }

    private fun drawStrip(c: Canvas) {
        val h = stripPx(); if (h <= 0f) return
        text.textSize = h * 0.45f
        val fm = text.fontMetrics
        if (cands.isEmpty()) {
            val miss = SystemClock.uptimeMillis() < noWordUntil
            val msg = if (miss) context.getString(R.string.strip_no_word) else if (glideEnabled && page == Page.LETTERS && (kind == FieldKind.TEXT || kind == FieldKind.SEARCH)) context.getString(R.string.strip_hint) else ""
            if (msg.isEmpty()) return
            text.color = if (miss) cAccent else cMuted; text.textSize = h * 0.38f
            c.drawText(TextUtils.ellipsize(msg, text, width - dp(8f), TextUtils.TruncateAt.END).toString(), width / 2f, (h - gap) / 2f + pad - (fm.ascent + fm.descent) / 2f, text)
            return
        }
        val r = dp(6f)
        for ((i, w) in cands.withIndex()) {
            val rect = chipBoxes()[i]
            fill.color = if (i == stripDown) cLit else cKey
            c.drawRoundRect(rect, r, r, fill)
            if (i == 0) { line.strokeWidth = dp(1.2f); line.color = cAccent; box.set(rect); box.inset(dp(0.6f), dp(0.6f)); c.drawRoundRect(box, r, r, line) }
            text.color = cText
            c.drawText(TextUtils.ellipsize(w, text, rect.width() - dp(8f), TextUtils.TruncateAt.END).toString(), rect.centerX(), rect.centerY() - (fm.ascent + fm.descent) / 2f, text)
        }
    }

    private fun drawTrail(c: Canvas) {
        if (gn < 2) return
        val alpha = if (gliding || fadeStart == 0L) 1f else (1f - (SystemClock.uptimeMillis() - fadeStart).toFloat() / TRAIL_FADE_MS).coerceIn(0f, 1f)
        if (alpha <= 0f) return
        trail.reset(); trail.moveTo(gx[0], gy[0])
        for (i in 1 until gn) trail.lineTo(gx[i], gy[i])
        trailPaint.strokeWidth = dp(8f); trailPaint.color = (cAccent and 0x00FFFFFF) or ((80 * alpha).toInt() shl 24); c.drawPath(trail, trailPaint)
        trailPaint.strokeWidth = dp(2.5f); trailPaint.color = (cAccent and 0x00FFFFFF) or ((235 * alpha).toInt() shl 24); c.drawPath(trail, trailPaint)
    }

    // ------------------------------------------------------------------ touch

    private fun keyAt(x: Float, y: Float): KeySpec? {
        layoutKeys()
        if (rects.isEmpty()) return null
        // the row under (or nearest to) the finger: the pad around the keys and the gaps between rows belong to the closest row
        var ri = 0; var best = Float.MAX_VALUE
        for ((i, row) in rects.withIndex()) {
            val top = row[0].top; val bottom = row[0].bottom
            val d = if (y < top) top - y else if (y > bottom) y - bottom else 0f
            if (d < best) { best = d; ri = i }
        }
        if (best > dp(10f)) return null
        // inside a key, else the nearest key of that row horizontally (a thumb that lands on a gap still types)
        var near: KeySpec? = null; var nearD = Float.MAX_VALUE
        for ((j, b) in rects[ri].withIndex()) {
            val k = rows[ri][j]; if (k.key == null) continue
            if (b.contains(x, y)) return k
            val d = Math.abs(x - b.centerX); if (d < nearD) { nearD = d; near = k }
        }
        return near
    }

    private fun boxOf(k: KeySpec?): KeyBox? {
        if (k == null) return null
        layoutKeys()
        for ((i, row) in rows.withIndex()) for ((j, s) in row.withIndex()) if (s === k) return rects[i][j]
        return null
    }

    private fun isLetter(k: KeySpec?): Boolean { val c = (k?.key as? Key.Ch)?.text ?: return false; return c.length == 1 && c[0].isLetter() }
    private fun isBubbleKey(k: KeySpec?): Boolean { val c = (k?.key as? Key.Ch)?.text ?: return false; return c.length == 1 && c != " " }
    /** The digit a top-row letter types when it is held (null for every other key and page). */
    private fun altOf(k: KeySpec?): String? {
        if (numberRow || page != Page.LETTERS || kind == FieldKind.NUMBER || kind == FieldKind.PHONE || k == null || k.label.length != 1) return null
        val i = TOP_ROW.indexOf(k.label.lowercase()); return if (i >= 0 && rows[0].contains(k)) DIGITS.substring(i, i + 1) else null
    }

    private fun light(k: KeySpec?) { litKey = k; dyn(); handler.removeCallbacks(unlight); if (k != null) handler.postDelayed(unlight, FLASH_MS) }
    private fun haptic() { if (hapticsOn) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    private fun clearHold() { handler.removeCallbacks(hold); holdAlt = null; bubbleKey = null }
    private fun holdFired() {
        val k = pressedLetter ?: down ?: return
        val alt = altOf(k) ?: return
        holdAlt = alt; bubbleKey = k; bubbleText = alt; dyn(); haptic()
    }

    @SuppressLint("ClickableViewAccessibility") // KB-305 adds TalkBack key descriptions; this view is a keyboard, not a button
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { requestUnbufferedDispatch(e); activeId = e.getPointerId(0); pressDown(e.getX(0), e.getY(0)) }   // unbuffered: every touch sample at once, not batched to the frame (a fast trail)
            MotionEvent.ACTION_POINTER_DOWN -> {
                // the second thumb: the letter the first one holds is typed NOW (in the order they were pressed), then the new finger is the tracked one
                finishHeldLetter()
                val i = e.actionIndex; activeId = e.getPointerId(i); pressDown(e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_MOVE -> { val i = e.findPointerIndex(activeId); if (i >= 0) pressMove(e, i) }
            MotionEvent.ACTION_POINTER_UP -> {
                val i = e.actionIndex
                if (e.getPointerId(i) == activeId) { pressUp(e, i); activeId = -1 }
            }
            MotionEvent.ACTION_UP -> { if (activeId >= 0) { val i = e.findPointerIndex(activeId).let { if (it < 0) 0 else it }; pressUp(e, i) }; activeId = -1 }
            MotionEvent.ACTION_CANCEL -> { pressCancel(); activeId = -1 }
        }
        return true
    }

    private fun pressDown(x: Float, y: Float) {
        handler.removeCallbacks(repeat); clearHold()
        armed = false; gliding = false; stripDown = -1
        if (stripPx() > 0f && y < stripPx() + pad) { stripDown = chipAt(x, y); dyn(); return }   // a guess in the strip was touched
        removeCallbacks(fadeStep); gn = 0; fadeStart = 0L
        val k = keyAt(x, y); down = k; light(k)
        spaceDown = (k?.key as? Key.Ch)?.text == " "; sliding = false; slideStartX = x; slideAnchorX = x
        pressedLetter = if (isLetter(k)) k else null
        if (isBubbleKey(k)) { bubbleKey = k; bubbleText = k!!.label }
        if (k != null) { haptic(); if (altOf(k) != null) handler.postDelayed(hold, HOLD_MS) }
        if (k?.key === Key.Backspace) { listener?.onKey(Key.Backspace); handler.postDelayed(repeat, REPEAT_FIRST_MS) }
        downX = x; downY = y
        if (glideEnabled && page == Page.LETTERS && pressedLetter != null) { armed = true; glideDown = k; addPoint(x, y) }
    }

    private fun pressMove(e: MotionEvent, i: Int) {
        val x = e.getX(i); val y = e.getY(i)
        if (stripDown >= 0) return
        if (armed) {                                     // a letter: nothing happens until the finger leaves its key (a glide) or lifts (a tap)
            for (h in 0 until e.historySize) addPoint(e.getHistoricalX(i, h), e.getHistoricalY(i, h))
            addPoint(x, y)
            if (!gliding && movedFar(x, y)) beginGlide()          // a glide starts as soon as the finger has really moved: no waiting for the edge of the key
            if (gliding) { dyn(); maybePreview() }
            return
        }
        if (pressedLetter != null) return                // (glide off) a letter: a tap types the letter it went down on
        if (spaceDown) {                                 // press the space key and slide sideways: the text cursor follows the finger
            if (!sliding && Math.abs(x - slideStartX) > dp(SLIDE_START_DP)) { sliding = true; slideAnchorX = x; litKey = down; dyn() }
            if (sliding) {
                val steps = ((x - slideAnchorX) / dp(SLIDE_STEP_DP)).toInt()
                if (steps != 0) { slideAnchorX += steps * dp(SLIDE_STEP_DP); haptic(); listener?.onCursor(steps) }
                return
            }
        }
        val k = keyAt(x, y)
        if (k !== down) { down = k; handler.removeCallbacks(repeat); clearHold(); if (isBubbleKey(k)) { bubbleKey = k; bubbleText = k!!.label }; light(k) }
    }

    private fun pressUp(e: MotionEvent, i: Int) {
        val x = e.getX(i); val y = e.getY(i)
        if (stripDown >= 0) {
            val c = chipAt(x, y); val was = stripDown; stripDown = -1; dyn()
            if (c == was && c in cands.indices) listener?.onCandidate(cands[c])
            return
        }
        if (gliding) { for (h in 0 until e.historySize) addPoint(e.getHistoricalX(i, h), e.getHistoricalY(i, h)); addPoint(x, y); endGlide(); return }
        armed = false
        handler.removeCallbacks(repeat)
        val alt = holdAlt; clearHold(); dyn()          // the bubble goes away BEFORE the key is sent to the app (which can take a moment)
        if (sliding) { sliding = false; spaceDown = false; down = null; litKey = null; return }   // the end of a cursor slide: no space is typed
        spaceDown = false
        val letter = pressedLetter
        if (letter != null) {                              // a tap on a letter types the letter it went down on, even when the finger drifted (held: its digit)
            pressedLetter = null; down = null; gn = 0
            light(letter); emit(if (alt != null) Key.Ch(alt) else letter.key!!)
            return
        }
        val k = keyAt(x, y)
        val d = down; down = null
        if (k != null && k === d && k.key !== Key.Backspace) { light(k); emit(k.key!!) }      // Backspace fired on the way down
    }

    private fun pressCancel() {
        handler.removeCallbacks(repeat); clearHold(); down = null; pressedLetter = null; armed = false; stripDown = -1; spaceDown = false; sliding = false; litKey = null; dyn()
        if (gliding) { gliding = false; fadeStart = SystemClock.uptimeMillis(); postOnAnimation(fadeStep) }
    }

    /** A second finger arrived: type the letter the first one is holding, once. */
    private fun finishHeldLetter() {
        val l = pressedLetter ?: return
        handler.removeCallbacks(repeat)
        val alt = holdAlt; clearHold()
        pressedLetter = null; down = null; armed = false; gliding = false; gn = 0
        light(l); emit(if (alt != null) Key.Ch(alt) else l.key!!)
    }

    // ---- glide
    private fun addPoint(x: Float, y: Float) {
        if (gn == gx.size) { if (gn >= MAX_TRAIL_POINTS) return; gx = gx.copyOf(gn * 2); gy = gy.copyOf(gn * 2) }
        gx[gn] = x; gy[gn] = y; gn++
    }
    private fun pitch(): Float = if (rects.isNotEmpty() && rects[0].size > 1) rects[0][1].centerX - rects[0][0].centerX else dp(29f)
    private fun movedFar(x: Float, y: Float): Boolean { val dx = x - downX; val dy = y - downY; return dx * dx + dy * dy > dp(GLIDE_START_DP) * dp(GLIDE_START_DP) }
    private fun pathPitches(): Float { var len = 0f; for (i in 1 until gn) len += Math.hypot((gx[i] - gx[i - 1]).toDouble(), (gy[i] - gy[i - 1]).toDouble()).toFloat(); return len / pitch() }

    private fun beginGlide() {
        lastPreviewAt = SystemClock.uptimeMillis(); lastPreviewN = 0
        clearHold(); gliding = true; down = null; pressedLetter = null
        handler.removeCallbacks(repeat); handler.removeCallbacks(unlight); litKey = null
        removeCallbacks(fadeStep); fadeStart = 0L
    }

    /** While the finger is still gliding, now and then (not faster than the decoder, not for every touch point) ask for a live guess of the path so far. */
    private fun maybePreview() {
        val now = SystemClock.uptimeMillis()
        if (now - lastPreviewAt < PREVIEW_MS || gn - lastPreviewN < PREVIEW_POINTS) return
        lastPreviewAt = now; lastPreviewN = gn
        listener?.onGlideMove(gx, gy, gn)
    }

    private fun endGlide() {
        gliding = false; armed = false
        val k = glideDown; glideDown = null
        if (pathPitches() < MIN_GLIDE_PITCHES) {         // a tap that slid a little: the key it went down on
            gn = 0; dyn()
            if (k?.key != null) { light(k); emit(k.key!!) }
            return
        }
        fadeStart = SystemClock.uptimeMillis(); postOnAnimation(fadeStep)
        listener?.onGlideEnd(gx.copyOf(gn), gy.copyOf(gn), gn)
    }

    /** Paging between letters and symbols is the view's own; everything else goes to the listener. */
    private fun emit(key: Key) {
        when (key) {
            Key.ToSymbols -> { page = Page.SYMBOLS; rebuildRows() }
            Key.ToLetters -> { page = Page.LETTERS; rebuildRows() }
            else -> listener?.onKey(key)
        }
    }

    // ------------------------------------------------------------------ tests

    /** Tests: the centre of the key labelled [label] in this view's pixels. */
    fun centerOf(label: String): Pair<Float, Float>? {
        layoutKeys()
        for ((i, row) in rows.withIndex()) for ((j, k) in row.withIndex()) if (k.label == label && k.key != null) return rects[i][j].centerX to rects[i][j].centerY
        return null
    }
    fun bubbleTextForTest(): String? = if (bubbleKey != null) bubbleText else null
    fun pageForTest(): Page = page
    fun candidatesForTest(): List<String> = cands
    fun chipCenterForTest(i: Int): Pair<Float, Float>? { val r = chipBoxes().getOrNull(i) ?: return null; return r.centerX() to r.centerY() }

    companion object {
        /** Sliding over the space key: the finger must move this far sideways before it is a cursor slide and not a space, and every step of this width moves the cursor one character. */
        const val SLIDE_START_DP = 10f
        const val SLIDE_STEP_DP = 11f
        /** How long a pressed key stays lit. */
        const val FLASH_MS = 130L
        const val REPEAT_FIRST_MS = 380L
        const val REPEAT_MS = 70L
        /** Holding a top-row letter this long types its digit. */
        const val HOLD_MS = 420L
        private const val TOP_ROW = "qwertyuiop"
        private const val DIGITS = "1234567890"
        /** The strip of guesses above the keys (glide typing on). */
        const val STRIP_H_DP = 30f
        /** A finger that has moved this far (dp) from where it went down has started a glide (a short drag that ends up under 1.2 key widths is still a tap). */
        const val GLIDE_START_DP = 5f
        /** A path shorter than this (in key pitches) is a tap that slid a little, not a glide. */
        const val MIN_GLIDE_PITCHES = 1.2f
        const val TRAIL_FADE_MS = 260L
        const val NO_WORD_MS = 1500L
        /** The live guess while gliding: at most this often, and only after this many new touch points. */
        const val PREVIEW_MS = 110L
        const val PREVIEW_POINTS = 4
        private const val MAX_TRAIL_POINTS = 4096
    }
}
