package app.swoop.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import app.kestrel.core.clipboard.ClipItem
import app.kestrel.core.clipboard.ClipboardStore

/**
 * The clipboard (owner: "build a good clipboard into this app modeled after Gboard clipboard"). It REPLACES the keys while it is open and is exactly as big as they are: a header with the way back to the
 * letters, the title and "Clear all", then the copied things as cards, newest first, pinned ones first. Tap a card to paste it; tap its pin to keep it; press and hold a card for Pin / Delete.
 * Drawn by one view like the keys (no view per card). Everything it shows is the person's own copied text: it is never logged.
 */
class ClipboardPanel(context: Context, private val keys: KeysView) : View(context) {
    interface Host {
        fun clipPaste(text: String)
        fun clipClose()
        /** The list changed (pin, delete, clear): the host saves it. */
        fun clipChanged()
        /** The "turn on" button of the off message. */
        fun clipTurnOn()
    }

    var host: Host? = null
    var store: ClipboardStore? = null
    /** Settings > Clipboard history. Off: the panel only explains how to turn it on. */
    var historyOn = true

    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d
    private val handler = Handler(Looper.getMainLooper())
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.LEFT }
    private val box = RectF()

    private class Card(val item: ClipItem, val layout: StaticLayout, val rect: RectF)
    private var cards: List<Card> = emptyList()
    private var scroll = 0f
    private var contentH = 0f
    private var laidOutFor = ""
    private var menuFor: String? = null            // the card whose Pin / Delete menu is open

    // touch
    private var downX = 0f; private var downY = 0f; private var lastY = 0f
    private var scrolling = false; private var pressedCard = -1; private var pressedPin = false; private var pressedHeader = 0
    private var longFired = false
    private val longPress = Runnable { val i = pressedCard; if (i in cards.indices && !scrolling) { longFired = true; menuFor = cards[i].item.text; performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); invalidate() } }

    init { isClickable = true; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO; visibility = GONE }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) { setMeasuredDimension(keys.measuredWidth, keys.measuredHeight) }

    fun open() { scroll = 0f; menuFor = null; laidOutFor = ""; visibility = VISIBLE; invalidate() }
    fun close() { visibility = GONE; menuFor = null; handler.removeCallbacks(longPress) }
    val isOpen: Boolean get() = visibility == VISIBLE
    /** The list changed behind the panel's back (something was copied while it is open). */
    fun refresh() { laidOutFor = ""; invalidate() }

    // ---- geometry
    private fun headerH() = dp(44f)
    private fun cols() = if (width > dp(640f)) 3 else 2
    private fun backBox() = RectF(dp(6f), dp(5f), dp(70f), headerH() - dp(5f))
    private fun clearBox() = RectF(width - dp(104f), dp(5f), width - dp(6f), headerH() - dp(5f))

    private fun layoutCards() {
        val st = store
        val key = "${width}x${height}/${st?.size}/${st?.items?.joinToString("") { (if (it.pinned) "P" else "N") + it.text.hashCode() }}"
        if (key == laidOutFor) return
        laidOutFor = key
        val items = if (historyOn) st?.items ?: emptyList() else emptyList()
        val gap = dp(6f); val c = cols()
        val cw = (width - gap * (c + 1)) / c
        tp.textSize = dp(14f); tp.typeface = Typeface.DEFAULT
        val list = ArrayList<Card>(items.size)
        var y = headerH() + gap
        var rowTop = y; var rowMax = 0f
        val pending = ArrayList<Pair<ClipItem, StaticLayout>>()
        fun flush() {
            if (pending.isEmpty()) return
            val h = maxOf(dp(56f), rowMax + dp(16f))
            for ((j, p) in pending.withIndex()) list.add(Card(p.first, p.second, RectF(gap + j * (cw + gap), rowTop, gap + j * (cw + gap) + cw, rowTop + h)))
            rowTop += h + gap; pending.clear(); rowMax = 0f
        }
        for (it in items) {
            val sl = StaticLayout.Builder.obtain(it.text, 0, it.text.length, tp, (cw - dp(24f)).toInt().coerceAtLeast(1)).setMaxLines(MAX_LINES).setEllipsize(TextUtils.TruncateAt.END).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
            pending.add(it to sl); rowMax = maxOf(rowMax, sl.height.toFloat())
            if (pending.size == c) flush()
        }
        flush()
        cards = list
        contentH = rowTop
        scroll = scroll.coerceIn(0f, maxOf(0f, contentH - height))
    }

    // ---- drawing
    override fun onDraw(c: Canvas) {
        c.drawColor(keys.backgroundColor)
        layoutCards()
        val r = dp(8f)
        // the cards (clipped below the header)
        c.save(); c.clipRect(0f, headerH(), width.toFloat(), height.toFloat()); c.translate(0f, -scroll)
        for (card in cards) {
            val pinned = card.item.pinned
            fill.color = keys.colKey; c.drawRoundRect(card.rect, r, r, fill)
            if (pinned) { line.strokeWidth = dp(1.4f); line.color = keys.colAccent; c.drawRoundRect(card.rect, r, r, line) }
            c.save(); c.translate(card.rect.left + dp(12f), card.rect.top + dp(8f)); tp.color = keys.colText; card.layout.draw(c); c.restore()
            drawPin(c, pinBox(card), pinned)
            if (menuFor == card.item.text) drawMenu(c, card)
        }
        c.restore()
        if (!historyOn) drawOff(c) else if (cards.isEmpty()) drawEmpty(c)
        drawHeader(c)
    }

    private fun pinBox(card: Card) = RectF(card.rect.right - dp(34f), card.rect.top, card.rect.right, card.rect.top + dp(34f))

    private fun drawPin(c: Canvas, b: RectF, pinned: Boolean) {
        val cx = b.centerX(); val cy = b.centerY(); val s = dp(6f)
        line.strokeWidth = dp(1.8f); line.color = if (pinned) keys.colAccent else keys.colMuted
        fill.color = if (pinned) keys.colAccent else 0
        if (pinned) c.drawCircle(cx, cy - dp(2f), s * 0.8f, fill) else c.drawCircle(cx, cy - dp(2f), s * 0.8f, line)
        c.drawLine(cx, cy + s * 0.5f, cx, cy + s * 1.7f, line)
    }

    private fun drawMenu(c: Canvas, card: Card) {
        val r = dp(8f)
        fill.color = keys.colSpecial; c.drawRoundRect(card.rect, r, r, fill)
        val half = card.rect.width() / 2f
        text(c, if (card.item.pinned) context.getString(R.string.clip_unpin) else context.getString(R.string.clip_pin), card.rect.left + half / 2f, card.rect.centerY(), keys.colText, dp(15f), true)
        text(c, context.getString(R.string.clip_delete), card.rect.left + half * 1.5f, card.rect.centerY(), keys.colAccent, dp(15f), true)
        line.strokeWidth = dp(1f); line.color = keys.colMuted; c.drawLine(card.rect.centerX(), card.rect.top + dp(8f), card.rect.centerX(), card.rect.bottom - dp(8f), line)
    }

    private fun drawHeader(c: Canvas) {
        fill.color = keys.backgroundColor; box.set(0f, 0f, width.toFloat(), headerH()); c.drawRect(box, fill)
        val r = dp(8f)
        val b = backBox(); fill.color = keys.colSpecial; c.drawRoundRect(b, r, r, fill)
        text(c, "abc", b.centerX(), b.centerY(), keys.colText, dp(16f), true)
        text(c, context.getString(R.string.clip_title), width / 2f, headerH() / 2f, keys.colText, dp(16f), true)
        if (historyOn && store?.isEmpty == false) {
            val x = clearBox(); fill.color = keys.colSpecial; c.drawRoundRect(x, r, r, fill)
            text(c, context.getString(R.string.clip_clear), x.centerX(), x.centerY(), keys.colText, dp(14f), false)
        }
    }

    private fun drawEmpty(c: Canvas) {
        wrapped(c, context.getString(R.string.clip_empty), keys.colMuted)
    }

    private fun drawOff(c: Canvas) {
        wrapped(c, context.getString(R.string.clip_off), keys.colMuted)
        val b = turnOnBox(); fill.color = keys.colAccent; c.drawRoundRect(b, dp(10f), dp(10f), fill)
        text(c, context.getString(R.string.clip_turn_on), b.centerX(), b.centerY(), keys.colAccentText, dp(15f), true)
    }

    private fun turnOnBox(): RectF { val w = dp(180f); val cy = headerH() + (height - headerH()) * 0.72f; return RectF((width - w) / 2f, cy - dp(22f), (width + w) / 2f, cy + dp(22f)) }

    private fun wrapped(c: Canvas, msg: String, color: Int) {
        tp.textSize = dp(14f); tp.color = color; tp.typeface = Typeface.DEFAULT
        val w = (width - dp(48f)).toInt().coerceAtLeast(1)
        val sl = StaticLayout.Builder.obtain(msg, 0, msg.length, tp, w).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
        c.save(); c.translate(dp(24f), headerH() + (height - headerH() - sl.height) * 0.4f); sl.draw(c); c.restore()
    }

    private fun text(c: Canvas, s: String, cx: Float, cy: Float, color: Int, size: Float, bold: Boolean) {
        tp.color = color; tp.textSize = size; tp.textAlign = Paint.Align.CENTER; tp.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        val fm = tp.fontMetrics
        c.drawText(s, cx, cy - (fm.ascent + fm.descent) / 2f, tp)
        tp.textAlign = Paint.Align.LEFT; tp.typeface = Typeface.DEFAULT
    }

    // ---- touch
    private fun cardAt(x: Float, y: Float): Int { val yy = y + scroll; if (y < headerH()) return -1; for ((i, k) in cards.withIndex()) if (k.rect.contains(x, yy)) return i; return -1 }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x; downY = e.y; lastY = e.y; scrolling = false; longFired = false; pressedPin = false
                pressedHeader = if (e.y < headerH()) (if (backBox().contains(e.x, e.y)) 1 else if (historyOn && store?.isEmpty == false && clearBox().contains(e.x, e.y)) 2 else 3) else 0
                pressedCard = if (pressedHeader == 0) cardAt(e.x, e.y) else -1
                if (pressedCard >= 0) {
                    val card = cards[pressedCard]
                    if (menuFor == null) {
                        pressedPin = pinBox(card).contains(e.x, e.y + scroll)
                        if (!pressedPin) handler.postDelayed(longPress, LONG_MS)
                    }
                }
                if (pressedHeader == 1 || pressedHeader == 2) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scrolling && Math.abs(e.y - downY) > dp(8f) && pressedHeader == 0) { scrolling = true; handler.removeCallbacks(longPress) }
                if (scrolling) { scroll = (scroll - (e.y - lastY)).coerceIn(0f, maxOf(0f, contentH - height)); invalidate() }
                lastY = e.y
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPress)
                if (scrolling || longFired) { /* a scroll, or the long press opened the menu */ }
                else release(e.x, e.y)
                scrolling = false; pressedCard = -1; pressedHeader = 0
            }
            MotionEvent.ACTION_CANCEL -> { handler.removeCallbacks(longPress); scrolling = false; pressedCard = -1; pressedHeader = 0 }
        }
        return true
    }

    private fun release(x: Float, y: Float) {
        val st = store
        if (pressedHeader == 1 && backBox().contains(x, y)) { host?.clipClose(); return }
        if (pressedHeader == 2 && clearBox().contains(x, y)) { st?.clearUnpinned(); menuFor = null; refresh(); host?.clipChanged(); return }
        if (!historyOn && turnOnBox().contains(x, y)) { host?.clipTurnOn(); return }
        val open = menuFor
        if (open != null) {                                   // the Pin / Delete menu of one card is open
            val card = cards.firstOrNull { it.item.text == open }
            menuFor = null
            if (card != null && card.rect.contains(x, y + scroll)) {
                if (x < card.rect.centerX()) st?.pin(open, !card.item.pinned) else st?.remove(open)
                refresh(); host?.clipChanged()
            } else invalidate()
            return
        }
        val i = cardAt(x, y)
        if (i < 0 || i != pressedCard) return
        val card = cards[i]
        if (pressedPin) { st?.pin(card.item.text, !card.item.pinned); refresh(); host?.clipChanged() }
        else host?.clipPaste(card.item.text)
    }

    override fun onDetachedFromWindow() { handler.removeCallbacks(longPress); super.onDetachedFromWindow() }

    // ---- tests
    fun cardCenterForTest(i: Int): Pair<Float, Float>? { layoutCards(); val k = cards.getOrNull(i) ?: return null; return k.rect.centerX() to (k.rect.centerY() - scroll) }
    fun pinCenterForTest(i: Int): Pair<Float, Float>? { layoutCards(); val k = cards.getOrNull(i) ?: return null; return pinBox(k).let { it.centerX() to (it.centerY() - scroll) } }
    fun backCenterForTest(): Pair<Float, Float> = backBox().let { it.centerX() to it.centerY() }
    fun clearCenterForTest(): Pair<Float, Float> = clearBox().let { it.centerX() to it.centerY() }
    fun turnOnCenterForTest(): Pair<Float, Float> = turnOnBox().let { it.centerX() to it.centerY() }
    fun cardCountForTest(): Int { layoutCards(); return cards.size }

    companion object {
        const val MAX_LINES = 4
        const val LONG_MS = 450L
    }
}
