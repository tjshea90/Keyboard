package app.swoop.keyboard

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

/** The transparent layer above the keys: draws what changes while a finger is down ([KeysView.drawOverlay]) and never takes a touch. */
class KeyOverlay(context: Context, private val keys: KeysView) : View(context) {
    init { isClickable = false; isFocusable = false; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    override fun onDraw(c: Canvas) { keys.drawOverlay(c) }
    override fun onTouchEvent(event: MotionEvent): Boolean = false
}

/** What the input method shows: the keys and, exactly on top of them, the overlay layer. */
class KeyboardRoot(context: Context) : FrameLayout(context) {
    val keys = KeysView(context)
    val overlay = KeyOverlay(context, keys)
    init {
        addView(keys, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(overlay, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        keys.overlay = overlay
    }
}
