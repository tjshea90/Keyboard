package app.kestrel.core.input.glide

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * R34a2: re-decodes a GLIDE PRACTICE FILE (the owner's real glides on his own tablet, saved by Settings > Help > Glide practice and attached to the chat) exactly as the keyboard decoded them: the recorded key
 * positions and view size, the shipped word list, the shipped decoder with the tablet's own values, and the open network's second look. Skipped unless PRACTICE_FILE=<the file> (an absolute path; the file stays where it
 * was put, outside the repository: tools/secretscan.sh refuses it). Prints first choice / first three for: what the keyboard offered when the glides were made (the file's own words), the decoder alone, and the hybrid;
 * the misses with their path-fit and language parts; and a sweep of the values that are only guesses for big keys (`shapeW`, `visitCap`, `lmMin`, the network weight) so the tablet values can be picked on real swipes.
 *
 *   PRACTICE_FILE=/abs/path/practice.txt java -cp <core main>:<core test>:<kotlin stdlib>:<junit>:<hamcrest> org.junit.runner.JUnitCore app.kestrel.core.input.glide.PracticeEval
 * (the shape network weights are read from build/r30/swipe_encoder.bin when present; copy ime/src/main/assets/glide/swipe_encoder.bin there)
 */
class PracticeEval {
    private fun letters(t: String) = IntArray(t.count { it in 'a'..'z' }).also { var i = 0; for (c in t) if (c in 'a'..'z') it[i++] = c - 'a' }

    private class Case(val word: String, val xs: FloatArray, val ys: FloatArray, val ts: FloatArray, val kept: List<String>)

    private fun run(label: String, keys: GlideKeys, cases: List<Case>, lex: BinaryLexicon, params: GlideParams, net: ShapeNet?, w: Float, gate: Float, verbose: Boolean): Triple<Int, Int, Int> {
        val dec = GlideDecoder(keys, params)
        val rescorer = net?.let { ShapeRescorer(it, keys, w, gate) }
        var first = 0; var three = 0; var n = 0
        for (c in cases) {
            val cs0 = dec.decode(c.xs, c.ys, c.xs.size, listOf(lex), null, 40)
            if (cs0.isEmpty()) { n++; if (verbose) println("PRACTICE $label miss: '${c.word}' -> nothing"); continue }
            val cs = rescorer?.rescore(c.xs, c.ys, c.ts, c.xs.size, cs0)?.candidates ?: cs0
            n++
            val top = cs.take(3).map { it.text }
            if (top.firstOrNull() == c.word) first++
            if (c.word in top) three++
            if (verbose && top.firstOrNull() != c.word) {
                val me = cs.indexOfFirst { it.text == c.word }
                println("PRACTICE $label miss: '${c.word}' -> '${top.firstOrNull()}' (meant word at rank ${if (me >= 0) me + 1 else -1} of ${cs.size}; keyboard then offered ${c.kept})")
            }
        }
        return Triple(n, first, three)
    }

    @Test fun practiceFile() {
        val path = System.getenv("PRACTICE_FILE")
        assumeTrue("set PRACTICE_FILE=<a glide practice file>", !path.isNullOrEmpty())
        val parsed = GlidePractice.parse(File(path!!).readText()) ?: error("not a glide practice file")
        val keys = parsed.keys
        val lex = GlideEval.loadLexicon()
        val cases = parsed.entries.filter { it.xs.size >= 2 }.map { Case(it.word, it.xs, it.ys, it.ts, it.got) }
        println("PRACTICE file: app ${parsed.version}, view ${parsed.viewW} x ${parsed.viewH} px at ${parsed.density}, key pitch %.1f dp (key %.1f x %.1f px), ${cases.size} glides".format(keys.pitchDp, keys.keyW, keys.keyH))
        val gotFirst = cases.count { it.kept.firstOrNull() == it.word }; val gotThree = cases.count { it.word in it.kept }
        println("PRACTICE the keyboard's own answers when they were made: first %d / %d (%.1f %%), in the first three %d (%.1f %%)".format(gotFirst, cases.size, 100.0 * gotFirst / cases.size, gotThree, 100.0 * gotThree / cases.size))
        val netFile = File("build/r30/swipe_encoder.bin")
        val net = if (netFile.exists()) ShapeNet.load(netFile.readBytes()) else null
        val base = GlideParams.forPitchDp(keys.pitchDp)
        fun show(label: String, r: Triple<Int, Int, Int>) = println("PRACTICE %-44s first %3d / %d (%.1f %%)  first three %3d (%.1f %%)".format(label, r.second, r.first, 100.0 * r.second / r.first, r.third, 100.0 * r.third / r.first))
        show("decoder alone, the shipped values", run("alone", keys, cases, lex, base, null, 0f, 0f, true))
        if (net != null) show("decoder + network, the shipped values", run("hybrid", keys, cases, lex, base, net, ShapeRescorer.DEFAULT_WEIGHT, ShapeRescorer.DEFAULT_GATE, true))
        else println("PRACTICE no build/r30/swipe_encoder.bin: the network is not used")
        // the values that are only guesses for big keys (research/R24 section 4: THE TABLET SIDE HAS NO REAL-SWIPE EVIDENCE)
        println("PRACTICE sweep of the big-key values (decoder alone, then with the network):")
        for (shapeW in listOf(0.5f, 1.0f, 1.5f, 2.0f, 3.0f)) for (visitCap in listOf(3f, 5f)) for (lmMin in listOf(0.12f, 0.45f, 0.8f)) {
            val p = GlideParams(shapeW = shapeW, visitCap = visitCap, lmMin = lmMin, touchField = base.touchField, letterW = base.letterW, patMaxGap = base.patMaxGap, confMaxGap = base.confMaxGap)
            val a = run("sweep", keys, cases, lex, p, null, 0f, 0f, false)
            val h = if (net != null) run("sweep", keys, cases, lex, p, net, ShapeRescorer.DEFAULT_WEIGHT, ShapeRescorer.DEFAULT_GATE, false) else a
            println("PRACTICE   shapeW %.1f visitCap %.0f lmMin %.2f: alone %.1f / %.1f %%, with network %.1f / %.1f %%".format(shapeW, visitCap, lmMin, 100.0 * a.second / a.first, 100.0 * a.third / a.first, 100.0 * h.second / h.first, 100.0 * h.third / h.first))
        }
    }
}
