package app.swoop.keyboard

import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import app.kestrel.core.input.editor.EditorBridge

/**
 * [EditorBridge] on the app's [InputConnection]. The connection can be null (no field focused), can die at any moment, and web views implement
 * only part of it: every call is wrapped, a failure is reported as false/null and the controller (which assumes nothing) resynchronises.
 * No text is ever logged here.
 */
class InputConnectionBridge(private val connection: () -> InputConnection?) : EditorBridge {
    private inline fun <T> with(default: T, f: (InputConnection) -> T): T {
        val c = connection() ?: return default
        return try { f(c) } catch (_: Exception) { default }
    }

    override fun textBefore(max: Int): String? = with(null) { it.getTextBeforeCursor(max, 0)?.toString() }
    override fun textAfter(max: Int): String? = with(null) { it.getTextAfterCursor(max, 0)?.toString() }
    override fun commitText(text: String): Boolean = with(false) { it.commitText(text, 1) }
    override fun setComposingText(text: String): Boolean = with(false) { it.setComposingText(text, 1) }
    override fun finishComposing(): Boolean = with(false) { it.finishComposingText() }
    override fun deleteSurrounding(before: Int, after: Int): Boolean = with(false) { it.deleteSurroundingText(before, after) }
    override fun setSelection(start: Int, end: Int): Boolean = with(false) { it.setSelection(start, end) }
    override fun performAction(action: Int): Boolean = with(false) { it.performEditorAction(action) }
    override fun beginBatch(): Boolean = with(false) { it.beginBatchEdit() }
    override fun endBatch(): Boolean = with(false) { it.endBatchEdit() }

    /** A key press as a soft keyboard sends it (the flags tell web pages and apps that no hardware key was involved). */
    override fun sendKey(code: Int): Boolean = with(false) { c ->
        val now = SystemClock.uptimeMillis()
        val flags = KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
        val down = c.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags))
        val up = c.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags))
        down && up
    }
}
