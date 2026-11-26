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
    @Test fun practiceFile() {
        val path = System.getenv("PRACTICE_FILE")
        assumeTrue("set PRACTICE_FILE=<a glide practice file>", !path.isNullOrEmpty())
        val parsed = GlidePractice.parse(File(path!!).readText()) ?: error("not a glide practice file")
        val netFile = File("build/r30/swipe_encoder.bin")
        report(parsed, GlideEval.loadLexicon(), if (netFile.exists()) ShapeNet.load(netFile.readBytes()) else null, sweep = true) { println(it) }
    }

    companion object {
        private class Case(val word: String, val xs: FloatArray, val ys: FloatArray, val ts: FloatArray, val kept: List<String>, val prev: String? = null, val prev2: String? = null)

        private fun run(label: String, keys: GlideKeys, cases: List<Case>, lex: BinaryLexicon, params: GlideParams, net: ShapeNet?, w: Float, gate: Float, say: ((String) -> Unit)?): Triple<Int, Int, Int> {
            val dec = GlideDecoder(keys, params)
            val rescorer = net?.let { ShapeRescorer(it, keys, w, gate) }
            var first = 0; var three = 0; var n = 0
            for (c in cases) {
                val cs0 = dec.decode(c.xs, c.ys, c.xs.size, listOf(lex), c.prev, 40, c.prev2)
                n++
                if (cs0.isEmpty()) { say?.invoke("PRACTICE $label miss: '${c.word}' -> nothing"); continue }
                val cs = rescorer?.rescore(c.xs, c.ys, c.ts, c.xs.size, cs0)?.candidates ?: cs0
                val top = cs.take(3).map { it.text }
                if (top.firstOrNull() == c.word) first++
                if (c.word in top) three++
                if (say != null && top.firstOrNull() != c.word) {
                    val me = cs.indexOfFirst { it.text == c.word }
                    say("PRACTICE $label miss: '${c.word}' -> '${top.firstOrNull()}' (meant word at rank ${if (me >= 0) me + 1 else -1} of ${cs.size}; the keyboard then offered ${c.kept})")
                }
            }
            return Triple(n, first, three)
        }

        /**
         * R36: what TEACHING would have done with these glides: the same glides in the order they were made, decoded by a keyboard that is taught each glide's word after it is decoded ([GlideTeaching], all lessons, as the
         * Glide training screen does), against one that learns nothing. Reports first-choice rates per quarter, the thumb habit and the touch plane that were learned, and the words whose answer changed either way.
         * With a file of distinct words only the thumb lessons can carry over to the next glide; a training file repeats words and the shape lessons show too.
         */
        fun replay(parsed: GlidePractice.Parsed, lex: BinaryLexicon, say: (String) -> Unit, parts: GlideTeaching.Parts = GlideTeaching.Parts.forPitchDp(parsed.keys.pitchDp)): Pair<Int, Int> {
            val keys = parsed.keys
            val cases = parsed.entries.filter { it.xs.size >= 2 }
            if (cases.isEmpty()) return 0 to 0
            val params = GlideParams.forPitchDp(keys.pitchDp)
            val learned = GlideDecoder(keys, params); val plain = GlideDecoder(keys, params)
            val model = PersonalModel(); learned.personal = model
            val q = 4
            val seenQ = IntArray(q); val hitL = IntArray(q); val hitP = IntArray(q)
            val gained = ArrayList<String>(); val lost = ArrayList<String>()
            for ((i, c) in cases.withIndex()) {
                val b = (i * q / cases.size).coerceAtMost(q - 1)
                val l = learned.decode(c.xs, c.ys, c.xs.size, listOf(lex), c.prev, 5, c.prev2)
                val pl = plain.decode(c.xs, c.ys, c.xs.size, listOf(lex), c.prev, 5, c.prev2)
                val lw = l.firstOrNull()?.text; val pw = pl.firstOrNull()?.text
                seenQ[b]++; if (lw == c.word) hitL[b]++; if (pw == c.word) hitP[b]++
                if (lw == c.word && pw != c.word) gained.add("${c.word}(was $pw)") else if (lw != c.word && pw == c.word) lost.add("${c.word}(now $lw)")
                GlideTeaching.teach(learned, lex, lex.idOf(c.word), model, keys, c.word, lw, learned.lastPrint, parts)
            }
            val n = seenQ.sum(); val a = hitL.sum(); val b = hitP.sum()
            say("PRACTICE replay with teaching: first %d / %d (%.1f %%) against %d / %d (%.1f %%) for a keyboard that learns nothing; by quarter taught %s plain %s".format(a, n, 100.0 * a / n, b, n, 100.0 * b / n,
                (0 until q).joinToString(" ") { "%.0f".format(100.0 * hitL[it] / seenQ[it].coerceAtLeast(1)) }, (0 until q).joinToString(" ") { "%.0f".format(100.0 * hitP[it] / seenQ[it].coerceAtLeast(1)) }))
            val o = FloatArray(2); var plane = 0f
            for (u in floatArrayOf(0f, 1f)) for (v in floatArrayOf(0f, 1f)) { learned.field.at(u, v, o); plane = Math.max(plane, Math.hypot(o[0].toDouble(), o[1].toDouble()).toFloat()) }
            say("PRACTICE replay: thumb habit learned x %.2f y %.2f pitches (%.1f, %.1f dp), touch plane up to %.1f dp, %d shapes of %d words, %d corrections".format(learned.biasX, learned.biasY, learned.biasX * keys.pitchDp, learned.biasY * keys.pitchDp, plane * keys.pitchDp, model.patterns.patternCount, model.patterns.wordCount, model.confusionCount))
            if (gained.isNotEmpty()) say("PRACTICE replay: words the teaching mended: ${gained.joinToString(", ")}")
            if (lost.isNotEmpty()) say("PRACTICE replay: words the teaching spoiled: ${lost.joinToString(", ")}")
            return a to b
        }

        /** Prints what the keyboard did when the glides were made, what the shipped decoder (alone and with the network) does with them now, and, with [sweep], how the guessed big-key values change that. Returns (glides, first right, first three) of the hybrid (or the decoder alone without a network). */
        fun report(parsed: GlidePractice.Parsed, lex: BinaryLexicon, net: ShapeNet?, sweep: Boolean, say: (String) -> Unit): Triple<Int, Int, Int> {
            val keys = parsed.keys
            val cases = parsed.entries.filter { it.xs.size >= 2 }.map { Case(it.word, it.xs, it.ys, it.ts, it.got, it.prev, it.prev2) }
            say("PRACTICE file: app ${parsed.version}, view ${parsed.viewW} x ${parsed.viewH} px at ${parsed.density}, key pitch %.1f dp (key %.1f x %.1f px), ${cases.size} glides".format(keys.pitchDp, keys.keyW, keys.keyH))
            val gotFirst = cases.count { it.kept.firstOrNull() == it.word }; val gotThree = cases.count { it.word in it.kept }
            say("PRACTICE the keyboard's own answers when they were made: first %d / %d (%.1f %%), in the first three %d (%.1f %%)".format(gotFirst, cases.size, 100.0 * gotFirst / cases.size.coerceAtLeast(1), gotThree, 100.0 * gotThree / cases.size.coerceAtLeast(1)))
            val base = GlideParams.forPitchDp(keys.pitchDp)
            fun show(label: String, r: Triple<Int, Int, Int>) = say("PRACTICE %-44s first %3d / %d (%.1f %%)  first three %3d (%.1f %%)".format(label, r.second, r.first, 100.0 * r.second / r.first.coerceAtLeast(1), r.third, 100.0 * r.third / r.first.coerceAtLeast(1)))
            val alone = run("alone", keys, cases, lex, base, null, 0f, 0f, say)
            show("decoder alone, the shipped values", alone)
            var result = alone
            if (net != null) { result = run("hybrid", keys, cases, lex, base, net, ShapeRescorer.DEFAULT_WEIGHT, ShapeRescorer.DEFAULT_GATE, say); show("decoder + network, the shipped values", result) }
            else say("PRACTICE no network weights (build/r30/swipe_encoder.bin): the network is not used")
            if (sweep) {
                // the values that are only guesses for big keys (research/R24 section 4: the tablet side has no real-swipe evidence)
                say("PRACTICE sweep of the big-key values (decoder alone, then with the network):")
                for (shapeW in listOf(0.5f, 1.0f, 1.5f, 2.0f, 3.0f)) for (visitCap in listOf(3f, 5f)) for (lmMin in listOf(0.12f, 0.45f, 0.8f)) {
                    val p = GlideParams(shapeW = shapeW, visitCap = visitCap, lmMin = lmMin, touchField = base.touchField, letterW = base.letterW, patMaxGap = base.patMaxGap, confMaxGap = base.confMaxGap)
                    val a = run("sweep", keys, cases, lex, p, null, 0f, 0f, null)
                    val h = if (net != null) run("sweep", keys, cases, lex, p, net, ShapeRescorer.DEFAULT_WEIGHT, ShapeRescorer.DEFAULT_GATE, null) else a
                    say("PRACTICE   shapeW %.1f visitCap %.0f lmMin %.2f: alone %.1f / %.1f %%, with network %.1f / %.1f %%".format(shapeW, visitCap, lmMin, 100.0 * a.second / a.first.coerceAtLeast(1), 100.0 * a.third / a.first.coerceAtLeast(1), 100.0 * h.second / h.first.coerceAtLeast(1), 100.0 * h.third / h.first.coerceAtLeast(1)))
                }
            }
            replay(parsed, lex, say)
            return result
        }
    }
}
