package app.kestrel.core.input.glide

import app.kestrel.core.input.Key
import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.InputController

/**
 * R32c: teaches the [EmailBook] the addresses the person TYPES. Only an address typed key by key counts ([InputController.typedEmail]): never one pasted, filled in by an app or password manager, or taken from the strip
 * (a strip pick only counts the use, see [picked]); nothing in password boxes or where the app asked for no personalised learning. R34b (owner 2026-11-26: "it still isn't recognizing email fields"): this holds in EVERY ordinary box
 * (text, search and e-mail boxes), not only in boxes the app flagged as e-mail ones, because many login pages and apps make an e-mail box a plain text box. An address counts when a space, comma, semicolon or the action key ends it, and
 * when the box is left: the address standing at the end of the text is remembered after every key ([afterKey]) until then, and [finishField] is called whenever the box changes (a new text box, but also a focus that moves to the next box
 * of the same window without the old one ever being "finished": that was the hole through which a typed login address never got in). Single-threaded: the UI thread. Never logs or reports an address.
 */
class EmailLearner(private val controller: InputController, private val book: () -> EmailBook?) {
    /** The address at the end of the box right now, typed by hand, waiting for the box to be left. */
    private var waiting: String? = null

    private fun allowed(): Boolean {
        val f = controller.field
        return f.learningAllowed && (f.kind == FieldKind.EMAIL || f.kind == FieldKind.TEXT || f.kind == FieldKind.SEARCH)
    }

    /** Call BEFORE a key reaches the controller. */
    fun beforeKey(key: Key) {
        val b = book() ?: return
        if (!allowed()) return
        val ends = key == Key.Go || (key is Key.Ch && key.text.length == 1 && (key.text[0].isWhitespace() || key.text[0] == ',' || key.text[0] == ';'))
        if (!ends) return
        val a = (if (controller.atSignNear()) controller.typedEmail() else null) ?: waiting
        if (a != null) b.learn(a)
        waiting = null
    }

    /** Call AFTER the controller handled the key: the address typed so far is remembered until the box is left. */
    fun afterKey() {
        if (!allowed() || !controller.atSignNear()) { waiting = null; return }
        waiting = controller.typedEmail()
    }

    /** The box was left (focus lost, or another box took the keyboard): an address typed by hand is learned now. */
    fun finishField() {
        val b = book(); val a = waiting; waiting = null
        if (b != null && a != null && allowed()) b.learn(a)
    }

    /** A new box: nothing waits. */
    fun reset() { waiting = null }

    /** An address was picked from the strip: it counts one more use (it is already in the book). */
    fun picked(address: String) { if (allowed()) book()?.learn(address) }
}
