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
    /**
     * The globe (switch keyboard). Owner, 2026-11-17: "remove the bottom left button to change keyboards": it is OFF by default now (Settings > Layout brings it back, and the crash-loop safe mode always shows it, plan 12 KB-106);
     * without it, HOLDING the space bar opens the keyboard picker. Its label is only a name; the view draws a globe.
     */
    const val GLOBE = "globe"
    private val globe = KeySpec(GLOBE, Key.NextKeyboard, 1f)
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

    /** The number row is a little thinner than the letter rows (it is only ever tapped, never glided over). */
    const val NUMBER_ROW_WEIGHT = 0.72f

    /** Row heights for [MiniKeyboardGeometry.boxes]: pass the SAME list to the view and to the decoder. */
    fun rowWeights(rows: List<List<KeySpec>>): List<Float> = if (hasNumberRow(rows)) List(rows.size) { if (it == 0) NUMBER_ROW_WEIGHT else 1f } else List(rows.size) { 1f }
    fun hasNumberRow(rows: List<List<KeySpec>>): Boolean = rows.size == 5

    /**
     * The symbols that used to live behind the "123" key (owner: "Remove the 123 button because there are always numbers"): they open as a grid when the full stop or the comma is HELD (see KeysView), laid out
     * in the rows people know. Every character of the old symbols page is here.
     */
    val symbolRows: List<List<String>> = listOf("@#$%&-+()/", "*\"':;!?_=~", "<>[]{}\\|").map { r -> r.map { it.toString() } }

    /** Width of the space bar: as much of the row as the other keys leave (owner: "make the space bar wider. As wide as can fit"). */
    const val ROW_UNITS = 10f

    fun rows(page: Page, shift: Boolean, kind: FieldKind, actionLabel: String = "⏎", numberRow: Boolean = true, showGlobe: Boolean = false): List<List<KeySpec>> {
        if (kind == FieldKind.NUMBER || kind == FieldKind.PHONE) return keypad(kind, actionLabel, showGlobe)
        val digits = if (numberRow) listOf(chars("1234567890")) else emptyList()
        fun letters(s: String) = chars(if (shift) s.uppercase() else s)
        // the bottom row: [globe if asked] [one or two small keys] [space: the rest] [the full stop and the action key]
        fun bottom(vararg fixed: KeySpec): List<KeySpec> {
            val g = if (showGlobe) listOf(globe) else emptyList()
            val used = (g + fixed).sumOf { it.weight.toDouble() }.toFloat()
            val space = KeySpec("space", Key.Ch(" "), ROW_UNITS - used)
            val head = fixed.takeWhile { it.label != "." && it.label != ".com" && it.key !== Key.Go }
            val tail = fixed.drop(head.size)
            return g + head + space + tail
        }
        val bottomRow = when (kind) {
            FieldKind.URL -> bottom(ch("/"), ch("."), ch(".com", 1.5f), action(actionLabel, 1.6f))
            FieldKind.EMAIL -> bottom(ch("@"), ch("."), ch(".com", 1.5f), action(actionLabel, 1.6f))
            else -> bottom(ch(","), ch("."), action(actionLabel, 1.6f))
        }
        return digits + listOf(
            letters("qwertyuiop"),
            listOf(KeySpec("", null, 0.5f)) + letters("asdfghjkl") + KeySpec("", null, 0.5f),
            listOf(KeySpec("⇧", Key.Shift, 1.5f)) + letters("zxcvbnm") + back,
            bottomRow,
        )
    }

    private fun keypad(kind: FieldKind, actionLabel: String, showGlobe: Boolean): List<List<KeySpec>> {
        val phone = kind == FieldKind.PHONE
        val g = if (showGlobe) listOf(KeySpec(GLOBE, Key.NextKeyboard, 1f)) else emptyList()
        return listOf(
            listOf(ch("1"), ch("2"), ch("3"), KeySpec("⌫", Key.Backspace, 1f)),
            listOf(ch("4"), ch("5"), ch("6"), ch(if (phone) "+" else "-")),
            listOf(ch("7"), ch("8"), ch("9"), ch(if (phone) "#" else ".")),
            if (showGlobe) g + listOf(ch("0"), if (phone) ch(",") else KeySpec("space", Key.Ch(" "), 1f), action(actionLabel, 1f))
            else listOf(ch("0", if (phone) 1.5f else 2f), if (phone) ch(",", 1f) else KeySpec("space", Key.Ch(" "), 1f), action(actionLabel, if (phone) 1.5f else 1f)),
        )
    }
}
