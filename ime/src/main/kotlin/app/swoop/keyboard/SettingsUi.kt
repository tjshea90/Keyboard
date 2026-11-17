package app.swoop.keyboard

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The look of the settings screen, built in code (no XML, no AndroidX): colours that follow the phone's light / dark setting and the person's accent colour, and the few
 * building blocks the screen is made of (a card, a switch row, a segmented choice, colour dots, an action row). One place, so every row looks and behaves the same.
 */
class Ui(val ctx: Context, val accent: Int, themeMode: Int = 0) {
    val d = ctx.resources.displayMetrics.density
    val dark = when (themeMode) { 1 -> true; 2 -> false; else -> (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES }
    val bg = if (dark) 0xFF0A0E14.toInt() else 0xFFF1F4F8.toInt()
    val card = if (dark) 0xFF131A23.toInt() else 0xFFFFFFFF.toInt()
    val stroke = if (dark) 0xFF222C38.toInt() else 0xFFDDE3EA.toInt()
    val text = if (dark) 0xFFE8EDF3.toInt() else 0xFF14202B.toInt()
    val muted = if (dark) 0xFF8A98A8.toInt() else 0xFF5B6978.toInt()
    val off = if (dark) 0xFF334050.toInt() else 0xFFC3CCD6.toInt()
    val chipBg = if (dark) 0xFF0D131A.toInt() else 0xFFE8EDF2.toInt()
    val onAccent = 0xFFFFFFFF.toInt()
    val danger = 0xFFFF5C7A.toInt()
    private val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    fun dp(v: Float) = (v * d + 0.5f).toInt()

    fun shape(color: Int, radiusDp: Float, strokeColor: Int = 0, strokeDp: Float = 0f) = GradientDrawable().apply {
        setColor(color); cornerRadius = radiusDp * d; if (strokeDp > 0f) setStroke(maxOf(1, (strokeDp * d).toInt()), strokeColor)
    }

    /** A soft press ripple in the accent colour over [base]. */
    fun ripple(base: android.graphics.drawable.Drawable?, radiusDp: Float = 0f): RippleDrawable {
        val mask = if (radiusDp > 0f) shape(Color.WHITE, radiusDp) else null
        return RippleDrawable(ColorStateList.valueOf((accent and 0x00FFFFFF) or 0x33000000), base, mask)
    }

    fun label(s: CharSequence, sp: Float, color: Int = text, bold: Boolean = false) = TextView(ctx).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); setTextColor(color); this.text = s
        typeface = if (bold) medium else Typeface.DEFAULT
        includeFontPadding = false
    }

    /** The small spaced capitals that name a group of settings. */
    fun sectionTitle(s: String) = label(s.uppercase(), 12f, accent, true).apply {
        letterSpacing = 0.12f
        setPadding(dp(8f), dp(24f), dp(8f), dp(8f))
        accessibilityHeading()
    }

    private fun View.accessibilityHeading() { isAccessibilityHeading = true }

    /** A rounded card that holds rows; rows are separated by thin lines. */
    fun card(): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = shape(card, 18f, stroke, 1f)
        clipChildren = true
    }

    fun LinearLayout.row(v: View, divider: Boolean = true) {
        if (divider && childCount > 0) addView(View(ctx).apply { setBackgroundColor(stroke) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1).apply { leftMargin = dp(16f); rightMargin = dp(16f) })
        addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun textColumn(title: String, summary: String?): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        addView(label(title, 16f, text, true))
        if (!summary.isNullOrEmpty()) addView(label(summary, 13f, muted).apply { setPadding(0, dp(3f), 0, 0); setLineSpacing(dp(2f).toFloat(), 1f) })
    }

    /** A row with a title, a one-line explanation and a switch; tapping anywhere on the row flips it. */
    fun switchRow(title: String, summary: String?, checked: Boolean, onChange: (Boolean) -> Unit): View {
        val toggle = Toggle(ctx, this).also { it.isChecked = checked }
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(60f); setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
            background = ripple(null)
            isClickable = true; isFocusable = true
            addView(textColumn(title, summary), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(12f) })
            addView(toggle, LinearLayout.LayoutParams(dp(48f), dp(30f)))
            setOnClickListener { toggle.isChecked = !toggle.isChecked; onChange(toggle.isChecked) }
            accessibilityDelegate = object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = "android.widget.Switch"; info.isCheckable = true; info.isChecked = toggle.isChecked
                    info.text = title + (if (summary != null) ". $summary" else "")
                }
            }
        }
        toggle.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        return row
    }

    /** A row of two or more choices as one segmented control; [selected] is the index shown lit. */
    fun choiceRow(title: String, summary: String?, options: List<String>, selected: Int, onSelect: (Int) -> Unit): View {
        val chips = ArrayList<TextView>()
        val bar = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; background = shape(chipBg, 12f); setPadding(dp(3f), dp(3f), dp(3f), dp(3f)) }
        fun paint(sel: Int) = chips.forEachIndexed { i, c ->
            c.background = if (i == sel) shape(accent, 10f) else null
            c.setTextColor(if (i == sel) onAccent else muted)
            c.isSelected = i == sel
        }
        options.forEachIndexed { i, name ->
            val c = label(name, 13f, muted, true).apply {
                gravity = Gravity.CENTER; minHeight = dp(38f); maxLines = 1; ellipsize = TextUtils.TruncateAt.END
                setPadding(dp(4f), 0, dp(4f), 0)
                isClickable = true; isFocusable = true; contentDescription = "$title: $name"
                setOnClickListener { paint(i); onSelect(i) }
            }
            chips.add(c); bar.addView(c, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        paint(selected)
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
            addView(textColumn(title, summary))
            addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10f) })
        }
    }

    /** Colour dots for the accent colour. */
    fun accentRow(title: String, summary: String?, colors: List<Accent>, selected: Int, onSelect: (Int) -> Unit): View {
        val dots = ArrayList<View>()
        val line = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        fun paint(sel: Int) = dots.forEachIndexed { i, v ->
            v.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(colors[i].color); if (i == sel) setStroke(dp(3f), text) else setStroke(dp(3f), Color.TRANSPARENT) }
            v.isSelected = i == sel
        }
        colors.forEachIndexed { i, a ->
            val v = View(ctx).apply { isClickable = true; isFocusable = true; contentDescription = "${a.name} accent"; setOnClickListener { paint(i); onSelect(i) } }
            dots.add(v); line.addView(v, LinearLayout.LayoutParams(dp(40f), dp(40f)).apply { rightMargin = dp(8f) })
        }
        paint(selected)
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
            addView(textColumn(title, summary))
            addView(line, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10f) })
        }
    }

    /** A row that does something when tapped (export, import, forget, open a screen). [danger] colours the title. */
    fun actionRow(title: String, summary: String?, danger: Boolean = false, onClick: () -> Unit): View = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(56f); setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
        background = ripple(null)
        isClickable = true; isFocusable = true
        addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(title, 16f, if (danger) this@Ui.danger else text, true))
            if (!summary.isNullOrEmpty()) addView(label(summary, 13f, muted).apply { setPadding(0, dp(3f), 0, 0); setLineSpacing(dp(2f).toFloat(), 1f) })
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(12f) })
        addView(label("›", 22f, muted))
        setOnClickListener { onClick() }
        contentDescription = title + (if (summary != null) ". $summary" else "")
    }

    /** A plain line of explanation inside a card. */
    fun note(s: CharSequence): View = label(s, 13f, muted).apply { setPadding(dp(16f), dp(12f), dp(16f), dp(12f)); setLineSpacing(dp(2f).toFloat(), 1f) }

    /** A filled, rounded button in the accent colour (or an outlined one). */
    fun button(s: String, filled: Boolean = true, onClick: () -> Unit): TextView = label(s, 16f, if (filled) onAccent else accent, true).apply {
        gravity = Gravity.CENTER; minHeight = dp(52f); setPadding(dp(20f), 0, dp(20f), 0)
        background = ripple(if (filled) shape(accent, 14f) else shape(Color.TRANSPARENT, 14f, accent, 1.5f), 14f)
        isClickable = true; isFocusable = true
        setOnClickListener { onClick() }
    }
}

/** The switch: a pill with a round thumb that slides across (drawn by hand, so it matches the rest and needs no theme). */
class Toggle(context: Context, private val ui: Ui) : View(context) {
    var isChecked = false
        set(v) { if (field != v) { field = v; animateTo(if (v) 1f else 0f) } else if (!shown) snap() }
    private var t = 0f
    private var shown = false
    private var anim: ValueAnimator? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val box = RectF()

    private fun snap() { shown = true; t = if (isChecked) 1f else 0f; invalidate() }
    private fun animateTo(target: Float) {
        anim?.cancel()
        if (!isAttachedToWindow) { t = target; shown = true; invalidate(); return }
        anim = ValueAnimator.ofFloat(t, target).apply { duration = 140; addUpdateListener { t = it.animatedValue as Float; invalidate() }; start() }
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); if (!shown) snap() }
    override fun onDetachedFromWindow() { anim?.cancel(); super.onDetachedFromWindow() }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val trackH = h * 0.8f; val top = (h - trackH) / 2f
        paint.color = blend(ui.off, ui.accent, t)
        box.set(0f, top, w, top + trackH); c.drawRoundRect(box, trackH / 2f, trackH / 2f, paint)
        val r = trackH / 2f - ui.dp(3f)
        val cx = trackH / 2f + (w - trackH) * t
        paint.color = if (ui.dark) 0xFFF4F7FA.toInt() else 0xFFFFFFFF.toInt()
        c.drawCircle(cx, h / 2f, r, paint)
    }

    private fun blend(a: Int, b: Int, f: Float): Int {
        fun m(x: Int, y: Int) = (x + (y - x) * f).toInt().coerceIn(0, 255)
        return Color.argb(255, m(Color.red(a), Color.red(b)), m(Color.green(a), Color.green(b)), m(Color.blue(a), Color.blue(b)))
    }
}

/** A column that never grows wider than [maxWidthDp] and sits in the middle: one comfortable reading width on a tablet. */
class CenteredColumn(context: Context, private val maxWidthDp: Float = 640f) : LinearLayout(context) {
    init { orientation = VERTICAL }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val cap = (maxWidthDp * resources.displayMetrics.density).toInt()
        val w = MeasureSpec.getSize(widthMeasureSpec)
        super.onMeasure(MeasureSpec.makeMeasureSpec(minOf(w, cap), MeasureSpec.EXACTLY), heightMeasureSpec)
    }
}

internal fun FrameLayout.centered(child: View) { addView(child, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL or Gravity.TOP)) }
