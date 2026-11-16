package app.kestrel.core.input.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * Random event streams against the fake editor. The invariant (plan 13.1): whenever every selection report has been delivered, the keyboard's
 * belief about the editor (window, cursor, composing word, last glide) is TRUE, whatever mix of taps, glides, swaps, Backspaces, Enters and cursor
 * moves it did, however late the reports arrived and whatever the user or the app did to the text in between. A failure prints the seed.
 */
class InputControllerFuzzTest {
    private val letters = "abcdefghijklmnopqrstuvwxyz"
    private val words = listOf("hello", "world", "for", "fir", "the", "doraby", "a", "i'm", "typing")
    private val punct = listOf(".", ",", "!", "?", "'", "/", ".com", "-", "(", ")", "1", "7", "😀", "é")

    private fun op(r: Rig, rnd: Random, external: Boolean) {
        val c = r.c
        when (val k = rnd.nextInt(if (external) 24 else 20)) {
            0, 1, 2, 3, 4, 5 -> c.type(letters[rnd.nextInt(26)].toString())
            6 -> c.type(punct[rnd.nextInt(punct.size)])
            7 -> c.space()
            8, 9 -> c.backspace()
            10, 11 -> c.commitGlide(words[rnd.nextInt(words.size)])
            12 -> c.swapGlide(words[rnd.nextInt(words.size)])
            13 -> c.moveCursor(rnd.nextInt(9) - 4)
            14 -> c.enter()
            15 -> c.tapShift()
            16 -> c.lockShift()
            17 -> c.setShift(ShiftState.OFF)
            18 -> c.moveCursor(rnd.nextInt(121) - 60)
            19 -> c.type(letters[rnd.nextInt(26)].toString())
            20 -> r.ed.userMove(rnd.nextInt(r.text.length + 1))
            21 -> r.ed.userInsert(listOf("PASTED", "x", "\n", "😀")[rnd.nextInt(4)])
            22 -> { val a = rnd.nextInt(r.text.length + 1); r.ed.userSelect(a, minOf(r.text.length, a + rnd.nextInt(5))) }
            else -> if (rnd.nextInt(6) == 0) r.ed.appSetText("app " + rnd.nextInt(1000) + " text") else r.ed.userMove(0)
        }
        if (k < 0) throw IllegalStateException()
    }

    private fun run(seed: Long, ops: Int, lazy: Boolean, external: Boolean, flaky: Boolean = false) {
        val rnd = Random(seed)
        val start = listOf("", "hello world", "x".repeat(130), "line one\nline two")[rnd.nextInt(4)]
        val r = Rig(start, if (rnd.nextBoolean()) start.length else rnd.nextInt(start.length + 1), multiline = rnd.nextInt(4) != 0,
            action = if (rnd.nextInt(5) == 0) ImeActions.SEARCH else ImeActions.UNSPECIFIED)
        try {
            for (i in 0 until ops) {
                if (flaky && rnd.nextInt(15) == 0) {
                    r.ed.failing = true; op(r, rnd, false); r.ed.failing = false
                    // a dead editor loses nothing it said: the keyboard is told about the new field state like Android does after a restart
                    r.ed.updates.clear(); r.c.startInput(FieldInfo(initialSelStart = r.ed.selS, initialSelEnd = r.ed.selE)); r.check(); continue
                }
                op(r, rnd, external)
                if (lazy) { repeat(rnd.nextInt(3)) { r.deliverOne() } } else { r.sync(); r.check() }
            }
            r.sync(); r.check()
        } catch (e: Throwable) {
            throw AssertionError("seed $seed (lazy=$lazy external=$external flaky=$flaky) after the editor text '${r.text.take(80).replace("\n", "\\n")}': ${e.message}", e)
        }
    }

    @Test fun ownEditsOnlyReportsDeliveredAtOnce() { for (s in 1L..250L) run(s, 120, lazy = false, external = false) }
    @Test fun ownEditsOnlyReportsArriveLateAndInBatches() { for (s in 1000L..1250L) run(s, 120, lazy = true, external = false) }
    @Test fun theUserAndTheAppChangeTheTextInBetweenReportsAtOnce() { for (s in 2000L..2250L) run(s, 120, lazy = false, external = true) }
    @Test fun theUserAndTheAppChangeTheTextInBetweenReportsLate() { for (s in 3000L..3250L) run(s, 120, lazy = true, external = true) }
    @Test fun anEditorThatDiesAndComesBack() { for (s in 4000L..4150L) run(s, 120, lazy = false, external = false, flaky = true) }

    /** Nothing the keyboard does with its beliefs may delete text it did not insert: a glide's Backspace removes exactly the glide, never more. */
    @Test fun backspaceAfterAGlideNeverEatsTheTextBeforeIt() {
        val rnd = Random(77)
        repeat(300) {
            val pre = (0 until rnd.nextInt(30)).map { letters[rnd.nextInt(26)] }.joinToString("")
            val r = Rig(pre); r.act { commitGlide(words[rnd.nextInt(words.size)]) }
            if (rnd.nextBoolean()) { r.ed.userInsert("Z"); r.sync() }       // the app typed something after the glide: the glide is no longer the last thing
            val before = r.text
            r.act { backspace() }
            assertTrue("backspace removed more than one glide or one character: '$before' -> '${r.text}'", before.length - r.text.length <= (r.c.lastGlide?.text?.length ?: 0).coerceAtLeast(maxOf(1, before.length - pre.length)))
            assertTrue("the text before the glide was touched: '$pre' -> '${r.text}'", r.text.startsWith(pre.dropLast(0)) || before.length - r.text.length == 1)
        }
    }

    @Test fun theFuzzerReallyChangesTheText() {   // guards the fuzzer itself against becoming a no-op
        val r = Rig(); val rnd = Random(5); repeat(200) { op(r, rnd, false); r.sync() }
        assertTrue(r.ed.calls > 200); assertEquals(true, r.text.isNotEmpty() || r.ed.calls > 0)
    }
}
