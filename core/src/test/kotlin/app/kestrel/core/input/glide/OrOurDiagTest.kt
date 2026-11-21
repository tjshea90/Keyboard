package app.kestrel.core.input.glide

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.Random

/**
 * R29 diagnosis (owner 2026-11-21: "our or out when i try to type the word or"): where does "our" beat "or"? For simulated glides of one word, prints the average cost (path fit), prior (language) and total
 * score of the word meant and of each wrong winner, and how often the winner's PATH cost was the lower one (shape decided) against its PRIOR being the higher one (language decided). Skipped unless REAL_TABLET=1.
 * LAB_WORD (default or), LAB_GEO (phone / tablet portrait / tablet landscape, default tablet portrait).
 */
class OrOurDiagTest {
    private fun keyOf(s: String) = String(GlideLetters.keyOf(s))

    @Test fun whereTheWrongWordWins() {
        assumeTrue("set REAL_TABLET=1", System.getenv("REAL_TABLET") == "1")
        val lex = GlideEval.loadLexicon()
        val word = System.getenv("LAB_WORD") ?: "or"
        val geos = mapOf("phone" to (292f to 161f), "tablet portrait" to (564f to 272f), "tablet landscape" to (916f to 272f))
        val geo = System.getenv("LAB_GEO") ?: "tablet portrait"; val wh = geos[geo]!!
        val keys = GlideEval.keysFor(wh.first, wh.second)
        val dec = GlideDecoder(keys, GlideParams.forPitchDp(keys.pitch))
        val sim = GestureSim(keys, Random(11L))
        val aim = System.getenv("LAB_AIM")?.toFloatOrNull() ?: 8f; val prev = System.getenv("LAB_PREV")?.takeIf { it.isNotEmpty() }
        val noise = SimNoise.aim(aim)
        val n = 600
        var hit = 0; val wrong = HashMap<String, IntArray>()
        var meanCostRight = 0.0; var meanPriorRight = 0.0; var cnt = 0
        for (i in 0 until n) {
            val path = sim.path(word, noise) ?: continue
            val c = dec.decode(path.xs, path.ys, path.xs.size, listOf<GlideLexicon>(lex), prev, 3)
            if (c.isEmpty()) continue
            val mine = c.firstOrNull { keyOf(it.text) == keyOf(word) }
            if (keyOf(c[0].text) == keyOf(word)) { hit++; continue }
            val w = c[0]
            val a = wrong.getOrPut(w.text) { DoubleArray(0).let { IntArray(1) } }; a[0]++
            if (mine != null) {
                cnt++; meanCostRight += mine.cost.toDouble(); meanPriorRight += mine.prior.toDouble()
                if (cnt <= 12) println("OUR-DIAG #$cnt winner ${w.text}: cost %.2f prior %.2f score %.2f letters %d | meant ${mine.text}: cost %.2f prior %.2f score %.2f letters %d".format(w.cost, w.prior, w.score, w.letters, mine.cost, mine.prior, mine.score, mine.letters))
            }
        }
        println("OUR-DIAG $word on $geo aim ${aim.toInt()} after ${prev ?: "nothing"}: first choice right %.1f%%; wrong winners: %s".format(100.0 * hit / n, wrong.entries.sortedByDescending { it.value[0] }.take(5).joinToString { it.key + " " + it.value[0] }))
        println("OUR-DIAG static ln prior: or %.2f our %.2f out %.2f of %.2f".format(dec.staticLn(lex, "or"), dec.staticLn(lex, "our"), dec.staticLn(lex, "out"), dec.staticLn(lex, "of")))
    }
}
