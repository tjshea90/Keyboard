package app.kestrel.core.input.glide

import org.junit.Test

class TmpLengthSweepTest {
    private val lex by lazy { GlideEval.loadLexicon() }
    private val keys = GlideEval.keysFor()
    @Test fun why() {
        for (w in listOf("there", "were", "her")) {
            val id = lex.idOf(w)
            println("WHY $w prior=%.2f".format(lex.logProb(id)))
            GlideEval.run(lex, keys, GlideParams(), SimNoise.NORMAL, List(40) { GlideEval.Pick(id, -1) }, 5L + w.hashCode(), debug = 4, trace = w)
        }
    }
}
