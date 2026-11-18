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
import app.kestrel.core.input.editor.Pad
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
        /** Backspace has been held long enough that every repeat takes a whole word (owner R23a). */
        fun onDeleteWord() {}
        /** The first box of the strip holds what the person copied, and it was tapped: paste it (owner R20a). */
        fun onClipChip() {}
        /** The first box holds the copy and it was PRESSED AND HELD: it goes away (it is not offered again). */
        fun onClipChipLong() {}
        /** A guess in the strip was PRESSED AND HELD (owner: long press a recommended word to delete it for good). */
        fun onCandidateLong(word: String) {}
        /** The person answered "remove this word for good?" ([yes]) for a word they had held in the strip. */
        fun onRemoveWord(word: String, yes: Boolean) {}
        /** An icon of the toolbar at the left of the strip was tapped ([TOOL_SETTINGS], [TOOL_CLIPBOARD], [TOOL_TRANSLATE]). */
        fun onToolbar(tool: Int) {}
    }

    var listener: Listener? = null
    /** Called once, after the keys were drawn for the first time (the crash-loop guard counts that start as healthy). */
    var onFirstDraw: (() -> Unit)? = null
    private var drewOnce = false

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density
    /** The gap inside the strip of guesses. */
    private val gap = dp(3f)
    private val pad = dp(5f)
    /**
     * The gaps between the keys (the photo, owner M1): across a row a gap is about 14 % of the key pitch (3 to 8 dp), and the rows are one and a half times as far apart as the keys are across.
     * They scale with the keyboard's width, so the squeezed docked keyboard and the floating one keep their proportions.
     */
    private fun gapX(w: Float = width.toFloat()): Float = (((w - 2 * pad) / 10f) * GAP_FRAC).coerceIn(dp(3f), dp(8f))
    private fun gapY(w: Float = width.toFloat()): Float = gapX(w) * GAP_Y_MUL
    private val handler = Handler(Looper.getMainLooper())

    /**
     * The layer above the keys (a transparent sibling view): everything that changes while a finger is down - the lit key, the key bubble, the glide trail, the strip of guesses -
     * is drawn there, so a press, a trail segment or the bubble going away redraws a tiny layer instead of every key of the keyboard (the tablet's GPU is weak). Null when the keys
     * view stands alone (tests): it then draws the dynamic things itself.
     */
    var overlay: View? = null
    private fun dyn() { val o = overlay; if (o != null) o.invalidate() else invalidate() }

    // ---- options (the settings screen writes them, the presenter hands them over)
    /** Off by default, like the photo: the digits are small hints on the top row and a held top-row key types its digit. Settings > Layout > Number row puts a row of digits above the letters. */
    var numberRow = false; set(v) { if (field != v) { field = v; rebuildRows(); requestLayout() } }
    /** The keyboard-switch key at the left of the space bar (off by default; safe mode turns it on). */
    var showGlobe = false; set(v) { if (field != v) { field = v; rebuildRows(); requestLayout() } }
    var keyPreview = true
    var hapticsOn = true
    var trailOn = true
    var soundOn = false
    /** Press the space bar and slide: the text cursor follows the finger. */
    var spaceSlideOn = true
    /** Owner: "turn off the letters completely, leaving a blank panel that I can type with but it still works as though the letters are there". The keys keep their places and their taps; the letters are not drawn (and no key bubble shows them). */
    var hideLetters = false; set(v) { if (field != v) { field = v; invalidate(); dyn() } }
    /** The translate icon is lit while translating is on (the presenter sets it). */
    var translateOn = false; set(v) { if (field != v) { field = v; dyn() } }
    /** The "ES" icon only when the person has the translate feature on in Settings. */
    var translateAvailable = true; set(v) { if (field != v) { field = v; dyn() } }
    /** An emoji or a word is shown in a suggestion chip; set while a long-press confirmation (remove this word for good?) is on screen. */
    var confirmWord: String? = null; set(v) { if (field != v) { field = v; dyn() } }
    /** 0 = follows the system's light / dark setting, 1 = dark, 2 = light. */
    var themeMode = 0; set(v) { if (field != v) { field = v; applyPalette(); invalidate(); dyn() } }
    /** 1.0 = normal key height; the settings offer small / normal / large. */
    var keyHeightScale = 1f; set(v) { if (field != v) { field = v; rectsW = -1; requestLayout(); invalidate() } }
    /** The floating keyboard's own height multiplier (corner drags), on top of [keyHeightScale]. */
    var floatMul = 1f; set(v) { if (field != v) { field = v; rectsW = -1; requestLayout(); invalidate() } }
    /** The palette, for the panels that replace the keys (the clipboard). */
    val colKey: Int get() = cKey
    val colSpecial: Int get() = cSpecial
    val colLit: Int get() = cLit
    val colText: Int get() = cText
    val colMuted: Int get() = cMuted
    val colAccent: Int get() = cAccent
    val colAccentText: Int get() = cAccentText
    /** The colour behind the keys (the floating frame is painted in it too). */
    val backgroundColor: Int get() = cBg
    var accent = 0xFF00DDE6.toInt(); set(v) { field = v; applyPalette(); invalidate(); dyn() }

    // ---- what is shown
    private var page = Page.LETTERS
    private var shift = ShiftState.OFF
    private var kind = FieldKind.TEXT
    /** The number pad the field wants instead of the letters (R23d), or [Pad.NONE]. */
    private var pad = Pad.NONE
    private var actionLabel = "⏎"
    private var rows: List<List<KeySpec>> = ImeLayout.rows(page, false, kind, actionLabel, false)
    private var rects: List<List<KeyBox>> = emptyList()
    private var rectsW = -1; private var rectsH = -1

    // ---- palette (light / dark follows the system; KB-305 adds themes)
    private var dark = false
    private var cBg = 0; private var cKey = 0; private var cSpecial = 0; private var cLit = 0; private var cText = 0; private var cMuted = 0; private var cAccent = 0; private var cAccentText = 0; private var cBubble = 0
    private var cPale = 0; private var cPaleText = 0
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
    private val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT }
    private val box = RectF()

    // ---- touch state (one pointer is tracked; see ACTION_POINTER_DOWN)
    private var activeId = -1
    private var down: KeySpec? = null           // the key the finger went down on (or slid to, for keys that are not letters)
    private var pressedLetter: KeySpec? = null  // a letter: what a tap that drifts a little still types
    private var litKey: KeySpec? = null
    private var holdAlt: String? = null         // the finger stayed on a top-row letter: its digit
    private var bubbleKey: KeySpec? = null; private var bubbleText = ""
    private var spaceIsDown = false; private var spaceHeld = false
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
    private var toolDown = -1
    private var stripLong = false
    private val trail = Path()
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val fadeStep = object : Runnable {
        override fun run() { if (SystemClock.uptimeMillis() - fadeStart < TRAIL_FADE_MS) { dyn(); postOnAnimation(this) } else { gn = 0; fadeStart = 0L; dyn() } }
    }
    private val noWordOff = Runnable { noWordUntil = 0L; dyn() }
    /** A guess in the strip was held: ask to remove it for good (the person confirms with a tap). */
    private val stripHold = Runnable {
        val i = stripDown
        if (i >= 0 && confirmWord == null && i in cands.indices) { stripLong = true; haptic(); if (clipChip && i == 0) listener?.onClipChipLong() else listener?.onCandidateLong(cands[i]) }
    }

    private val unlight = Runnable { litKey = null; dyn() }
    /** When Backspace went down: how long it has been held decides how fast it repeats and when it starts deleting whole words ([BackspaceRepeat], owner R23a). */
    private var repeatStartAt = 0L
    private val repeat = object : Runnable {
        override fun run() {
            val held = SystemClock.uptimeMillis() - repeatStartAt
            val k = down?.key
            if (k != null) { if (k === Key.Backspace && BackspaceRepeat.wordMode(held)) listener?.onDeleteWord() else listener?.onKey(k) }
            handler.postDelayed(this, BackspaceRepeat.delayAfter(held))
        }
    }
    private val hold = Runnable { holdFired() }
    /** The space bar was held (and not slid): the keyboard picker opens (the globe key is gone by default). */
    private val spaceHold = Runnable { if (spaceIsDown && !sliding) { spaceHeld = true; haptic(); dyn(); listener?.onKey(Key.NextKeyboard) } }

    init {
        contentDescription = context.getString(R.string.app_name)
        applyPalette()
    }

    /** Every option from the settings in one call (the keyboard opening, the settings preview). */
    fun applyOptions(s: Settings) {
        numberRow = s.numberRow; keyPreview = s.keyPreview; hapticsOn = s.haptics; soundOn = s.sound; trailOn = s.trail
        keyHeightScale = s.keyHeightScale; accent = s.accentColor; themeMode = s.theme; spaceSlideOn = s.spaceSlide; showGlobe = s.showGlobe
        hideLetters = s.hideLetters; translateAvailable = s.translateEnabled
    }

    // ------------------------------------------------------------------ what the service tells the view

    /** The controller owns the shift state; the view only shows it (capitals on the keys, a lit Shift key). */
    fun setShift(state: ShiftState) { if (shift != state) { shift = state; rebuildRows() } }

    /** The field being edited: which bottom row / keypad, and what the action key says. */
    fun setField(newKind: FieldKind, newActionLabel: String, newPad: Pad = Pad.of(newKind)) {
        if (kind == newKind && actionLabel == newActionLabel && pad == newPad) return
        kind = newKind; actionLabel = newActionLabel; pad = newPad; page = Page.LETTERS; rebuildRows()
    }

    /** Glide typing on or off (off in password fields: the guesses would show what is being typed). Switching it changes the height: the strip of guesses sits above the keys. */
    fun setGlideEnabled(on: Boolean) { if (glideEnabled != on) { glideEnabled = on; if (!on) { cands = emptyList(); gn = 0 }; rectsW = -1; requestLayout(); invalidate() } }

    /** Word suggestions while tapping on or off: the strip above the keys is there for them even when glide typing is off. */
    fun setSuggestionsEnabled(on: Boolean) { if (suggestionsEnabled != on) { suggestionsEnabled = on; if (!on && !glideEnabled) { cands = emptyList(); gn = 0 }; rectsW = -1; requestLayout(); invalidate() } }

    /**
     * The guesses in the strip, left to right. [accent] is the box that gets the accent outline (the one a Space would take): the first by default, the middle one after a glide, where that box is the NEXT word and the others
     * replace the glided one (owner R20b). An empty string is a box that is not filled yet: nothing is drawn there and it takes no touch.
     */
    fun showCandidates(words: List<String>, accent: Int = 0, clip: Boolean = false) { cands = words; accentChip = accent; clipChip = clip && words.isNotEmpty(); noWordUntil = 0L; dyn() }
    private var accentChip = 0
    /** The first box holds what the person copied (owner R20a): it shows a small clipboard and a tap pastes the whole copy ([Listener.onClipChip]), not the shortened text shown. */
    private var clipChip = false
    fun clearCandidates() { if (cands.isNotEmpty() || noWordUntil != 0L) { cands = emptyList(); accentChip = 0; clipChip = false; noWordUntil = 0L; dyn() } }
    private var notice: String? = null
    /** A short message in the strip (translating, an error) for a few seconds. */
    fun showNotice(msg: String) { notice = msg; cands = emptyList(); noWordUntil = SystemClock.uptimeMillis() + NOTICE_MS; handler.removeCallbacks(noWordOff); handler.postDelayed(noWordOff, NOTICE_MS); dyn() }
    fun showNoWord() { notice = null; cands = emptyList(); noWordUntil = SystemClock.uptimeMillis() + NO_WORD_MS; handler.removeCallbacks(noWordOff); handler.postDelayed(noWordOff, NO_WORD_MS); dyn() }
    val isGliding: Boolean get() = gliding
    /** The strip above the keys always exists now: the toolbar (settings, clipboard, translate) lives at its left, the guesses fill the rest. */
    private fun stripPx() = dp(STRIP_H_DP)

    /** Everything in flight stops (the keyboard closed or the field changed). */
    fun reset() {
        handler.removeCallbacks(repeat); handler.removeCallbacks(unlight); handler.removeCallbacks(hold); handler.removeCallbacks(spaceHold); handler.removeCallbacks(stripHold); toolDown = -1; stripLong = false; confirmWord = null; popup = null; spaceIsDown = false; spaceHeld = false; handler.removeCallbacks(noWordOff); removeCallbacks(fadeStep)
        armed = false; gliding = false; gn = 0; fadeStart = 0L; cands = emptyList(); clipChip = false; noWordUntil = 0L; stripDown = -1
        activeId = -1; down = null; pressedLetter = null; litKey = null; holdAlt = null; bubbleKey = null; spaceDown = false; sliding = false
        invalidate()
    }

    private fun rebuildRows() {
        rows = ImeLayout.rows(page, shift != ShiftState.OFF, kind, actionLabel, numberRow, showGlobe, pad)
        rectsW = -1
        invalidate()
        if (Diag.enabled) post { keymapLine()?.let { Diag.d(it) } }          // (debug builds: the tools see the new page's keys)
    }

    private fun applyPalette() {
        dark = when (themeMode) { 1 -> true; 2 -> false; else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES }
        // Owner M1: the dark colours are measured from the photo (Gboard, dark teal): background 0d1d1d, letter keys 253535, Shift / Backspace / comma / full stop 35494a, ?123 a4cedc,
        // text f1ffff, digit hints 9babab, Enter and the lit translate icon in the accent colour (cyan 00dde6 by default).
        cBg = if (dark) 0xFF0D1D1D.toInt() else 0xFFE4EDED.toInt()
        cKey = if (dark) 0xFF253535.toInt() else 0xFFFFFFFF.toInt()
        cSpecial = if (dark) 0xFF35494A.toInt() else 0xFFC4D4D5.toInt()
        cLit = if (dark) 0xFF4D6A6B.toInt() else 0xFF9FB8BA.toInt()
        cText = if (dark) 0xFFF1FFFF.toInt() else 0xFF0D1D1D.toInt()
        cMuted = if (dark) 0xFF9BABAB.toInt() else 0xFF566B6C.toInt()
        cAccent = accent
        cAccentText = if (luma(accent) > 0.5f) 0xFF062020.toInt() else 0xFFFFFFFF.toInt()      // dark ink on the bright accents, white ink on the dark ones
        cPale = if (dark) 0xFFA4CEDC.toInt() else 0xFF9CC7D6.toInt()
        cPaleText = 0xFF0D1D1D.toInt()
        cBubble = if (dark) 0xFF0C161D.toInt() else 0xFFFFFFFF.toInt()
        onPaletteChanged?.invoke()
    }

    /** Called after the colours were chosen again (a new setting, or the phone changed between day and night): the keyboard's frame paints its own background in the same colour. */
    var onPaletteChanged: (() -> Unit)? = null

    private fun luma(c: Int): Float = (0.299f * ((c shr 16) and 0xFF) + 0.587f * ((c shr 8) and 0xFF) + 0.114f * (c and 0xFF)) / 255f
    private fun mix(v: Int, f: Float): Int = (v + (255 - v) * f).toInt().coerceIn(0, 255)
    private fun lighten(c: Int, f: Float): Int {
        val alpha = c and 0xFF000000.toInt()
        return alpha or (mix((c shr 16) and 0xFF, f) shl 16) or (mix((c shr 8) and 0xFF, f) shl 8) or mix(c and 0xFF, f)
    }

    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); applyPalette(); invalidate() }

    // ------------------------------------------------------------------ layout

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, heightFor(w, floatMul))
    }

    /** The height this keyboard has when [w] pixels wide and its rows are [mul] times as tall as the settings say (the floating keyboard asks this to turn a corner drag into a height). */
    fun heightFor(w: Int, mul: Float): Int = (fixedHeight(w) + rowHeight(w) * mul * ImeLayout.rowWeights(rows).sum()).toInt()

    /** What does not scale with the row multiplier: the strip, the gaps between the rows and the padding. */
    private fun fixedHeight(w: Int): Float = stripPx() + (rows.size - 1) * gapY(w.toFloat()) + 2 * pad

    /** The height of one row of weight 1 at [w] pixels wide with the settings' key height, before the floating multiplier. The photo's keys are taller than wide (1.6 to 1) on a phone: a row is 1.35 key pitches tall, within sensible bounds on a phone (about 54 dp) and on the tablet (56 dp). */
    private fun rowHeight(w: Int): Float = (((w - 2 * pad) / 10f) * ROW_H_PITCHES).coerceIn(dp(ROW_H_MIN_DP), dp(ROW_H_MAX_DP)) * keyHeightScale

    /**
     * The row multiplier that makes this view [keysH] pixels tall at [w] pixels wide: the exact inverse of [heightFor] (the strip, gaps and padding do not scale, so "target over the height at 1" is only an
     * approximation, and a corner drag that used it drifted a few pixels from the finger).
     */
    fun mulFor(w: Int, keysH: Int): Float {
        val per = rowHeight(w) * ImeLayout.rowWeights(rows).sum()
        return if (per <= 0f) 1f else ((keysH - fixedHeight(w)) / per).coerceAtLeast(0f)
    }

    private fun layoutKeys() {
        if (rectsW == width && rectsH == height && rects.isNotEmpty()) return
        rectsW = width; rectsH = height
        rects = MiniKeyboardGeometry.slots(rows, width - 2 * pad, height - 2 * pad - stripPx(), gapX(), gapY(), 0f, ImeLayout.rowWeights(rows)).map { r -> r.map { KeyBox(it.left + pad, it.top + pad + stripPx(), it.right + pad, it.bottom + pad + stripPx()) } }
    }

    /** The 26 letter keys' centres as laid out now (the same function that draws them), for the glide decoder (KB-103). Null before the first layout. */
    fun glideKeys(): GlideKeys? {
        if (width <= 0 || height <= 0) return null
        val letters = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT, "⏎", numberRow)
        val b = MiniKeyboardGeometry.slots(letters, width - 2 * pad, height - 2 * pad - stripPx(), gapX(), gapY(), 0f, ImeLayout.rowWeights(letters)).map { r -> r.map { KeyBox(it.left + pad, it.top + pad + stripPx(), it.right + pad, it.bottom + pad + stripPx()) } }
        return try { GlideKeys.from(letters, b) } catch (e: IllegalArgumentException) { null }
    }

    // ------------------------------------------------------------------ drawing

    override fun onDraw(c: Canvas) {
        layoutKeys()
        c.drawColor(cBg)
        if (!drewOnce) { drewOnce = true; handler.post { onFirstDraw?.invoke() } }
        if (!blankSlate()) for ((i, row) in rows.withIndex()) for ((j, k) in row.withIndex()) { if (k.key != null) drawKey(c, k, rects[i][j], false, i == 0) }
        if (overlay == null) drawOverlay(c)
    }

    /**
     * Owner R18e: "for the setting to hide letters on the keyboard, don't even show the blank keys. just make it one solid blank slate". With the setting on, a page that has letter keys draws NO key at all (no shape,
     * no icon, no lit key): the key area is one solid colour. The keys are still where they were and still type and glide. The symbols and number pages have no letters to hide and show their keys as usual.
     */
    private fun blankSlate(): Boolean = hideLetters && rows.any { row -> row.any { isLetter(it) } }

    /** The dynamic layer: the lit key, the strip of guesses, the key bubble and the glide trail. Called by the overlay view (or by [onDraw] when there is none). */
    fun drawOverlay(c: Canvas) {
        layoutKeys()
        val lit = litKey
        if (lit != null && !blankSlate()) { val kb = boxOf(lit); if (kb != null) drawKey(c, lit, kb, true, rows[0].contains(lit)) }
        drawStrip(c)
        if (!gliding && keyPreview && !hideLetters) drawBubble(c)
        if (trailOn) drawTrail(c)
        drawPopup(c)
    }

    /** The corner radius of a letter key: about a fifth of its width, like the photo. */
    private fun keyRadius(): Float = Math.max(dp(5f), (pitch() - gapX()) * KEY_R_FRAC)

    /** The two round keys of the photo: ?123 / abc at the left of the bottom row and the action key (Enter) at the right of it. */
    private fun isPill(k: KeySpec): Boolean {
        val key = k.key ?: return false
        return key === Key.Go || ((key === Key.ToSymbols || key === Key.ToLetters) && rows.lastOrNull()?.firstOrNull() === k)
    }

    /** The small keys of the bottom row (comma, full stop, slash, at sign, .com) are the lighter slate in the photo; letters, digits, symbols and the space bar are the darker one. */
    private fun isPunctuation(k: KeySpec): Boolean { val t = (k.key as? Key.Ch)?.text ?: return false; return t != " " && rows.lastOrNull()?.contains(k) == true }

    private fun drawKey(c: Canvas, k: KeySpec, kb: KeyBox, lit: Boolean, topRow: Boolean) {
        val key = k.key ?: return
        box.set(kb.left, kb.top, kb.right, kb.bottom)
        val h = box.height()
        val pill = isPill(k)
        val r = if (pill) h / 2f else keyRadius()
        val locked = key === Key.Shift && shift == ShiftState.LOCK
        val pale = pill && key !== Key.Go
        val bg = when {
            key === Key.Go || locked -> cAccent
            pale -> cPale
            key === Key.Backspace || key === Key.Shift || key === Key.ToSymbols || key === Key.ToLetters || key === Key.NextKeyboard || isPunctuation(k) -> cSpecial
            else -> cKey
        }
        fill.color = if (!lit) bg else if (bg == cAccent || bg == cPale) lighten(bg, 0.3f) else cLit
        c.drawRoundRect(box, r, r, fill)
        val ink = if (bg == cAccent) cAccentText else if (bg == cPale) cPaleText else cText
        text.color = ink
        if (key === Key.NextKeyboard) { drawGlobe(c, box, ink); return }
        if (key === Key.Shift) { drawShift(c, box, ink, shift != ShiftState.OFF, locked); return }
        if (key === Key.Backspace) { drawBackspace(c, box, ink); return }
        if (key === Key.Go && k.label == "⏎") { drawEnter(c, box, ink); return }
        val t = (key as? Key.Ch)?.text
        if (t == " " && pad == Pad.NONE) return          // the space bar is blank, like the photo
        val label = if (key === Key.ToLetters) "ABC" else k.label
        text.textSize = h * (if (label.length > 1) 0.30f else if (topRow && ImeLayout.hasNumberRow(rows)) 0.52f else 0.44f)
        val fm = text.fontMetrics
        val low = (t == "." || t == ",") && rows.lastOrNull()?.contains(k) == true      // the photo sets the comma and the full stop low in their keys
        c.drawText(label, box.centerX(), if (low) box.top + h * 0.80f else box.centerY() - (fm.ascent + fm.descent) / 2f, text)
        if (!numberRow && topRow && page == Page.LETTERS && pad == Pad.NONE && label.length == 1) {   // without the number row: the digit a held top-row letter types, small in its corner
            val d = TOP_ROW.indexOf(label.lowercase())
            if (d >= 0) {
                text.color = cMuted; text.textSize = h * 0.22f
                val hf = text.fontMetrics
                c.drawText(DIGITS.substring(d, d + 1), box.right - dp(7f), box.top + dp(3f) - hf.ascent, text)
            }
        }
    }

    private val ico = Path()
    /** Shift: an arrow pointing up, drawn (no font symbol: the tablet's fonts may lack one): an outline while off, solid while on, with a bar under it when locked. */
    private fun drawShift(c: Canvas, b: RectF, ink: Int, solid: Boolean, locked: Boolean) {
        val a = Math.min(b.height() * 0.21f, b.width() * 0.24f)
        val cx = b.centerX(); val cy = b.centerY() - (if (locked) a * 0.12f else 0f)
        ico.reset()
        ico.moveTo(cx, cy - a); ico.lineTo(cx + a, cy + a * 0.05f); ico.lineTo(cx + a * 0.45f, cy + a * 0.05f); ico.lineTo(cx + a * 0.45f, cy + a * 0.85f)
        ico.lineTo(cx - a * 0.45f, cy + a * 0.85f); ico.lineTo(cx - a * 0.45f, cy + a * 0.05f); ico.lineTo(cx - a, cy + a * 0.05f); ico.close()
        if (solid) { fill.color = ink; c.drawPath(ico, fill) } else { line.color = ink; line.strokeWidth = dp(2f); c.drawPath(ico, line) }
        if (locked) { fill.color = ink; box2.set(cx - a * 0.45f, cy + a * 1.05f, cx + a * 0.45f, cy + a * 1.25f); c.drawRect(box2, fill) }
    }

    /** Backspace: a tag pointing left with a cross in it (an outline, like the photo). */
    private fun drawBackspace(c: Canvas, b: RectF, ink: Int) {
        val w = Math.min(b.width() * 0.46f, b.height() * 0.66f); val hh = w * 0.72f
        val l = b.centerX() - w / 2f; val rt = b.centerX() + w / 2f; val tp = b.centerY() - hh / 2f; val bt = b.centerY() + hh / 2f
        ico.reset(); ico.moveTo(l + w * 0.30f, tp); ico.lineTo(rt, tp); ico.lineTo(rt, bt); ico.lineTo(l + w * 0.30f, bt); ico.lineTo(l, b.centerY()); ico.close()
        line.color = ink; line.strokeWidth = dp(2f); c.drawPath(ico, line)
        val x0 = l + w * 0.6f; val d = hh * 0.17f
        c.drawLine(x0 - d, b.centerY() - d, x0 + d, b.centerY() + d, line); c.drawLine(x0 - d, b.centerY() + d, x0 + d, b.centerY() - d, line)
    }

    /** Enter: the return arrow (down, then left with a head), as in the photo. */
    private fun drawEnter(c: Canvas, b: RectF, ink: Int) {
        val a = Math.min(b.height() * 0.19f, b.width() * 0.19f)
        val cx = b.centerX(); val cy = b.centerY()
        line.color = ink; line.strokeWidth = dp(2.4f)
        ico.reset(); ico.moveTo(cx + a, cy - a * 0.9f); ico.lineTo(cx + a, cy + a * 0.35f); ico.lineTo(cx - a, cy + a * 0.35f); c.drawPath(ico, line)
        ico.reset(); ico.moveTo(cx - a * 0.3f, cy - a * 0.35f); ico.lineTo(cx - a, cy + a * 0.35f); ico.lineTo(cx - a * 0.3f, cy + a * 1.05f); c.drawPath(ico, line)
    }

    /** The globe on the next-keyboard key: a circle, an equator and two meridians (no font symbol: the tablet's fonts may lack one). */
    private fun drawGlobe(c: Canvas, b: RectF, ink: Int) {
        val r = Math.min(b.width(), b.height()) * 0.24f
        line.color = ink; line.strokeWidth = dp(1.6f)
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

    // ---- the strip: the toolbar icons at the left, the guesses in the rest
    private fun toolCount() = if (translateAvailable) 3 else 2
    private fun toolId(i: Int) = if (i == 0) TOOL_SETTINGS else if (i == 1) TOOL_CLIPBOARD else TOOL_TRANSLATE
    /** With nothing beside the toolbar (no guesses, no message) its icons are spread evenly across the strip, like the photo; with guesses they sit small at the left and the guesses fill the rest. */
    private fun spread(): Boolean = chips().isEmpty() && SystemClock.uptimeMillis() >= noWordUntil
    private val icon = RectF()
    private fun toolBox(i: Int): RectF {
        if (spread()) { val w = (width - 2 * pad) / toolCount().toFloat(); return RectF(pad + i * w, pad, pad + (i + 1) * w, stripPx() - gap) }
        val w = dp(TOOL_W_DP); return RectF(pad + i * w, pad, pad + (i + 1) * w - gap, stripPx() - gap)
    }
    private fun toolAt(x: Float, y: Float): Int {
        if (y >= stripPx() + pad) return -1
        for (i in 0 until toolCount()) { val r = toolBox(i); if (x >= r.left - gap / 2 && x < r.right + gap / 2) return i }
        return -1
    }
    private fun chipsLeft() = pad + toolCount() * dp(TOOL_W_DP) + gap
    /** What the chips show: the guesses, or, while a long press asks "remove this word for good?", the question's two answers. */
    private fun chips(): List<String> { val w = confirmWord; return if (w != null) listOf(context.getString(R.string.strip_remove, w), context.getString(R.string.strip_keep)) else cands }
    private fun chipBoxes(): List<RectF> {
        val h = stripPx(); val list = chips(); val n = list.size
        if (h <= 0f || n == 0 || width <= 0) return emptyList()
        val left = chipsLeft(); val cw = (width - pad - left - gap * (n - 1)) / n
        return List(n) { i -> RectF(left + i * (cw + gap), pad, left + i * (cw + gap) + cw, h - gap) }
    }
    private fun chipAt(x: Float, y: Float): Int {
        if (y >= stripPx() + pad || chips().isEmpty()) return -1
        val list = chips()
        for ((i, r) in chipBoxes().withIndex()) if (x >= r.left - gap / 2 && x < r.right + gap / 2) return if (list[i].isEmpty()) -1 else i       // (a box not filled yet takes no touch)
        return -1
    }

    private fun drawStrip(c: Canvas) {
        val h = stripPx(); if (h <= 0f) return
        val r = dp(6f)
        // the toolbar: settings, clipboard, (translate)
        for (i in 0 until toolCount()) {
            val tb = toolBox(i); val id = toolId(i)
            val lit = id == TOOL_TRANSLATE && translateOn
            val rr = Math.min(tb.height(), dp(36f)) / 2f
            icon.set(tb.centerX() - rr, tb.centerY() - rr, tb.centerX() + rr, tb.centerY() + rr)          // a square the icon is drawn in; a circle behind it only while pressed or lit (the photo's cyan button)
            if (lit || i == toolDown) {
                fill.color = if (lit) cAccent else cSpecial
                if (id == TOOL_TRANSLATE) { box.set(icon.centerX() - rr * 1.45f, icon.centerY() - rr, icon.centerX() + rr * 1.45f, icon.centerY() + rr); c.drawRoundRect(box, rr, rr, fill) }
                else c.drawCircle(icon.centerX(), icon.centerY(), rr, fill)
            }
            val ink = if (lit) cAccentText else cText
            when (id) { TOOL_SETTINGS -> drawGear(c, icon, ink); TOOL_CLIPBOARD -> drawClipboard(c, icon, ink); else -> drawTranslate(c, icon, ink) }
        }
        text.textSize = h * 0.45f
        val fm = text.fontMetrics
        val list = chips()
        val left = chipsLeft()
        if (list.isEmpty()) {
            val miss = SystemClock.uptimeMillis() < noWordUntil
            if (!miss) return                                                        // the icons are spread across the strip then (no hint text next to them)
            val msg = notice ?: context.getString(R.string.strip_no_word)
            text.color = if (miss) cAccent else cMuted; text.textSize = h * 0.38f
            c.drawText(TextUtils.ellipsize(msg, text, width - left - dp(8f), TextUtils.TruncateAt.END).toString(), (left + width) / 2f, (h - gap) / 2f + pad - (fm.ascent + fm.descent) / 2f, text)
            return
        }
        val boxes = chipBoxes()
        for ((i, w) in list.withIndex()) {
            if (w.isEmpty()) continue
            val rect = boxes[i]
            fill.color = if (i == stripDown) cLit else if (confirmWord != null && i == 0) cAccent else cKey
            c.drawRoundRect(rect, r, r, fill)
            if (i == accentChip && confirmWord == null) { line.strokeWidth = dp(1.2f); line.color = cAccent; box.set(rect); box.inset(dp(0.6f), dp(0.6f)); c.drawRoundRect(box, r, r, line) }
            text.color = if (confirmWord != null && i == 0) cAccentText else cText
            if (clipChip && i == 0) {                                                    // what was copied: a small clipboard, then the start of the text
                val iw = dp(18f); val ib = RectF(rect.left + dp(4f), rect.centerY() - iw / 2f, rect.left + dp(4f) + iw, rect.centerY() + iw / 2f)
                drawClipboard(c, ib, cAccent)
                val tx = rect.left + dp(4f) + iw + dp(2f)
                c.drawText(TextUtils.ellipsize(w, text, rect.right - tx - dp(6f), TextUtils.TruncateAt.MIDDLE).toString(), (tx + rect.right - dp(4f)) / 2f, rect.centerY() - (fm.ascent + fm.descent) / 2f, text)
                continue
            }
            c.drawText(TextUtils.ellipsize(w, text, rect.width() - dp(8f), TextUtils.TruncateAt.END).toString(), rect.centerX(), rect.centerY() - (fm.ascent + fm.descent) / 2f, text)
        }
    }

    /** A cog: eight teeth round a body with a hole, drawn (no font symbol: the tablet's fonts may lack one). */
    private fun drawGear(c: Canvas, b: RectF, ink: Int) {
        val cx = b.centerX(); val cy = b.centerY()
        val r = Math.min(b.width(), b.height()) * 0.36f; val body = r * 0.76f; val hole = r * 0.32f
        val d = Math.toRadians(15.0); val t = Math.toRadians(8.5)
        ico.reset(); ico.fillType = Path.FillType.EVEN_ODD
        for (k in 0 until 8) {
            val a = Math.PI / 4.0 * k
            val x1 = (cx + Math.cos(a - d) * body).toFloat(); val y1 = (cy + Math.sin(a - d) * body).toFloat()
            if (k == 0) ico.moveTo(x1, y1) else ico.lineTo(x1, y1)
            ico.lineTo((cx + Math.cos(a - t) * r).toFloat(), (cy + Math.sin(a - t) * r).toFloat())
            ico.lineTo((cx + Math.cos(a + t) * r).toFloat(), (cy + Math.sin(a + t) * r).toFloat())
            ico.lineTo((cx + Math.cos(a + d) * body).toFloat(), (cy + Math.sin(a + d) * body).toFloat())
        }
        ico.close(); ico.addCircle(cx, cy, hole, Path.Direction.CCW)
        fill.color = ink; c.drawPath(ico, fill)
        ico.fillType = Path.FillType.WINDING
    }

    /** A clipboard: a board, its clip and two lines of text. */
    private fun drawClipboard(c: Canvas, b: RectF, ink: Int) {
        val w = Math.min(b.width(), b.height()) * 0.52f; val h = w * 1.2f
        line.color = ink; line.strokeWidth = dp(1.8f)
        box2.set(b.centerX() - w / 2f, b.centerY() - h / 2f + dp(2f), b.centerX() + w / 2f, b.centerY() + h / 2f + dp(2f)); c.drawRoundRect(box2, dp(3f), dp(3f), line)
        fill.color = ink; box.set(b.centerX() - w * 0.28f, box2.top - dp(2.5f), b.centerX() + w * 0.28f, box2.top + dp(2.5f)); c.drawRoundRect(box, dp(2f), dp(2f), fill)
        c.drawLine(box2.left + w * 0.22f, box2.top + h * 0.45f, box2.right - w * 0.22f, box2.top + h * 0.45f, line)
        c.drawLine(box2.left + w * 0.22f, box2.top + h * 0.72f, box2.right - w * 0.40f, box2.top + h * 0.72f, line)
    }

    /** The translate icon: an A, an arrow and ES (English to Spanish), drawn with lines for the arrow (no font symbol). */
    private fun drawTranslate(c: Canvas, b: RectF, ink: Int) {
        val h = b.height()
        text.color = ink; text.typeface = Typeface.DEFAULT_BOLD; text.textSize = h * 0.40f; text.textAlign = Paint.Align.LEFT
        val fm = text.fontMetrics; val base = b.centerY() - (fm.ascent + fm.descent) / 2f
        val wA = text.measureText("A"); val wEs = text.measureText("ES"); val arrow = h * 0.30f; val g = h * 0.08f
        var x = b.centerX() - (wA + g + arrow + g + wEs) / 2f
        c.drawText("A", x, base, text); x += wA + g
        line.color = ink; line.strokeWidth = dp(1.8f)
        val ay = b.centerY(); c.drawLine(x, ay, x + arrow, ay, line)
        c.drawLine(x + arrow - h * 0.08f, ay - h * 0.08f, x + arrow, ay, line); c.drawLine(x + arrow - h * 0.08f, ay + h * 0.08f, x + arrow, ay, line)
        x += arrow + g
        c.drawText("ES", x, base, text)
        text.textAlign = Paint.Align.CENTER; text.typeface = Typeface.DEFAULT
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

    /**
     * Debug builds only (the emulator tools read it from logcat, tag SwoopDiag): the centre of every key of the page shown, in SCREEN pixels, as the app itself laid them out, so a test driver never has to
     * repeat the geometry formulas. Format: `keymap page=letters key=x,y ...` where a one-character key is `k<hex code point>` and the others are shift, back, sym, abc, go, globe. Geometry only, never text.
     */
    fun keymapLine(): String? {
        if (width <= 0 || height <= 0) return null
        layoutKeys()
        val loc = IntArray(2); getLocationOnScreen(loc)
        val sb = StringBuilder("keymap page=").append(page.name.lowercase()).append(" shift=").append(if (shift == ShiftState.OFF) 0 else 1)
        for ((i, row) in rows.withIndex()) for ((j, s) in row.withIndex()) {
            val key = s.key ?: continue
            val name = when (key) {
                is Key.Ch -> if (key.text.codePointCount(0, key.text.length) == 1) "k" + Integer.toHexString(key.text.codePointAt(0)) else "t" + key.text.length + "_" + Integer.toHexString(key.text.hashCode() and 0xffff)
                Key.Shift -> "shift"; Key.Backspace -> "back"; Key.ToSymbols -> "sym"; Key.ToLetters -> "abc"; Key.Go -> "go"; Key.NextKeyboard -> "globe"
                else -> continue
            }
            val b = rects[i][j]
            sb.append(' ').append(name).append('=').append(Math.round(loc[0] + b.centerX)).append(',').append(Math.round(loc[1] + b.centerY))
        }
        return sb.toString()
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
        if (numberRow || page != Page.LETTERS || pad != Pad.NONE || k == null || k.label.length != 1) return null
        val i = TOP_ROW.indexOf(k.label.lowercase()); return if (i >= 0 && rows[0].contains(k)) DIGITS.substring(i, i + 1) else null
    }

    private fun light(k: KeySpec?) { litKey = k; dyn(); handler.removeCallbacks(unlight); if (k != null) handler.postDelayed(unlight, FLASH_MS) }
    private fun haptic() { if (hapticsOn) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    private fun click() { if (soundOn) try { (context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager).playSoundEffect(android.media.AudioManager.FX_KEYPRESS_STANDARD) } catch (_: Exception) { } }
    private fun clearHold() { handler.removeCallbacks(hold); holdAlt = null; bubbleKey = null }
    // ---- the symbols grid: HOLD the full stop or the comma (the 123 key is gone: owner, 2026-11-17)
    private class SymbolPopup(val owner: KeySpec, val chars: List<String>, val cells: List<RectF>, val panel: RectF) { var sel = -1 }
    private var popup: SymbolPopup? = null

    private fun isSymbolKey(k: KeySpec?): Boolean {
        val t = (k?.key as? Key.Ch)?.text ?: return false
        return (t == "." || t == ",") && pad == Pad.NONE
    }

    private fun openPopup(k: KeySpec) {
        val kb = boxOf(k) ?: return
        val grid = ImeLayout.symbolRows
        val cols = grid.maxOf { it.size }
        val cw = minOf(dp(44f), (width - 2 * pad) / cols.toFloat()); val chh = dp(46f); val m = dp(6f)
        val pw = cw * cols + 2 * m; val ph = chh * grid.size + 2 * m
        val left = (kb.centerX - pw / 2f).coerceIn(0f, maxOf(0f, width - pw)); val top = maxOf(0f, kb.top - ph - dp(8f))
        val cells = ArrayList<RectF>(); val chars = ArrayList<String>()
        for ((r, row) in grid.withIndex()) {
            val x0 = left + m + (cols - row.size) * cw / 2f
            for ((c, ch) in row.withIndex()) { cells.add(RectF(x0 + c * cw, top + m + r * chh, x0 + (c + 1) * cw, top + m + (r + 1) * chh)); chars.add(ch) }
        }
        popup = SymbolPopup(k, chars, cells, RectF(left, top, left + pw, top + ph))
        bubbleKey = null; holdAlt = null
        haptic(); dyn()
    }

    private fun popupSelect(p: SymbolPopup, x: Float, y: Float): Int {
        for ((i, r) in p.cells.withIndex()) if (x >= r.left - dp(2f) && x < r.right + dp(2f) && y >= r.top - dp(2f) && y < r.bottom + dp(2f)) return i
        return -1
    }

    private fun drawPopup(c: Canvas) {
        val p = popup ?: return
        val r = dp(12f)
        fill.color = cBubble; c.drawRoundRect(p.panel, r, r, fill)
        line.strokeWidth = dp(1.4f); line.color = cAccent; c.drawRoundRect(p.panel, r, r, line)
        for ((i, cell) in p.cells.withIndex()) {
            val on = i == p.sel
            if (on) { fill.color = cAccent; box.set(cell); box.inset(dp(2f), dp(2f)); c.drawRoundRect(box, dp(8f), dp(8f), fill) }
            text.color = if (on) cAccentText else cText; text.textSize = cell.height() * 0.52f
            val fm = text.fontMetrics
            c.drawText(p.chars[i], cell.centerX(), cell.centerY() - (fm.ascent + fm.descent) / 2f, text)
        }
    }

    private fun holdFired() {
        val k0 = pressedLetter ?: down ?: return
        if (isSymbolKey(k0)) { openPopup(k0); return }
        val k = k0
        val alt = altOf(k) ?: return
        holdAlt = alt; bubbleKey = k; bubbleText = alt; dyn(); haptic()
    }

    @SuppressLint("ClickableViewAccessibility") // KB-305 adds TalkBack key descriptions; this view is a keyboard, not a button
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { requestUnbufferedDispatch(e); activeId = e.getPointerId(0); pressDown(e.getX(0), e.getY(0)) }   // unbuffered: every touch sample at once, not batched to the frame (a fast trail)
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (gliding) return true                          // a glide in progress is never cut short by a second touch (a resting palm)
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
        if (y < stripPx() + pad) {                                    // the strip was touched: a toolbar icon or a guess
            toolDown = toolAt(x, y); stripDown = if (toolDown >= 0) -1 else chipAt(x, y); stripLong = false
            if (toolDown >= 0) haptic()
            if (stripDown >= 0 && confirmWord == null) handler.postDelayed(stripHold, STRIP_HOLD_MS)
            dyn(); return
        }
        removeCallbacks(fadeStep); gn = 0; fadeStart = 0L
        val k = keyAt(x, y); down = k; light(k)
        spaceDown = spaceSlideOn && (k?.key as? Key.Ch)?.text == " "; sliding = false; slideStartX = x; slideAnchorX = x
        pressedLetter = if (isLetter(k)) k else null
        if (isBubbleKey(k)) { bubbleKey = k; bubbleText = k!!.label }
        spaceIsDown = (k?.key as? Key.Ch)?.text == " "; spaceHeld = false
        if (spaceIsDown) handler.postDelayed(spaceHold, SPACE_HOLD_MS)
        if (k != null) { haptic(); click(); if (altOf(k) != null || isSymbolKey(k)) handler.postDelayed(hold, HOLD_MS) }
        if (k?.key === Key.Backspace) { listener?.onKey(Key.Backspace); repeatStartAt = SystemClock.uptimeMillis(); handler.postDelayed(repeat, BackspaceRepeat.FIRST_MS) }
        downX = x; downY = y
        if (glideEnabled && page == Page.LETTERS && pressedLetter != null) { armed = true; glideDown = k; addPoint(x, y) }
    }

    private fun pressMove(e: MotionEvent, i: Int) {
        val x = e.getX(i); val y = e.getY(i)
        popup?.let { p -> val ns = popupSelect(p, x, y); if (ns != p.sel) { p.sel = ns; dyn() }; return }
        if (stripDown >= 0 || toolDown >= 0) return
        if (armed) {                                     // a letter: nothing happens until the finger leaves its key (a glide) or lifts (a tap)
            for (h in 0 until e.historySize) addPoint(e.getHistoricalX(i, h), e.getHistoricalY(i, h))
            addPoint(x, y)
            if (!gliding && movedFar(x, y)) beginGlide()          // a glide starts as soon as the finger has really moved: no waiting for the edge of the key
            if (gliding) { dyn(); maybePreview() }
            return
        }
        if (pressedLetter != null) return                // (glide off) a letter: a tap types the letter it went down on
        if (spaceDown) {                                 // press the space key and slide sideways: the text cursor follows the finger
            if (!sliding && Math.abs(x - slideStartX) > dp(SLIDE_START_DP)) { sliding = true; handler.removeCallbacks(spaceHold); slideAnchorX = x; litKey = down; dyn() }
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
        handler.removeCallbacks(spaceHold)
        popup?.let { p ->                                  // the symbols grid is open: the cell under the finger is typed; lifting on the held key itself types that key; anywhere else types nothing
            val sel = popupSelect(p, x, y); popup = null; clearHold(); down = null; pressedLetter = null; litKey = null; spaceDown = false; dyn()
            if (sel >= 0) emit(Key.Ch(p.chars[sel])) else if (keyAt(x, y) === p.owner) emit(p.owner.key!!)
            return
        }
        if (spaceHeld) { spaceHeld = false; spaceIsDown = false; spaceDown = false; sliding = false; down = null; litKey = null; dyn(); return }    // the picker was opened: no space is typed
        spaceIsDown = false
        handler.removeCallbacks(stripHold)
        if (toolDown >= 0) { val t = toolAt(x, y); val was = toolDown; toolDown = -1; dyn(); if (t == was) listener?.onToolbar(toolId(was)); return }
        if (stripDown >= 0) {
            val c = chipAt(x, y); val was = stripDown; stripDown = -1; dyn()
            if (stripLong) { stripLong = false; return }             // the long press already asked its question
            val cw = confirmWord
            if (c == was && cw != null) { confirmWord = null; listener?.onRemoveWord(cw, c == 0) }
            else if (c == was && c in cands.indices) { if (clipChip && c == 0) listener?.onClipChip() else listener?.onCandidate(cands[c]) }
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
        handler.removeCallbacks(stripHold); toolDown = -1; handler.removeCallbacks(repeat); handler.removeCallbacks(spaceHold); clearHold(); popup = null; spaceIsDown = false; spaceHeld = false; down = null; pressedLetter = null; armed = false; stripDown = -1; spaceDown = false; sliding = false; litKey = null; dyn()
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

    /**
     * Owner, 2026-11-17: "always glide unless I am clearly only pressing one key". A touch is a tap only when every point of it stayed on the key it went down on (plus a margin of
     * [TAP_MARGIN_PITCH] key widths for a thumb that rolls) or the whole path is shorter than [MIN_GLIDE_PITCHES] pitches. Before, a quick swipe to the NEXT key ("as", "it", "no") was shorter than
     * 1.2 pitches, counted as a tap and typed one letter.
     */
    private fun stayedOnKey(k: KeySpec?): Boolean {
        if (k == null) return true
        val b = boxOf(k) ?: return pathPitches() < MIN_GLIDE_PITCHES
        val m = pitch() * TAP_MARGIN_PITCH
        for (i in 0 until gn) if (gx[i] < b.left - m || gx[i] > b.right + m || gy[i] < b.top - m || gy[i] > b.bottom + m) return pathPitches() < MIN_GLIDE_PITCHES
        return true
    }

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
        if (stayedOnKey(k)) {                            // a tap that slid a little (the finger never left its own key): the key it went down on
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
            // ?123 on the letters opens the first symbols page; the =\< key there opens the second one and ?123 on the second goes back to the first (the same Key.ToSymbols cycles them, like Gboard)
            Key.ToSymbols -> { page = if (page == Page.SYMBOLS) Page.SYMBOLS2 else Page.SYMBOLS; rebuildRows() }
            Key.ToLetters -> { page = Page.LETTERS; rebuildRows() }
            else -> {
                listener?.onKey(key)
                // Owner, 2026-11-17 (like Gboard): after ONE special character the keyboard goes straight back to the letters (digits stay: several are typed in a row; Backspace and Shift change nothing)
                if (page != Page.LETTERS && key is Key.Ch && !(key.text.length == 1 && key.text[0].isDigit())) { page = Page.LETTERS; rebuildRows() }
            }
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
    /** The guesses the strip shows now. */
    fun currentCandidates(): List<String> = cands
    fun popupOpenForTest(): Boolean = popup != null
    fun popupCellCenterForTest(ch: String): Pair<Float, Float>? { val p = popup ?: return null; val i = p.chars.indexOf(ch); return if (i < 0) null else p.cells[i].let { it.centerX() to it.centerY() } }
    fun toolCenterForTest(tool: Int): Pair<Float, Float>? { for (i in 0 until toolCount()) if (toolId(i) == tool) return toolBox(i).let { it.centerX() to it.centerY() }; return null }
    fun chipCenterForTest(i: Int): Pair<Float, Float>? { val r = chipBoxes().getOrNull(i) ?: return null; return r.centerX() to r.centerY() }

    companion object {
        /** Sliding over the space key: the finger must move this far sideways before it is a cursor slide and not a space, and every step of this width moves the cursor one character. */
        const val SLIDE_START_DP = 10f
        const val SLIDE_STEP_DP = 11f
        /** How long a pressed key stays lit. */
        const val FLASH_MS = 130L
        const val REPEAT_FIRST_MS = BackspaceRepeat.FIRST_MS
        /** Holding a top-row letter this long types its digit. */
        const val HOLD_MS = 420L
        /** Holding the space bar this long (without sliding) opens the keyboard picker. */
        const val SPACE_HOLD_MS = 600L
        private const val TOP_ROW = "qwertyuiop"
        private const val DIGITS = "1234567890"
        /** The strip above the keys: the toolbar icons (settings, clipboard, translate) and the guesses. */
        const val STRIP_H_DP = 40f
        /** The photo's geometry: the gap across a row is this share of the key pitch, the gap between rows this many times the gap across, a row is this many pitches tall (within the dp bounds), a letter key's corner is this share of its width. */
        const val GAP_FRAC = 0.14f
        const val GAP_Y_MUL = 1.5f
        const val ROW_H_PITCHES = 1.35f
        const val ROW_H_MIN_DP = 44f
        const val ROW_H_MAX_DP = 56f
        const val KEY_R_FRAC = 0.22f
        /** One toolbar icon is this wide. */
        const val TOOL_W_DP = 40f
        const val STRIP_HOLD_MS = 500L
        const val TOOL_SETTINGS = 1
        const val TOOL_CLIPBOARD = 2
        const val TOOL_TRANSLATE = 3
        /** A finger that has moved this far (dp) from where it went down has started a glide (a short drag that ends up under 1.2 key widths is still a tap). */
        const val GLIDE_START_DP = 5f
        /** A path shorter than this (in key pitches) is a tap that slid a little, not a glide. */
        const val MIN_GLIDE_PITCHES = 0.45f
        /** A path that never strays further than this (key widths) outside the key it started on is a tap. */
        const val TAP_MARGIN_PITCH = 0.22f
        const val TRAIL_FADE_MS = 260L
        const val NO_WORD_MS = 1500L
        const val NOTICE_MS = 5000L
        /** The live guess while gliding: at most this often, and only after this many new touch points. */
        const val PREVIEW_MS = 110L
        const val PREVIEW_POINTS = 4
        private const val MAX_TRAIL_POINTS = 4096
    }
}
