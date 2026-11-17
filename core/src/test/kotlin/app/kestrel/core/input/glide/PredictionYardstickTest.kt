package app.kestrel.core.input.glide

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * KB-406: how well the strip predicts while the person TAPS, on the shipped list with no personal model (the day the keyboard is installed). The floors below are the numbers of the day this test was
 * written minus a margin: they catch a change that makes prediction worse, they are not targets. Every run prints the whole table (Gradle: `--tests '*PredictionYardstickTest' -i`).
 */
class PredictionYardstickTest {
    private val lex = GlideEval.loadLexicon()
    private val keys = GlideEval.keysFor(564f, 160f)

    @Test fun theStripPredictsEverydayEnglish() {
        val r = PredictionEval.run(lex, null, keys, EvalCorpus.sentences, EvalCorpus.searches, verbose = System.getProperty("eval.verbose") != null)
        println("sentences ${EvalCorpus.sentences.size}, words ${EvalCorpus.sentences.sumOf { it.size }}, searches ${EvalCorpus.searches.size}")
        println(PredictionEval.report(r))
        for (n in r.notes) println(n)
        FLOORS.check(r)
    }

    /** Floors: set from the first measurement (see research/LEARNING_RESULTS.md section KB-406); raise them when the numbers go up for good. */
    private object FLOORS {
        fun check(r: PredictionEval.Result) {
            assertTrue("right words changed by a space: ${r.rightWordChanged} of ${r.rightWords}", r.rightWordChanged <= r.rightWords / 100 + 1)
            assertTrue("next word in 3: ${r.next}", r.next.r3 >= 0.05)
        }
    }
}
