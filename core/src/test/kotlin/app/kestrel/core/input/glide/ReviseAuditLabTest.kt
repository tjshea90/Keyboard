package app.kestrel.core.input.glide

import app.kestrel.core.input.Key
import app.kestrel.core.input.editor.Rig
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.Random
import java.util.concurrent.Executor

/**
 * R40 lab (set R40_LAB=1): the retroactive fix of the word before ([GlideSession.revisePrevious]) on EVERYDAY sentences, the way the owner writes, with the real engine and the real session over a simulated thumb.
 * Three variants on the same sentences and the same thumbs: "off" (Settings: Fix the word before = off), "unguarded" (R26a as shipped until alpha17) and "guarded" (R40: [CorrectionGuard]). Every change the fix makes to a word on
 * screen is labelled against the word the writer meant: MENDED (wrong -> right), SPOILED (right -> wrong), neutral (wrong -> another wrong word); and a change that swaps an everyday word (ln share -7 or more) for one that is 3 nats or
 * more rarer is counted separately: that is the "to it -> yo it" class. The numbers go into research/R40-correction-policies.md.
 */
class ReviseAuditLabTest {
    private class Tally { var words = 0; var right = 0; var events = 0; var mended = 0; var spoiled = 0; var neutral = 0; var commonToRare = 0; var refused = 0; val examples = ArrayList<String>() }

    @Test fun lab() {
        assumeTrue("set R40_LAB=1", System.getenv("R40_LAB") == "1")
        val bytes = GlideEval.findAsset().readBytes()
        val lex = LexiconFile.read(bytes)
        val keys = GlideEval.keysFor(564f, 272f)
        val rounds = System.getenv("R40_ROUNDS")?.toIntOrNull() ?: 2
        fun ln(w: String): Float { val id = lex.idOf(w); return if (id < 0) -20f else lex.logProb(id) }
        for (noise in listOf(SimNoise.NORMAL, SimNoise.SLOPPY)) {
            val line = StringBuilder()
            for (variant in listOf("off", "unguarded", "guarded")) {
                val t = Tally()
                val engine = GlideTyping({ bytes }, Executor { it.run() }, savePersonal = { })
                engine.prepare(keys, null, null)
                for (round in 0 until rounds) {
                    val sim = GestureSim(keys, Random(1000L + round))
                    for (sentence in EvalCorpus.sentences) {
                        val rig = Rig("")
                        var clock = 1_000L
                        val s = GlideSession(rig.c, { engine }, { clock }, { })
                        s.reviseBefore = variant != "off"; s.guardRevisions = variant == "guarded"
                        s.ui = object : GlideSession.Ui { override fun showCandidates(words: List<String>) {}; override fun clearCandidates() {}; override fun showNoWord() {} }
                        val tokensBefore = ArrayList<String>()
                        for ((i, w) in sentence.withIndex()) {
                            clock += 700
                            val before = rig.text.trim().split(' ').filter { it.isNotEmpty() }
                            val path = if (lex.idOf(w) < 0) null else sim.path(w, noise)
                            if (path == null) {                                         // a one-letter word (a, I) or an unknown word: tapped, and right
                                for (ch in w) { val k = Key.Ch(ch.toString()); s.beforeKey(k); rig.c.type(ch.toString()); rig.sync(); s.afterKey(k) }
                                val sp = Key.Ch(" "); s.beforeKey(sp); rig.c.type(" "); rig.sync(); s.afterKey(sp)
                                continue
                            }
                            val (p2, p1) = s.context()
                            engine.decode(path.xs, path.ys, path.xs.size, p1, p2, null) { res -> s.onDecoded(res, p1, p2) }
                            rig.sync()
                            val after = rig.text.trim().split(' ').filter { it.isNotEmpty() }
                            // did the fix change the word before? (the new word is the last token; the one before it is the one that may have changed)
                            if (before.isNotEmpty() && after.size >= 2 && after.size == before.size + 1 && after[after.size - 2] != before.last()) {
                                val idx = i - 1
                                val old = before.last(); val new = after[after.size - 2]
                                val meant = sentence.getOrNull(idx)
                                t.events++
                                val oldRight = old == meant; val newRight = new == meant
                                if (!oldRight && newRight) t.mended++ else if (oldRight && !newRight) t.spoiled++ else t.neutral++
                                if (ln(old) >= -7f && ln(old) - ln(new) >= 3f) { t.commonToRare++; if (t.examples.size < 6) t.examples.add("$old -> $new (meant $meant)") }
                            }
                            tokensBefore.clear()
                        }
                        s.settle(); rig.sync()
                        val finalTokens = rig.text.trim().split(' ').filter { it.isNotEmpty() }
                        for ((i, w) in sentence.withIndex()) { if (lex.idOf(w) < 0) continue; t.words++; if (finalTokens.getOrNull(i) == w) t.right++ }
                    }
                }
                line.append("R40LAB ${noise.name} %-9s: words %d, right %.2f%%, retroactive changes %d (mended %d, spoiled %d, other %d), common word -> 3+ nats rarer: %d %s\n".format(variant, t.words, 100.0 * t.right / t.words, t.events, t.mended, t.spoiled, t.neutral, t.commonToRare, t.examples))
            }
            print(line)
        }
    }
}
