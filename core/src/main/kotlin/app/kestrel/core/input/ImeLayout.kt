package app.kestrel.core.input

import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.ImeActions

enum class Page { LETTERS, SYMBOLS }

/**
 * The keys of the SYSTEM keyboard (the browser's MiniKeyboardLayout is for an address bar and stays as the engine's reference layout). Rows 1-3 of the
 * letters page are the browser's own, so the glide decoder sees the same geometry; only the bottom row changes with the field (text, URL, e-mail) and
 * number / phone fields get a keypad. The action key ([Key.Go]) is labelled for what Enter does in the field (KB-104 decides which action).
 */
object ImeLayout {
    private fun ch(s: String, w: Float = 1f) = KeySpec(s, Key.Ch(s), w)
    private fun chars(s: String): List<KeySpec> = s.map { ch(it.toString()) }
    private val back = KeySpec("⌫", Key.Backspace, 1.5f)
    private fun action(label: String, w: Float) = KeySpec(label, Key.Go, w)

    /** The label of the action key: what Enter does in the field right now. */
    fun actionLabel(imeAction: Int, noEnterAction: Boolean): String {
        if (noEnterAction) return "⏎"
        return when (imeAction) {
            ImeActions.GO -> "Go"
            ImeActions.SEARCH -> "Search"
            ImeActions.SEND -> "Send"
            ImeActions.NEXT -> "Next"
            ImeActions.PREVIOUS -> "Prev"
            ImeActions.DONE -> "Done"
            else -> "⏎"
        }
    }

    fun rows(page: Page, shift: Boolean, kind: FieldKind, actionLabel: String = "⏎"): List<List<KeySpec>> {
        if (kind == FieldKind.NUMBER || kind == FieldKind.PHONE) return keypad(kind, actionLabel)
        if (page == Page.SYMBOLS) return listOf(
            chars("1234567890"),
            chars("@#$%&-+()/"),
            chars("_*\"':;!?") + back.let { KeySpec(it.label, it.key, 2f) },
            listOf(KeySpec("abc", Key.ToLetters, 1.5f), ch(","), KeySpec("space", Key.Ch(" "), 4.5f), ch("."), action(actionLabel, 2f)),
        )
        fun letters(s: String) = chars(if (shift) s.uppercase() else s)
        val bottom = when (kind) {
            FieldKind.URL -> listOf(KeySpec("123", Key.ToSymbols, 1.5f), ch("/"), KeySpec("space", Key.Ch(" "), 3f), ch("."), ch(".com", 1.5f), action(actionLabel, 2f))
            FieldKind.EMAIL -> listOf(KeySpec("123", Key.ToSymbols, 1.5f), ch("@"), KeySpec("space", Key.Ch(" "), 3.5f), ch("."), ch(".com", 1.5f), action(actionLabel, 1.5f))
            else -> listOf(KeySpec("123", Key.ToSymbols, 1.5f), ch(","), KeySpec("space", Key.Ch(" "), 4.5f), ch("."), action(actionLabel, 2f))
        }
        return listOf(
            letters("qwertyuiop"),
            listOf(KeySpec("", null, 0.5f)) + letters("asdfghjkl") + KeySpec("", null, 0.5f),
            listOf(KeySpec("⇧", Key.Shift, 1.5f)) + letters("zxcvbnm") + back,
            bottom,
        )
    }

    private fun keypad(kind: FieldKind, actionLabel: String): List<List<KeySpec>> {
        val phone = kind == FieldKind.PHONE
        return listOf(
            listOf(ch("1"), ch("2"), ch("3"), KeySpec("⌫", Key.Backspace, 1f)),
            listOf(ch("4"), ch("5"), ch("6"), ch(if (phone) "+" else "-")),
            listOf(ch("7"), ch("8"), ch("9"), ch(if (phone) "#" else ".")),
            listOf(ch(if (phone) "*" else ","), ch("0"), if (phone) ch(",") else KeySpec("space", Key.Ch(" "), 1f), action(actionLabel, 1f)),
        )
    }
}
