package app.kestrel.core.input.editor

import org.junit.Test
import java.io.File
import java.util.Random

class DebugTmpTest {
    @Test fun trace() {
        val seed = 1000L
        val rnd = Random(seed)
        val start = listOf("", "hello world", "x".repeat(130), "line one\nline two")[rnd.nextInt(4)]
        val r = Rig(start, if (rnd.nextBoolean()) start.length else rnd.nextInt(start.length + 1), multiline = rnd.nextInt(4) != 0, action = if (rnd.nextInt(5) == 0) ImeActions.SEARCH else ImeActions.UNSPECIFIED)
        val out = StringBuilder("start len ${start.length} sel ${r.ed.selS}\n")
        val letters = "abcdefghijklmnopqrstuvwxyz"
        val words = listOf("hello", "world", "for", "fir", "the", "doraby", "a", "i'm", "typing")
        val punct = listOf(".", ",", "!", "?", "'", "/", ".com", "-", "(", ")", "1", "7", "😀", "é")
        for (i in 0 until 14) {
            val c = r.c
            val k = rnd.nextInt(20)
            val d = when (k) {
                0, 1, 2, 3, 4, 5, 19 -> { val ch = letters[rnd.nextInt(26)].toString(); c.type(ch); "type $ch" }
                6 -> { val p = punct[rnd.nextInt(punct.size)]; c.type(p); "type '$p'" }
                7 -> { c.space(); "space" }
                8, 9 -> { c.backspace(); "backspace" }
                10, 11 -> { val w = words[rnd.nextInt(words.size)]; c.commitGlide(w); "glide $w" }
                12 -> { val w = words[rnd.nextInt(words.size)]; val ok = c.swapGlide(w); "swap $w=$ok" }
                13 -> { val dd = rnd.nextInt(9) - 4; c.moveCursor(dd); "move $dd" }
                14 -> { c.enter(); "enter" }
                15 -> { c.tapShift(); "shift" }
                16 -> { c.lockShift(); "lock" }
                17 -> { c.setShift(ShiftState.OFF); "shiftOff" }
                else -> { val dd = rnd.nextInt(121) - 60; c.moveCursor(dd); "move $dd" }
            }
            out.append("#$i $d -> believed ${c.selStart}, editor ${r.ed.selS}, composing '${c.composing}', expected [${c.pendingForTest()}], editor queue ${r.ed.updates.map { "${it[0]}${if (it[2] >= 0) "c" else ""}" }}\n")
            repeat(rnd.nextInt(3)) { r.deliverOne() }
            while (r.ed.updates.size > 12) r.deliverOne()
            out.append("     after delivering: queue ${r.ed.updates.map { "${it[0]}${if (it[2] >= 0) "c" else ""}" }} foreign ${r.foreign} ${r.firstForeignInfo ?: ""}\n")
        }
        File(System.getenv("SWOOP_TRACE") ?: "/tmp/swoop-trace.txt").writeText(out.toString())
    }
}
