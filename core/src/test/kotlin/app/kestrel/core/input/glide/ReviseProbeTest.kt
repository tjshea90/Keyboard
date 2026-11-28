package app.kestrel.core.input.glide

import app.kestrel.core.input.editor.Rig
import org.junit.Test
import java.util.Random
import java.util.concurrent.Executor

/** R40 scratch probe (prints numbers only; replaced by the real lab/regression tests). */
class ReviseProbeTest {
    @Test fun printPairNumbers() {
        val lex = GlideEval.loadLexicon()
        fun u(w: String) = lex.idOf(w).let { if (it < 0) Float.NaN else lex.logProb(it) }
        fun b(a: String, c: String): Float { val ia = lex.idOf(a); val ic = lex.idOf(c); return if (ia < 0 || ic < 0) Float.NaN else lex.bigramLogProb(ia, ic) }
        for (w in listOf("to", "yo", "it", "and", "ave", "make", "maggie", "the", "for", "fur", "or", "our", "out", "age", "access", "form", "from"))
            println("PROBE unigram ln($w) = ${u(w)}")
        for ((a, c) in listOf("to" to "it", "yo" to "it", "and" to "the", "ave" to "the", "make" to "it", "maggie" to "it", "for" to "it", "fur" to "it"))
            println("PROBE bigram ln P($c | $a) = ${b(a, c)}")
    }

    @Test fun simulateToItWithLearnedYoIt() {
        val bytes = GlideEval.findAsset().readBytes()
        val keys = GlideEval.keysFor(564f, 160f)
        for (noise in listOf(SimNoise.NORMAL, SimNoise.SLOPPY)) for (k in listOf(0, 1, 2, 3)) for (revise in listOf(true, false)) {
            var changed = 0; val n = 300
            val seen = HashMap<String, Int>()
            for (seed in 1..n) {
                val rnd = Random(seed.toLong())
                val sim = GestureSim(keys, rnd)
                val rig = Rig("")
                val engine = GlideTyping({ bytes }, Executor { it.run() }, savePersonal = { })
                engine.prepare(keys, null, null)
                repeat(k) { engine.learnWord(null, "yo", 1f); engine.learnWord("yo", "it", 1f) }
                val s = GlideSession(rig.c, { engine }, { 1_000L }, { })
                s.reviseBefore = revise
                s.ui = object : GlideSession.Ui { override fun showCandidates(words: List<String>) {}; override fun clearCandidates() {}; override fun showNoWord() {} }
                for (w in listOf("to", "it")) {
                    val p = sim.path(w, noise) ?: continue
                    val (p2, p1) = s.context()
                    engine.decode(p.xs, p.ys, p.xs.size, p1, p2, null) { res -> s.onDecoded(res, p1, p2) }
                    rig.sync()
                }
                val t = rig.text.trim()
                seen[t] = (seen[t] ?: 0) + 1
                if (t != "to it") changed++
            }
            println("PROBE2 ${noise.name} learned yo-it x$k revise=$revise: not-'to it' = $changed/$n; " + seen.entries.sortedByDescending { it.value }.take(6).joinToString { "${it.key}=${it.value}" })
        }
    }

    @Test fun nextWordNumbers() {
        val bytes = GlideEval.findAsset().readBytes()
        val keys = GlideEval.keysFor(564f, 160f)
        val engine = GlideTyping({ bytes }, Executor { it.run() }, savePersonal = { })
        engine.prepare(keys, null, null)
        val cases = listOf(
            "it" to listOf("to", "yo", "go", "so", "no", "do", "of", "or", "ti"),
            "the" to listOf("and", "ave", "ane", "abd", "any", "access", "age"),
            "it" to listOf("make", "maggie", "mace", "male", "made", "take", "mike"),
            "you" to listOf("for", "fur", "foe", "fir", "fog", "got"),
            "it" to listOf("or", "our", "out", "ir", "of"),
            "to" to listOf("and", "ave", "age", "access"),
        )
        for ((next, cands) in cases) {
            val ln = engine.nextWordLnProbs(cands, next, null)
            println("PROBE3 next=$next: " + cands.indices.joinToString { "${cands[it]}=${ln?.get(it)?.let { v -> "%.2f".format(v) }}" })
        }
    }

    @Test fun simulateToIt() {
        val bytes = GlideEval.findAsset().readBytes()
        val keys = GlideEval.keysFor(564f, 160f)
        for (noise in listOf(SimNoise.NORMAL, SimNoise.SLOPPY)) {
            var changed = 0; val n = 300
            val seen = HashMap<String, Int>()
            for (seed in 1..n) {
                val rnd = Random(seed.toLong())
                val sim = GestureSim(keys, rnd)
                val rig = Rig("")
                val engine = GlideTyping({ bytes }, Executor { it.run() }, savePersonal = { })
                engine.prepare(keys, null, null)
                val s = GlideSession(rig.c, { engine }, { 1_000L }, { })
                s.ui = object : GlideSession.Ui { override fun showCandidates(words: List<String>) {}; override fun clearCandidates() {}; override fun showNoWord() {} }
                for (w in listOf("to", "it")) {
                    val p = sim.path(w, noise) ?: continue
                    val (p2, p1) = s.context()
                    engine.decode(p.xs, p.ys, p.xs.size, p1, p2, null) { res -> s.onDecoded(res, p1, p2) }
                    rig.sync()
                }
                val t = rig.text.trim()
                seen[t] = (seen[t] ?: 0) + 1
                if (t != "to it") changed++
            }
            println("PROBE ${noise.name}: of $n sessions typed 'to it' glides, not-'to it' = $changed; outcomes: " + seen.entries.sortedByDescending { it.value }.take(12).joinToString { "${it.key}=${it.value}" })
        }
    }
}
