package app.kestrel.core.input.glide

import app.kestrel.core.input.Key
import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.InputController

/**
 * R32c: teaches the [EmailBook] the addresses the person TYPES. Only an address typed key by key counts ([InputController.typedEmail]): never one pasted, filled in by an app or password manager, or taken from the strip
 * (a strip pick only counts the use, see [picked]); nothing in password boxes or where the app asked for no personalised learning. In a normal text box an address counts when a space, comma, semicolon or Enter ends it; in an
 * e-mail box it counts when the box is left or its action key is pressed (the address is written there as one piece). Single-threaded: the UI thread. Never logs or reports an address.
 */
class EmailLearner(private val controller: InputController, private val book: () -> EmailBook?) {
    /** The address in the e-mail box right now, typed by hand, waiting for the box to be left. */
    private var waiting: String? = null

    private fun allowed(): Boolean { val f = controller.field; return f.learningAllowed && f.kind != FieldKind.PASSWORD }

    /** Call BEFORE a key reaches the controller. */
    fun beforeKey(key: Key) {
        val b = book() ?: return
        if (!allowed()) return
        val ends = key == Key.Go || (key is Key.Ch && key.text.length == 1 && (key.text[0].isWhitespace() || key.text[0] == ',' || key.text[0] == ';'))
        if (!ends) return
        val a = controller.typedEmail() ?: if (controller.field.kind == FieldKind.EMAIL) waiting else null
        if (a != null) b.learn(a)
        waiting = null
    }

    /** Call AFTER the controller handled the key: in an e-mail box the address typed so far is remembered until the box is left. */
    fun afterKey() {
        if (controller.field.kind != FieldKind.EMAIL || !allowed()) { waiting = null; return }
        waiting = controller.typedEmail()
    }

    /** The box was left (focus lost): an address typed by hand in an e-mail box is learned now. */
    fun finishField() {
        val b = book(); val a = waiting; waiting = null
        if (b != null && a != null && allowed()) b.learn(a)
    }

    /** A new box: nothing waits. */
    fun reset() { waiting = null }

    /** An address was picked from the strip: it counts one more use (it is already in the book). */
    fun picked(address: String) { if (allowed()) book()?.learn(address) }
}
