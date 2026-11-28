package app.kestrel.core.input.glide

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.Random

/**
 * R36: would the Glide training WORK? A made-up hand (a thumb offset, plus its own habit for the way it draws every word) glides the real training script ([GlideTraining]: paragraphs, look-alike drills, the page of
 * words that went wrong) on the owner's key geometries, the keyboard is taught every glide ([GlideTeaching]), and afterwards the same hand glides 2 000 everyday words that were NOT asked for, decoded by the taught
 * keyboard and by one that learned nothing. Everything is simulation: it says what the teaching CAN do for a hand that keeps its own habits (the generous case: real hands also change from day to day), not what the owner's
 * hand does; the number that counts will come from his own training file (PracticeEval.replay). Skipped unless REAL_TABLET=1.
 *   LAB_GEOS=tablet LAB_VARIANTS=all,thumb  LAB_SEEDS=3
 */
class TrainingLabTest {
    private fun keyOf(s: String) = String(GlideLetters.keyOf(s))

    private fun habitsFor(words: Collection<String>, seed: Long, habitDp: Float): Map<String, List<FloatArray?>> {
        val out = HashMap<String, List<FloatArray?>>()
        for (w in words) {
            val r = Random(seed * 7919 + w.hashCode())
            val m = GlideHabitEval.distinctLetters(w)
            out[w] = List(m) { floatArrayOf(r.nextGaussian().toFloat() * habitDp, r.nextGaussian().toFloat() * habitDp) }
        }
        return out
    }

    private class Result(val trainThirds: List<Double>, val learned: Double, val plain: Double, val spoiled: Int, val fixed: Int, val scriptLearned: Double, val scriptPlain: Double, val taughtGlides: Int, val habitDp: Float, val planeDp: Float)

    private fun parts(v: String, pitch: Float): GlideTeaching.Parts? = when (v) {
        "none" -> null
        "all" -> GlideTeaching.Parts(thumb = true, shape = true, confusion = true)
        "shipped" -> GlideTeaching.Parts.forPitchDp(pitch)
        "thumbshape" -> GlideTeaching.Parts(thumb = true, shape = true, confusion = false)
        "thumb" -> GlideTeaching.Parts(thumb = true, shape = false, confusion = false)
        "shape" -> GlideTeaching.Parts(thumb = false, shape = true, confusion = false)
        "confusion" -> GlideTeaching.Parts(thumb = false, shape = false, confusion = true)
        "nothumb" -> GlideTeaching.Parts(thumb = false, shape = true, confusion = true)
        else -> error("variant $v")
    }

    private fun run(lex: BinaryLexicon, keys: GlideKeys, variant: String, seed: Long, trainAim: Float, testAim: Float, habitDp: Float, userBias: Float, holdout: Int, pitchForParams: Float = keys.pitchDp): Result {
        val script = GlideTraining()
        val scriptWords = script.practice.words.toSet()
        val base = GlideEval.frequencyPick(lex, 4000, Random(seed * 13), 2).map { lex.text(it.id) }
        val allWords = scriptWords + base + listOf("our", "or", "out", "there", "the", "they", "then", "and", "any", "access", "for", "from")
        val fixed = habitsFor(allWords, seed, habitDp)
        val handTrain = GlideHabitEval.Hand(keys, SimNoise.aim(trainAim).withUserBias(userBias), seed, 0f, fixed)
        val handTest = GlideHabitEval.Hand(keys, SimNoise.aim(testAim).withUserBias(userBias), seed + 1, 0f, fixed)
        val params = GlideParams.forPitchDp(pitchForParams)
        val dec = GlideDecoder(keys, params); val model = PersonalModel(); dec.personal = model
        val plain = GlideDecoder(keys, GlideParams.forPitchDp(pitchForParams, touchField = false).let { GlideParams(shapeW = it.shapeW, visitCap = it.visitCap, lmMin = it.lmMin, patGain = 0f, touchField = false, letterW = it.letterW) })
        val lexicons = listOf<GlideLexicon>(lex)
        val p = parts(variant, keys.pitchDp)
        // ---- the training pass: the real script, glided in order, taught after every glide
        val hits = ArrayList<Boolean>()
        var taught = 0
        var guard = 0
        while (!script.done && guard++ < 5000) {
            if (script.atBreak) { script.continueNext(); continue }
            val w = script.current ?: break
            val pg = script.page!!
            // the words the keyboard sees: the two before it in the paragraph (null after a sentence mark and on a drill page)
            var prev: String? = null; var prev2: String? = null
            if (pg.kind == GlideTraining.Kind.TEXT) {
                val i = script.tokenIndex
                fun wordAt(j: Int): String? = if (j < 0) null else pg.tokens[j].shown.filter { it.isLetter() }.lowercase().takeIf { it.isNotEmpty() }
                val endsSentence = { j: Int -> j >= 0 && pg.tokens[j].shown.last() in ".!?" }
                if (i >= 1 && !endsSentence(i - 1)) { prev = wordAt(i - 1); if (i >= 2 && !endsSentence(i - 2)) prev2 = wordAt(i - 2) }
            }
            val path = handTrain.path(w)
            if (path == null) { script.skip(); continue }
            val c = dec.decode(path.xs, path.ys, path.xs.size, lexicons, prev, 3, prev2)
            val top = c.map { it.text }
            hits.add(top.firstOrNull() == w)
            script.record(path.xs, path.ys, FloatArray(path.xs.size) { it * 12f }, path.xs.size, top, prev, prev2)
            if (p != null) { GlideTeaching.teach(dec, lex, lex.idOf(w), model, keys, w, top.firstOrNull(), dec.lastPrint, p); taught++ }
        }
        val third = hits.size / 3
        val thirds = (0 until 3).map { k -> val s = hits.subList(k * third, if (k == 2) hits.size else (k + 1) * third); 100.0 * s.count { it } / s.size.coerceAtLeast(1) }
        // ---- the hold-out: everyday words nobody asked for, in short runs so that the words before them are real
        val rnd = Random(seed * 31 + 7)
        var n = 0; var hitL = 0; var hitP = 0; var spoiled = 0; var fixedN = 0; var sN = 0; var sL = 0; var sP = 0
        while (n < holdout) {
            val q = List(1 + rnd.nextInt(3)) { base[rnd.nextInt(base.size)] }
            var prev: String? = null; var prev2: String? = null
            for (w in q) {
                if (n >= holdout) break
                val path = handTest.path(w) ?: continue
                val key = keyOf(w)
                val c = dec.decode(path.xs, path.ys, path.xs.size, lexicons, prev, 3, prev2)
                val c0 = plain.decode(path.xs, path.ys, path.xs.size, lexicons, prev, 3, prev2)
                val ok = c.isNotEmpty() && keyOf(c[0].text) == key; val ok0 = c0.isNotEmpty() && keyOf(c0[0].text) == key
                n++; if (ok) hitL++; if (ok0) hitP++; if (ok0 && !ok) spoiled++; if (ok && !ok0) fixedN++
                if (w in scriptWords) { sN++; if (ok) sL++; if (ok0) sP++ }
                prev2 = prev; prev = w
            }
        }
        val o = FloatArray(2); var plane = 0f
        for (u in floatArrayOf(0f, 1f)) for (v in floatArrayOf(0f, 1f)) { dec.field.at(u, v, o); plane = Math.max(plane, Math.hypot(o[0].toDouble(), o[1].toDouble()).toFloat()) }
        return Result(thirds, 100.0 * hitL / n, 100.0 * hitP / n, spoiled, fixedN, 100.0 * sL / sN.coerceAtLeast(1), 100.0 * sP / sN.coerceAtLeast(1), taught, Math.hypot(dec.biasX.toDouble(), dec.biasY.toDouble()).toFloat() * keys.pitchDp, plane * keys.pitchDp)
    }

    @Test fun whatTheTrainingScriptTeachesAHandThatKeepsItsOwnHabits() {
        assumeTrue("set REAL_TABLET=1", System.getenv("REAL_TABLET") == "1")
        val lex = GlideEval.loadLexicon()
        val geos = listOf("phone 292x161" to (292f to 161f), "tablet portrait 564x272" to (564f to 272f), "tablet landscape 916x272" to (916f to 272f))
        val only = System.getenv("LAB_GEOS")?.split(',')
        val variants = (System.getenv("LAB_VARIANTS") ?: "none,all,thumb,shape,confusion").split(',')
        val seeds = (System.getenv("LAB_SEEDS")?.toIntOrNull() ?: 2)
        val hold = System.getenv("LAB_HOLD")?.toIntOrNull() ?: 2000
        val habit = System.getenv("LAB_HABIT")?.toFloatOrNull() ?: 8f
        val bias = System.getenv("LAB_BIAS")?.toFloatOrNull() ?: 6f
        val trainAim = System.getenv("LAB_TRAIN_AIM")?.toFloatOrNull() ?: 8f
        val testAim = System.getenv("LAB_TEST_AIM")?.toFloatOrNull() ?: 10f
        for ((name, wh) in geos) {
            if (only != null && only.none { name.contains(it) }) continue
            val keys = GlideEval.keysFor(wh.first, wh.second)
            for (variant in variants) {
                val rs = (1..seeds).map { run(lex, keys, variant, it.toLong(), trainAim, testAim, habit, bias, hold) }
                fun avg(f: (Result) -> Double) = rs.map(f).average()
                println("TRAINLAB %-24s teach %-9s hold-out first choice learned %.1f%% plain %.1f%% (spoiled %.0f fixed %.0f of %d) | words of the script learned %.1f%% plain %.1f%% | training thirds %.0f/%.0f/%.0f%% | habit %.1f dp plane %.1f dp | habit %.0f dp word, thumb %.0f dp, aim %.0f->%.0f".format(
                    name, variant, avg { it.learned }, avg { it.plain }, avg { it.spoiled.toDouble() }, avg { it.fixed.toDouble() }, hold, avg { it.scriptLearned }, avg { it.scriptPlain },
                    avg { it.trainThirds[0] }, avg { it.trainThirds[1] }, avg { it.trainThirds[2] }, avg { it.habitDp.toDouble() }, avg { it.planeDp.toDouble() }, habit, bias, trainAim, testAim))
            }
        }
    }
}
