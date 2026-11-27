package app.swoop.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.os.SystemClock
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import app.kestrel.core.input.FloatingMath

/** The transparent layer above the keys: draws what changes while a finger is down ([KeysView.drawOverlay]) and never takes a touch. */
class KeyOverlay(context: Context, private val keys: KeysView) : View(context) {
    init { isClickable = false; isFocusable = false; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    /**
     * Exactly as big as the keys and never bigger: a plain View that is asked to fill its parent takes the WHOLE screen height when the parent only says "at most" (that is how the keyboard window is measured), and
     * the keyboard window would cover the app. The keys view sits before this one in the pane, so it is measured already.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) { setMeasuredDimension(keys.measuredWidth, keys.measuredHeight) }
    override fun onDraw(c: Canvas) { keys.drawOverlay(c) }
    override fun onTouchEvent(event: MotionEvent): Boolean = false
}

/**
 * The neon bar on the top edge of the keyboard (N3). Docked: LONG PRESS it (or drag it up) and the keyboard lifts off and follows the finger. Floating: drag it to move the keyboard, double-tap it to dock again.
 * It only reports what the finger did ([Host]); [KeyboardRoot] owns the geometry.
 */
class GripBar(context: Context) : View(context) {
    interface Host {
        /** The docked keyboard is to detach with the finger at screen position ([rawX], [rawY]). */
        fun gripDetach(rawX: Float, rawY: Float)
        /** The floating keyboard is dragged by ([dx], [dy]) pixels. */
        fun gripDrag(dx: Float, dy: Float)
        /** The finger lifted after a drag. */
        fun gripDrop()
        /** A double tap: dock. */
        fun gripDock()
        /** Docked: the end arrow on [side] (0 left, 1 right) was dragged [dx] pixels from where the finger went down; the width at that moment is the reference. */
        fun gripSqueezeStart(side: Int) {}
        /** Docked: the arrow is dragged ([dx], [dy]) pixels from where the finger went down: sideways changes the width, UP makes the keyboard taller and down shorter (owner R19c). */
        fun gripSqueeze(side: Int, dx: Float, dy: Float) {}
        /** Docked: two fingers pinch the bar; [scale] is the distance between them now over the distance when they went down. */
        fun gripPinchStart() {}
        fun gripPinch(scale: Float) {}
        /** Docked and narrow: the bar is dragged sideways by [dx] pixels (the whole keyboard follows). */
        fun gripSlide(dx: Float) {}
        /** The squeeze / slide gesture ended: save. */
        fun gripSqueezeEnd() {}
        /** Double tap on a narrow docked bar: full width again. */
        fun gripFullWidth() {}
        /** True when the docked keyboard is narrower than the screen. */
        fun gripSqueezed(): Boolean = false
        /** True when there is a full-size keyboard to snap back to (squeezed, resized taller or shorter, or floating): the bar then shows its "Full size" pill. */
        fun gripCanRestore(): Boolean = false
        /** The "Full size" pill was tapped: back to the full-width docked keyboard. */
        fun gripRestore() {}
    }
    var host: Host? = null
    var accent = 0xFF00DDE6.toInt(); set(v) { field = v; invalidate() }
    var floating = false; set(v) { field = v; invalidate() }

    private val d = resources.displayMetrics.density
    private val handler = Handler(Looper.getMainLooper())
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val box = RectF()
    private var downX = 0f; private var downY = 0f; private var lastX = 0f; private var lastY = 0f
    private var moved = false; private var dragging = false
    private var mode = 0                 // 0 none/detach, 1 squeeze from the left arrow, 2 from the right arrow, 3 pinch, 4 slide
    private var pinchStart = 0f; private var pinchPointer = -1
    private var lastTapAt = Long.MIN_VALUE / 2
    private val longPress = Runnable { Diag.d("grip long press fired floating=$floating dragging=$dragging"); if (!floating && !dragging) { performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); dragging = true; moved = true; host?.gripDetach(lastX, lastY) } }

    init { contentDescription = "Keyboard handle. Press and hold, then drag, to float the keyboard."; isClickable = true }

    override fun onMeasure(w: Int, h: Int) { setMeasuredDimension(MeasureSpec.getSize(w), (HEIGHT_DP * d).toInt()) }

    private val label = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD }
    private val pillBox = RectF()

    /**
     * The "Full size" pill (owner R18c: "an easy way to snap it back to full size"): in the middle of the bar, between the squeeze arrows, whenever the keyboard is squeezed or floating. One tap puts the full-width docked
     * keyboard back (a double tap on the bar still does). Null when there is nothing to restore or the bar is too narrow for it.
     */
    private fun pillRect(): RectF? {
        if (host?.gripCanRestore() != true) return null
        val zone = if (floating) 0f else ARROW_ZONE_DP * d
        val w = Math.min(PILL_W_DP * d, width - 2 * zone - 8 * d)
        if (w < 26 * d) return null
        pillBox.set(width / 2f - w / 2f, 2 * d, width / 2f + w / 2f, height - 2 * d)
        return pillBox
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f; val cy = height / 2f
        val len = (if (floating) 72f else 52f) * d; val th = 4f * d
        val pr = pillRect()
        if (pr != null) drawPill(c, pr) else {
            // the glow: a wider, fainter bar behind the bright one
            paint.style = Paint.Style.FILL
            paint.color = (accent and 0x00FFFFFF) or ((if (floating) 90 else 55) shl 24)
            box.set(cx - len / 2 - 5 * d, cy - th / 2 - 3 * d, cx + len / 2 + 5 * d, cy + th / 2 + 3 * d); c.drawRoundRect(box, 8 * d, 8 * d, paint)
            paint.color = if (floating) lighten(accent) else accent
            box.set(cx - len / 2, cy - th / 2, cx + len / 2, cy + th / 2); c.drawRoundRect(box, th / 2, th / 2, paint)
        }
        if (!floating) {                       // the squeeze arrows at both ends: drag one inwards (or pinch the bar) to make the keyboard narrower
            paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND; paint.strokeWidth = 2.4f * d; paint.color = accent
            val a = 5f * d
            for (side in 0..1) {
                val x = if (side == 0) 20f * d else width - 20f * d; val dir = if (side == 0) 1f else -1f
                path.reset(); path.moveTo(x - dir * a * 0.6f, cy - a); path.lineTo(x + dir * a * 0.6f, cy); path.lineTo(x - dir * a * 0.6f, cy + a); c.drawPath(path, paint)
                path.reset(); path.moveTo(x - dir * a * 2.2f, cy - a); path.lineTo(x - dir * a, cy); path.lineTo(x - dir * a * 2.2f, cy + a); c.drawPath(path, paint)
            }
            paint.style = Paint.Style.FILL
        }
    }
    private val path = Path()

    /** The pill: a faint accent fill, an accent outline, two arrows pointing outwards (drawn: the tablet's fonts may lack the symbol) and, when it is wide enough, the words "Full size". */
    private fun drawPill(c: Canvas, r: RectF) {
        val rad = r.height() / 2f
        paint.style = Paint.Style.FILL; paint.color = (accent and 0x00FFFFFF) or (70 shl 24); c.drawRoundRect(r, rad, rad, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.4f * d; paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND; paint.color = accent
        c.drawRoundRect(r, rad, rad, paint)
        val words = context.getString(R.string.grip_full_size)
        label.textSize = 10.5f * d; label.color = accent
        val arrowW = 15f * d
        val showWords = r.width() >= 70f * d
        val total = if (showWords) arrowW + 5f * d + label.measureText(words) else arrowW
        var x = r.centerX() - total / 2f; val cy = r.centerY(); val a = 3f * d
        c.drawLine(x, cy, x + arrowW, cy, paint)                                                    // the double-headed arrow
        path.reset(); path.moveTo(x + a, cy - a); path.lineTo(x, cy); path.lineTo(x + a, cy + a); c.drawPath(path, paint)
        path.reset(); path.moveTo(x + arrowW - a, cy - a); path.lineTo(x + arrowW, cy); path.lineTo(x + arrowW - a, cy + a); c.drawPath(path, paint)
        if (showWords) { x += arrowW + 5f * d; val fm = label.fontMetrics; c.drawText(words, x, cy - (fm.ascent + fm.descent) / 2f, label) }
        paint.style = Paint.Style.FILL
    }

    private fun half(x: Int) = x + (255 - x) / 2
    private fun lighten(c: Int): Int = 0xFF000000.toInt() or (half((c shr 16) and 255) shl 16) or (half((c shr 8) and 255) shl 8) or half(c and 255)

    private fun pinchDistance(e: MotionEvent): Float = if (e.pointerCount < 2) 0f else Math.abs(e.getRawX(0) - e.getRawX(1)).let { dx -> if (Build.VERSION.SDK_INT >= 29) dx else dx }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val rx = e.rawX; val ry = e.rawY
        if (!floating) {
            // two fingers on the bar: pinch to squeeze the keyboard
            if (e.actionMasked == MotionEvent.ACTION_POINTER_DOWN && e.pointerCount == 2) {
                handler.removeCallbacks(longPress); mode = 3; pinchStart = Math.max(pinchDistance(e), 1f); moved = true; host?.gripPinchStart(); return true
            }
            if (mode == 3) {
                when (e.actionMasked) {
                    MotionEvent.ACTION_MOVE -> if (e.pointerCount >= 2) host?.gripPinch(pinchDistance(e) / pinchStart)
                    MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { host?.gripSqueezeEnd(); mode = 0; moved = false; dragging = false }
                }
                return true
            }
        }
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mode = 0
                if (!floating) {
                    val z = ARROW_ZONE_DP * d
                    if (e.x < z) mode = 1 else if (e.x > width - z) mode = 2
                    if (mode != 0) { host?.gripSqueezeStart(mode - 1); downX = rx; downY = ry; lastX = rx; lastY = ry; moved = false; dragging = false; parent?.requestDisallowInterceptTouchEvent(true); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); return true }
                }
                Diag.d("grip down raw=${rx.toInt()},${ry.toInt()} floating=$floating")
                downX = rx; downY = ry; lastX = rx; lastY = ry; moved = false; dragging = false
                parent?.requestDisallowInterceptTouchEvent(true)
                if (!floating) handler.postDelayed(longPress, LONG_PRESS_MS)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = rx - downX; val dy = ry - downY
                if (mode == 1 || mode == 2) { moved = true; host?.gripSqueeze(mode - 1, dx, dy); return true }
                if (mode == 4) { host?.gripSlide(rx - lastX); lastX = rx; lastY = ry; return true }
                if (!floating && !dragging && host?.gripSqueezed() == true && Math.abs(dx) > SLOP_DP * 2 * d && Math.abs(dx) > Math.abs(dy)) { handler.removeCallbacks(longPress); mode = 4; moved = true; host?.gripSlide(rx - lastX); lastX = rx; lastY = ry; return true }
                if (floating) {
                    if (!moved && Math.hypot(dx.toDouble(), dy.toDouble()) > SLOP_DP * d) moved = true
                    if (moved) host?.gripDrag(rx - lastX, ry - lastY)
                } else if (!dragging) {
                    if (dy < -FloatingMath.DETACH_DRAG_DP * d) { handler.removeCallbacks(longPress); performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); dragging = true; moved = true; host?.gripDetach(rx, ry) }
                    else if (Math.hypot(dx.toDouble(), dy.toDouble()) > SLOP_DP * 2 * d) handler.removeCallbacks(longPress)     // a slide that is not up: not a long press
                } else host?.gripDrag(rx - lastX, ry - lastY)
                lastX = rx; lastY = ry
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPress)
                if (mode != 0) { host?.gripSqueezeEnd(); mode = 0; moved = false; dragging = false; return true }
                if (e.actionMasked == MotionEvent.ACTION_UP) {
                    val pr = pillRect()
                    if (!moved && !dragging && pr != null && e.x >= pr.left - 6 * d && e.x <= pr.right + 6 * d) { lastTapAt = Long.MIN_VALUE / 2; host?.gripRestore(); dragging = false; moved = false; return true }
                    if (!moved && !floating && host?.gripCanRestore() == true) { val now = SystemClock.uptimeMillis(); if (now - lastTapAt < DOUBLE_TAP_MS) { lastTapAt = Long.MIN_VALUE / 2; host?.gripFullWidth() } else lastTapAt = now }
                    if (moved) host?.gripDrop()
                    else if (floating) { val now = SystemClock.uptimeMillis(); if (now - lastTapAt < DOUBLE_TAP_MS) { lastTapAt = Long.MIN_VALUE / 2; host?.gripDock() } else lastTapAt = now }
                }
                dragging = false; moved = false
            }
        }
        return true
    }

    override fun onDetachedFromWindow() { handler.removeCallbacks(longPress); super.onDetachedFromWindow() }

    companion object {
        const val HEIGHT_DP = 22f
        /** The ends of the bar (this wide) are the squeeze arrows. */
        const val ARROW_ZONE_DP = 52f
        const val LONG_PRESS_MS = 350L
        const val SLOP_DP = 4f
        const val DOUBLE_TAP_MS = 320L
        /** The "Full size" pill is this wide at most. */
        const val PILL_W_DP = 92f
    }
}

/**
 * What the input method shows (docs/FLOATING.md): [panel] = the neon bar above the keys (and the overlay layer exactly on top of them). Docked it is the keyboard at the bottom, full width, as always. Floating the window is
 * the whole screen, transparent, and the panel stands where the person put it, at the size they chose; four corner marks resize it, and the service tells Android that everything outside the panel (and the
 * corner grab squares) belongs to the app behind ([touchRegion], [FloatingMath]).
 */
class KeyboardRoot(context: Context) : FrameLayout(context), GripBar.Host {
    interface Host {
        /** The keyboard docked or floated: the service changes its window to fit. */
        fun modeChanged(floating: Boolean)
        /** The panel was moved or resized for good (a finger lifted): the decoder needs the new key positions. */
        fun geometryChanged()
        /**
         * Docked, a drag of the bar's arrow that changes the height needs room above the keyboard to grow into: the window is the whole screen (transparent, only the keyboard takes touches, the app is not resized) until the finger lifts (owner R19c).
         * Floating has the whole-screen window anyway ([modeChanged]).
         */
        fun windowTall(tall: Boolean) {}
    }

    val keys = KeysView(context)
    val overlay = KeyOverlay(context, keys)
    /** The clipboard: it covers the keys while it is open. */
    val clipboard = ClipboardPanel(context, keys)
    val grip = GripBar(context)
    private val pane = FrameLayout(context)
    private val panel = LinearLayout(context)
    var host: Host? = null

    private val d = resources.displayMetrics.density
    /** The neon bar and floating are on (Settings). */
    var floatEnabled = true; set(v) { field = v; grip.visibility = if (v) View.VISIBLE else View.GONE; if (!v && floating) setFloating(false); requestLayout() }
    var floating = false; private set
    /** The copy in the settings screen: shows the bar but never floats. */
    var previewOnly = false
    private var posX = 0.5f; private var posY = 0.8f          // where the panel stands: a fraction of the free room across and down (kept as a float so that holding still never moves it)
    private var wDp = 0f                                        // 0 = the default width for this screen
    private var hMul = 1f
    private var settings: Settings? = null
    /** The docked keyboard's width as a fraction of the screen (1 = full width) and where it stands (0..1 of the free room): squeezed with two fingers or the bar's arrows, moved by dragging the bar. */
    private var dockFrac = 1f; private var dockX = 0.5f
    /** The docked keyboard's row height multiplier (owner R19c: it can be made taller or shorter at the bottom), on top of the key height setting. 1 = as the settings say. */
    private var dockHMul = 1f
    /** True while a drag of the bar's arrow changes the height: the window is the whole screen so the keyboard can grow upwards ([Host.windowTall]). */
    var dockResizing = false; private set
    /** The panel's height when that drag began: the part of the screen the app is already laid out above, and kept so while the finger drags. */
    private var coveredH = 0
    fun dockCoveredHeight() = coveredH
    /** The window is as tall as the screen: floating, or a docked height drag in progress. */
    val windowTall: Boolean get() = floating || dockResizing
    val squeezed: Boolean get() = !floating && (if (previewing) pvW < 0.995f * screenW() else dockFrac < 0.995f)
    private var sqStartW = 0; private var sqStartLeft = 0; private var sqStartH = 0; private var pinchW0 = 0

    // ---- the live picture of a resize (owner R18c: "it jitters as i'm moving it to resize. make it resize smoothly")
    // While a finger drags a corner, an arrow of the bar, or pinches it, nothing is laid out again and nothing is measured: the finished panel is only scaled and moved on the GPU (view transforms), once per touch
    // event, so the weak tablet has nothing to compute between two frames and the edge follows the finger exactly. When the finger lifts the new size is laid out ONCE, with the keys at their proper shape.
    private var previewing = false
    private var pvLeft = 0; private var pvTop = 0; private var pvW = 0; private var pvH = 0           // the rectangle the panel is meant to cover now, in this view's pixels
    private fun setPreview(l: Int, t: Int, w: Int, h: Int) {
        val lw = panel.width; val lh = panel.height
        if (lw <= 0 || lh <= 0 || w <= 0 || h <= 0) return
        pvLeft = l; pvTop = t; pvW = w; pvH = h; previewing = true
        panel.pivotX = 0f; panel.pivotY = 0f
        panel.scaleX = w / lw.toFloat(); panel.scaleY = h / lh.toFloat()
        panel.translationX = (l - panel.left).toFloat(); panel.translationY = (t - panel.top).toFloat()
        invalidate()
    }
    private fun clearPreview() {
        if (!previewing) return
        previewing = false
        panel.scaleX = 1f; panel.scaleY = 1f; panel.translationX = 0f; panel.translationY = 0f
    }
    /** How far the keyboard was dragged since it detached: letting go near the bottom docks it only after a real drag, so a long press that is released at once leaves it floating. */
    private var travelled = 0f

    /**
     * Android 15+ (edge to edge, and this app targets 36): the system navigation bar is drawn OVER the bottom of the keyboard window, so without this the bottom row (?123, space, Enter) sat under it. The keys stand
     * above that bar and the keyboard's own colour fills the strip behind it. Zero before Android 15, where the window already ends above the bar (the Android 10 tablet), and whenever the bar does not overlap.
     */
    private var navBottom = 0; private var navLeft = 0; private var navRight = 0          // (a phone turned sideways with three-button navigation has the bar at a side)

    // the corner grab
    private var resizeCorner = -1
    private var rsLeft = 0; private var rsTop = 0; private var rsW = 0; private var rsH = 0; private var rsX = 0f; private var rsY = 0f
    private val mark = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val markPath = Path()

    init {
        Diag.init(context)
        pane.addView(keys, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        pane.addView(overlay, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        pane.addView(clipboard, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        keys.overlay = overlay
        panel.orientation = LinearLayout.VERTICAL
        panel.addView(grip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        panel.addView(pane, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        grip.host = this
        addView(panel, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        setWillNotDraw(false)
        setOnApplyWindowInsetsListener { _, ins ->
            if (Build.VERSION.SDK_INT >= 35) {
                val n = ins.getInsets(android.view.WindowInsets.Type.navigationBars())
                if (n.bottom != navBottom || n.left != navLeft || n.right != navRight) { navBottom = n.bottom; navLeft = n.left; navRight = n.right; applyMode() }
            }
            ins
        }
        keys.onPaletteChanged = { panel.background = panelBackground(); background = rootBackground() }
        applyMode()
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); requestApplyInsets(); if (Diag.enabled) post(keymapTick) }
    override fun onDetachedFromWindow() { removeCallbacks(keymapTick); super.onDetachedFromWindow() }
    /** Debug builds only: the key map again every half second while the keyboard is attached, when it changed (the window may still be sliding or moving after the first layout, and the emulator tools tap where the LAST line says). */
    private val keymapTick = object : Runnable {
        override fun run() {
            if (!Diag.enabled || !isAttachedToWindow) return
            keys.keymapLine()?.let { if (it != lastKeymapLog) { lastKeymapLog = it; Diag.d(it) } }
            postDelayed(this, 500)
        }
    }

    // ------------------------------------------------------------------ the person's settings

    /** The keyboard opened (or the settings changed): take the options, the floating geometry and, when it floated last time, float again. [safe]: crash-loop safe mode, always docked. */
    fun applySettings(st: Settings, safe: Boolean = false) {
        settings = st
        keys.applyOptions(st)
        grip.accent = st.accentColor
        posX = st.floatX / 1000f; posY = st.floatY / 1000f; wDp = st.floatWDp.toFloat(); hMul = FloatingMath.clampHeightMul(st.floatHMul)
        floatEnabled = st.floatEnabled && !safe
        dockFrac = if (safe) 1f else st.dockFrac / 1000f; dockX = st.dockX / 1000f
        dockHMul = if (safe) 1f else st.dockH / 1000f
        if (dockResizing) { dockResizing = false; clearPreview(); host?.windowTall(false) }       // (a drag that was cut off by the keyboard closing)
        val want = floatEnabled && st.floating && !previewOnly
        if (want != floating) setFloating(want, save = false) else applyMode()
    }

    private fun save() { settings?.saveFloat(floating, Math.round(posX * 1000f), Math.round(posY * 1000f), Math.round(wDp), hMul) }

    // ------------------------------------------------------------------ geometry

    private fun screenW() = resources.displayMetrics.widthPixels
    private fun screenH() = resources.displayMetrics.heightPixels
    private fun frame() = if (floating) (FRAME_DP * d).toInt() else 0

    /** The height the panel really has at [pw] pixels wide (measured, not estimated: an estimate that is a pixel off makes the panel creep). */
    private fun panelHeightAt(pw: Int): Int {
        panel.measure(MeasureSpec.makeMeasureSpec(pw, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        return panel.measuredHeight
    }
    private fun panelWidthPx(): Int = if (!floating) (if (dockFrac >= 0.995f) ViewGroup.LayoutParams.MATCH_PARENT else Math.round(dockFrac * screenW())) else Math.round(FloatingMath.clampWidthDp(if (wDp > 0f) wDp else FloatingMath.defaultWidthDp(screenW() / d), screenW() / d) * d)

    /** Another app asked for it (the Kestrel browser's navigation panel opens the keyboard floating over the page): float, or dock. Does nothing when floating is switched off in the settings or in the settings preview. */
    fun requestFloating(on: Boolean) { if (floatEnabled && !previewOnly) setFloating(on) }

    private fun setFloating(on: Boolean, save: Boolean = true) {
        if (floating == on || (on && previewOnly)) return
        Diag.d("floating -> $on (x=$posX y=$posY wDp=$wDp hMul=$hMul)")
        floating = on
        applyMode()
        if (save) save()
        host?.modeChanged(on)
    }

    /**
     * What the panel paints behind the bar row and the keys: the keyboard's own colour, docked and floating alike. (Docked it used to be nothing, so the bar row showed the keyboard WINDOW's background, which is white
     * on a phone in day mode: a white strip above dark keys once the colours no longer follow the phone.)
     */
    private fun panelBackground(): android.graphics.drawable.Drawable =
        if (floating) GradientDrawable().apply { setColor(keys.backgroundColor); cornerRadius = 16 * d; setStroke((2 * d).toInt(), grip.accent) }
        else android.graphics.drawable.ColorDrawable(keys.backgroundColor)

    /**
     * The window behind the panel. Docked and squeezed, the keyboard window is still as wide as the screen: the strips beside the narrower panel are in the keyboard's colour too (they showed the keyboard WINDOW's own
     * background, white on a phone in day mode). Floating, the window is the whole screen and must stay transparent: the app shows around the panel.
     */
    private fun rootBackground(): android.graphics.drawable.Drawable? = if (windowTall) null else android.graphics.drawable.ColorDrawable(keys.backgroundColor)

    /** Docked: full width at the bottom. Floating: the chosen width, the chosen row height, at the saved position, in a neon frame. */
    private fun applyMode() {
        clearPreview()
        grip.floating = floating
        keys.floatMul = if (floating) hMul else dockHMul
        val f = frame()
        panel.setPadding(f + (if (floating) 0 else navLeft), f / 2, f + (if (floating) 0 else navRight), f + (if (floating) 0 else navBottom))
        panel.background = panelBackground()
        background = rootBackground()
        panel.elevation = if (floating) 12 * d else 0f
        minimumHeight = if (windowTall) screenH() else 0
        requestLayout()
    }

    /** The window became as tall as the screen or went back to the keyboard's height (a docked height drag): the room above the keyboard shows the app, so the window paints nothing there. */
    private fun syncWindowState() {
        minimumHeight = if (windowTall) screenH() else 0
        background = rootBackground()
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // The overlay and the clipboard are measured like the keys (their size IS the keys' size) but are siblings of the keys: a change of the keys' height flags the keys and their parents, never these two, so a SHORTER
        // keyboard kept the old taller measure of the overlay and the panel stayed tall (found by the Full size pill test, R19c: a docked keyboard dragged shorter). They are measured again whenever the root is.
        overlay.forceLayout(); clipboard.forceLayout()
        val lp = panel.layoutParams as LayoutParams
        lp.width = panelWidthPx()
        if (floating) {
            lp.gravity = Gravity.TOP or Gravity.LEFT
            val pw = lp.width
            val ph = panelHeightAt(pw)
            val w = MeasureSpec.getSize(widthMeasureSpec).let { if (it > 0) it else screenW() }
            val h = MeasureSpec.getSize(heightMeasureSpec).let { if (it > 0 && MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED) it else screenH() }
            lp.leftMargin = FloatingMath.posF(posX, w - pw); lp.topMargin = FloatingMath.posF(posY, h - navBottom - ph)
        } else if (dockFrac < 0.995f) {
            val w = MeasureSpec.getSize(widthMeasureSpec).let { if (it > 0) it else screenW() }
            lp.gravity = Gravity.BOTTOM or Gravity.LEFT; lp.leftMargin = Math.round(dockX * (w - lp.width).coerceAtLeast(0)); lp.topMargin = 0
        } else { lp.gravity = Gravity.BOTTOM; lp.leftMargin = 0; lp.topMargin = 0 }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /**
     * R38 (owner 2026-11-27: "it still often does not register that I'm glide typing, especially at the edges of the keyboard"): with gesture navigation (the Android 10 tablet's default, and the phone's) a touch that starts within
     * about 30 dp of the left or right edge of the SCREEN is the "back" swipe: Android takes the finger away from the keyboard (the keyboard gets a cancel), so a glide that starts on q, a, p or l, or on the erase key,
     * dies before it began. The keyboard's two side strips are handed back to the keyboard (API 29 `setSystemGestureExclusionRects`; the system only looks at the part of them that touches a screen edge, and keeps at most 200 dp
     * of height per edge). A keyboard floating or squeezed away from the edges is not touched by this. The "back" swipe still works everywhere above the keyboard.
     */
    private fun keepEdgeSwipeOffTheKeys() {
        val p = panelRect()
        if (p.width() <= 0 || p.height() <= 0) { if (edgeRects.isNotEmpty()) { edgeRects = emptyList(); systemGestureExclusionRects = edgeRects }; return }
        val e = Math.min((EDGE_EXCLUDE_DP * d).toInt(), p.width() / 2)
        val want = listOf(Rect(p.left, p.top, p.left + e, p.bottom), Rect(p.right - e, p.top, p.right, p.bottom))
        if (want != edgeRects) { edgeRects = want; systemGestureExclusionRects = want }
    }
    private var edgeRects: List<Rect> = emptyList()

    private var lastLayoutLog = ""
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        keepEdgeSwipeOffTheKeys()
        val loc = IntArray(2); grip.getLocationOnScreen(loc)
        val msg = "layout root=${r - l}x${b - t} panel=${panelRect().flattenToString()} grip=${grip.width}x${grip.height} at screen ${loc[0]},${loc[1]} keys=${keys.width}x${keys.height} floating=$floating"
        if (msg != lastLayoutLog) { lastLayoutLog = msg; Diag.d(msg) }
        if (Diag.enabled) post { keys.keymapLine()?.let { if (it != lastKeymapLog) { lastKeymapLog = it; Diag.d(it) } } }     // after the layout pass: the key centres the emulator tools tap
    }
    private var lastKeymapLog = ""
    /** Debug builds: logs the key map again right now (the tools call it after a page change); no-op in a release build. */
    fun logKeymapNow() { if (Diag.enabled) keys.keymapLine()?.let { lastKeymapLog = it; Diag.d(it) } }

    /** The panel's rectangle in this view's coordinates. */
    fun panelRect(): Rect = if (previewing) Rect(pvLeft, pvTop, pvLeft + pvW, pvTop + pvH) else Rect(panel.left, panel.top, panel.right, panel.bottom)

    private fun cornerSize() = (CORNER_DP * d).toInt()

    /** What a touch may take while the keyboard floats: the panel and the four corner grab squares; the rest of the window is the app's. */
    fun touchRegion(): Region {
        val r = panelRect(); val z = cornerSize()
        val reg = Region(r)
        if (floating) for (corner in 0..3) reg.op(cornerSquare(corner, r, z), Region.Op.UNION)
        return reg
    }

    private fun cornerSquare(corner: Int, r: Rect, z: Int): Rect {
        val x = if (corner == 0 || corner == 2) r.left else r.right; val y = if (corner == 0 || corner == 1) r.top else r.bottom
        return Rect(x - z, y - z, x + z, y + z)
    }

    /** Which corner grab square is at ([x], [y]) in this view, or -1: the square minus the keys' own area, so a corner key (Backspace, Enter, the globe) is never taken. */
    fun cornerAt(x: Float, y: Float): Int {
        if (!floating) return -1
        val r = panelRect(); val z = cornerSize(); val f = frame()
        val inner = Rect(r.left + f, r.top + f, r.right - f, r.bottom - f)
        if (inner.contains(x.toInt(), y.toInt())) return -1
        for (corner in 0..3) if (cornerSquare(corner, r, z).contains(x.toInt(), y.toInt())) return corner
        return -1
    }

    // ------------------------------------------------------------------ the bar's gestures

    override fun gripDetach(rawX: Float, rawY: Float) {
        // the panel leaves the bottom and stands with its bar under the finger; the window becomes the whole screen first, so the position is worked out in screen terms
        val w = screenW(); val h = screenH() - navBottom
        val pw = Math.round(FloatingMath.clampWidthDp(if (wDp > 0f) wDp else FloatingMath.defaultWidthDp(w / d), w / d) * d)
        keys.floatMul = hMul
        floating = true; applyMode(); floating = false           // (the frame and the row height count in the measure below)
        val ph = panelHeightAt(pw)
        val left = FloatingMath.clampPos((rawX - pw / 2f).toInt(), pw, w); val top = FloatingMath.clampPos((rawY - GripBar.HEIGHT_DP * d / 2f - FRAME_DP * d).toInt(), ph, h)
        posX = FloatingMath.fracF(left, w - pw); posY = FloatingMath.fracF(top, h - ph)
        travelled = 0f
        setFloating(true)
    }

    override fun gripDrag(dx: Float, dy: Float) {
        if (!floating) return
        val w = if (width > 0) width else screenW(); val h = (if (height > 0) height else screenH()) - navBottom
        if (dx == 0f && dy == 0f) return                          // a finger held still moves nothing
        val r = panelRect(); val pw = r.width().coerceAtLeast(1); val ph = r.height().coerceAtLeast(1)
        val left = FloatingMath.clampPos(r.left + Math.round(dx), pw, w); val top = FloatingMath.clampPos(r.top + Math.round(dy), ph, h)
        travelled += Math.abs(dx) + Math.abs(dy)
        posX = FloatingMath.fracF(left, w - pw); posY = FloatingMath.fracF(top, h - ph)
        requestLayout()
    }

    override fun gripDrop() {
        val r = panelRect()
        if (travelled > 48 * d && FloatingMath.dropsToDock(r.bottom, (if (height > 0) height else screenH()) - navBottom, d)) { gripDock(); return }
        save(); host?.geometryChanged()
    }

    override fun gripDock() { setFloating(false); host?.geometryChanged() }

    // ---- squeeze, resize and slide of the docked keyboard (M8, R19c)
    private fun applyDock(left: Int, w: Int, h: Int = curHeight()) {
        val sw = screenW()
        val ww = w.coerceIn(Math.round(MIN_DOCK_FRAC * sw), sw)
        val l = left.coerceIn(0, sw - ww)
        setPreview(l, panel.bottom - h, ww, h)                           // only the picture moves; the layout waits for the finger to lift (gripSqueezeEnd)
    }
    private fun curLeft(): Int = if (previewing) pvLeft else if (dockFrac >= 0.995f) 0 else Math.round(dockX * (screenW() - Math.round(dockFrac * screenW())))
    private fun curWidth(): Int = if (previewing) pvW else if (dockFrac >= 0.995f) screenW() else Math.round(dockFrac * screenW())
    private fun curHeight(): Int = if (previewing) pvH else panel.height
    override fun gripSqueezeStart(side: Int) { sqStartW = curWidth(); sqStartLeft = curLeft(); sqStartH = curHeight() }

    /** The docked keyboard grows upwards from the bottom edge: the window must be the whole screen for it (see [Host.windowTall]). */
    private fun beginDockResize() {
        if (dockResizing || floating || previewOnly) return
        dockResizing = true; coveredH = panel.height
        Diag.d("docked height drag: the window is the whole screen (covered=$coveredH)")
        syncWindowState()
        host?.windowTall(true)
    }

    /**
     * Owner R19c: an end arrow of the bar drags like a top corner of the floating keyboard: sideways = narrower / wider (the other edge stays), UP = taller, down = shorter, the bottom edge stays on the bottom of the screen.
     * Only a picture moves meanwhile ([setPreview]). A little drift up or down while squeezing sideways is ignored ([FloatingMath.softDead]).
     */
    override fun gripSqueeze(side: Int, dx: Float, dy: Float) {
        val sw = screenW()
        val vy = if (previewOnly) 0f else FloatingMath.softDead(dy, VERT_DEAD_DP * d)
        if (!dockResizing && vy != 0f) beginDockResize()
        val minW = Math.round(MIN_DOCK_FRAC * sw)
        val guessW = (if (side == 0) sqStartW - Math.round(dx) else sqStartW + Math.round(dx)).coerceIn(minW, sw)
        val keysW = guessW - navLeft - navRight; val gripH = gripHeight()
        val minH = gripH + keys.heightFor(keysW, FloatingMath.DOCK_MIN_H_MUL) + navBottom
        val maxByMul = gripH + keys.heightFor(keysW, FloatingMath.DOCK_MAX_H_MUL) + navBottom
        val maxH = maxOf(sqStartH, minOf(maxByMul, Math.round(FloatingMath.DOCK_MAX_SCREEN_FRAC * screenH())))
        val r = FloatingMath.dockedResize(side, sqStartLeft, sqStartW, sqStartH, Math.round(dx), if (dockResizing) Math.round(vy) else 0, sw, minW, minOf(minH, sqStartH), maxH)
        setPreview(r.left, panel.bottom - r.height, r.width, r.height)
    }
    private fun gripHeight(): Int = if (grip.height > 0) grip.height else Math.round(GripBar.HEIGHT_DP * d)
    override fun gripPinchStart() { sqStartW = curWidth(); sqStartLeft = curLeft(); sqStartH = curHeight() }
    override fun gripPinch(scale: Float) {
        val w = Math.round(sqStartW * scale).coerceIn(Math.round(MIN_DOCK_FRAC * screenW()), screenW())
        applyDock(sqStartLeft + (sqStartW - w) / 2, w, sqStartH)        // pinching keeps the middle where it was (and the height)
    }
    override fun gripSlide(dx: Float) { applyDock(curLeft() + Math.round(dx), curWidth()) }
    /** The finger lifted: the size the picture shows is laid out for real (once), kept, and the glide decoder is told where the keys are now. */
    override fun gripSqueezeEnd() {
        if (previewing) {
            val sw = screenW()
            dockFrac = pvW / sw.toFloat(); dockX = if (sw - pvW <= 0) 0.5f else pvLeft / (sw - pvW).toFloat()
            if (dockResizing) dockHMul = FloatingMath.clampDockMul(keys.mulFor(pvW - navLeft - navRight, pvH - gripHeight() - navBottom))     // (the exact inverse: the keys come out as tall as the picture was)
            keys.floatMul = dockHMul
            clearPreview(); requestLayout(); grip.invalidate()
        }
        if (dockResizing) { dockResizing = false; syncWindowState(); host?.windowTall(false) }
        settings?.saveDock(Math.round(dockFrac * 1000f), Math.round(dockX * 1000f), Math.round(dockHMul * 1000f)); host?.geometryChanged()
    }
    override fun gripFullWidth() { clearPreview(); dockFrac = 1f; dockX = 0.5f; dockHMul = 1f; if (!floating) keys.floatMul = 1f; requestLayout(); grip.invalidate(); gripSqueezeEnd() }
    override fun gripSqueezed(): Boolean = squeezed
    override fun gripCanRestore(): Boolean = floating || squeezed || Math.abs(dockHMul - 1f) > 0.005f
    /** The "Full size" pill: a squeezed (or resized) keyboard goes back to the full width and the normal height; a floating one docks again, full size (owner R18c, R19c). */
    override fun gripRestore() {
        if (floating) { dockFrac = 1f; dockX = 0.5f; dockHMul = 1f; settings?.saveDock(1000, 500, 1000); setFloating(false); host?.geometryChanged() }
        else gripFullWidth()
    }

    // ------------------------------------------------------------------ the corners

    /**
     * R38: a squeezed docked keyboard stands in a window as wide as the screen; the strips beside it belonged to nobody, so a thumb that began a glide a few millimetres outside the q or p key (the edge keys are where the
     * thumb overshoots most) did nothing at all. A touch that starts in the margin right beside the keys is now the keys' own: it is handed to the keys view, which takes the nearest key, and the whole touch (moves and lift) follows.
     */
    private var marginTouch = false; private var marginDx = 0f; private var marginDy = 0f
    private fun inKeyMargin(x: Float, y: Float): Boolean {
        if (floating || !squeezed || previewOnly || previewing || resizeCorner >= 0 || clipboard.isOpen || keys.width <= 0) return false
        val p = panelRect(); if (y < p.top + grip.height || y >= p.bottom) return false
        val m = EDGE_MARGIN_DP * d
        return (p.left > 0 && x >= p.left - m && x < p.left) || (p.right < width && x >= p.right && x < p.right + m)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            marginTouch = false
            val c = cornerAt(ev.x, ev.y)
            if (c >= 0) { startResize(c, ev); return true }
            if (inKeyMargin(ev.x, ev.y)) { marginTouch = true; marginDx = (panel.left + pane.left + keys.left).toFloat(); marginDy = (panel.top + pane.top + keys.top).toFloat() }
        }
        if (marginTouch) {
            val m = MotionEvent.obtain(ev); m.offsetLocation(-marginDx, -marginDy)
            keys.dispatchTouchEvent(m); m.recycle()
            if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) marginTouch = false
            return true
        }
        if (resizeCorner >= 0) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> doResize(ev)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> endResize()
            }
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun startResize(corner: Int, ev: MotionEvent) {
        val r = panelRect()
        resizeCorner = corner; rsLeft = r.left; rsTop = r.top; rsW = r.width(); rsH = r.height(); rsX = ev.rawX; rsY = ev.rawY
        panel.elevation = 0f                                      // (the shadow is drawn again at every size: nothing the weak tablet GPU needs while a finger drags)
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); invalidate()
    }

    /** One touch event of a corner drag: the rectangle the finger asks for (the opposite corner stays put) is only SHOWN (see [setPreview]); nothing is measured or laid out here. */
    private fun doResize(ev: MotionEvent) {
        val sw = if (width > 0) width else screenW(); val sh = (if (height > 0) height else screenH()) - navBottom
        val f = frame(); val gripH = (GripBar.HEIGHT_DP * d).toInt()
        val minW = Math.round(FloatingMath.clampWidthDp(FloatingMath.MIN_W_DP, sw / d) * d); val maxW = Math.round(FloatingMath.clampWidthDp(FloatingMath.MAX_W_DP, sw / d) * d)
        val minH = gripH + keys.heightFor(minW - 2 * f, FloatingMath.MIN_H_MUL) + f * 3 / 2
        val maxH = gripH + keys.heightFor(maxW - 2 * f, FloatingMath.MAX_H_MUL) + f * 3 / 2
        val r = FloatingMath.resize(resizeCorner, rsLeft, rsTop, rsW, rsH, (ev.rawX - rsX).toInt(), (ev.rawY - rsY).toInt(), minW, maxW, minH, maxH, sw, sh)
        setPreview(r.left, r.top, r.width, r.height)
    }

    /** The finger lifted: the rectangle that was shown becomes the real size (width, row height, position), laid out once; a top corner keeps the bottom edge where it was. */
    private fun endResize() {
        val corner = resizeCorner
        resizeCorner = -1
        if (previewing) {
            val sw = if (width > 0) width else screenW(); val sh = (if (height > 0) height else screenH()) - navBottom
            val f = frame(); val gripH = (GripBar.HEIGHT_DP * d).toInt()
            wDp = pvW / d
            hMul = FloatingMath.clampHeightMul(keys.mulFor(pvW - 2 * f, pvH - gripH - f * 3 / 2))     // (the exact inverse: the keys come out as tall as the picture was)
            keys.floatMul = hMul
            val ph = panelHeightAt(pvW)
            val top = if (corner == 0 || corner == 1) FloatingMath.clampPos(rsTop + rsH - ph, ph, sh) else FloatingMath.clampPos(pvTop, ph, sh)
            posX = FloatingMath.fracF(pvLeft, sw - pvW); posY = FloatingMath.fracF(top, sh - ph)
            clearPreview()
            requestLayout()
        }
        panel.elevation = if (floating) 12 * d else 0f
        save(); host?.geometryChanged(); invalidate()
    }

    override fun dispatchDraw(c: Canvas) {
        super.dispatchDraw(c)
        if (!floating) return
        // four small L marks hug the outside of the corners: the handles
        val r = panelRect(); val l = 14 * d; val o = 3 * d
        mark.strokeWidth = 3 * d; mark.color = if (resizeCorner >= 0) 0xFFFFFFFF.toInt() else grip.accent
        for (corner in 0..3) {
            val x = if (corner == 0 || corner == 2) r.left - o else r.right + o; val y = if (corner == 0 || corner == 1) r.top - o else r.bottom + o
            val sx = if (corner == 0 || corner == 2) 1f else -1f; val sy = if (corner == 0 || corner == 1) 1f else -1f
            markPath.reset(); markPath.moveTo(x + sx * l, y); markPath.lineTo(x, y); markPath.lineTo(x, y + sy * l); c.drawPath(markPath, mark)
        }
    }

    companion object {
        /** The docked keyboard is never narrower than this share of the screen. */
        const val MIN_DOCK_FRAC = 0.5f
        /** A drag of an arrow of the bar changes the height only after the finger has moved this far up or down (dp). */
        const val VERT_DEAD_DP = 10f
        const val FRAME_DP = 8f
        const val CORNER_DP = 24f
        /** R38: how wide the strips at the keyboard's left and right edge are that the system's "back" swipe may not take (it starts within about 30 dp of a screen edge, a bit more with the high sensitivity setting). */
        const val EDGE_EXCLUDE_DP = 56f
        /** R38: how far beside a squeezed keyboard a touch still counts as the keys' own (see [inKeyMargin]). */
        const val EDGE_MARGIN_DP = 36f
    }
}
