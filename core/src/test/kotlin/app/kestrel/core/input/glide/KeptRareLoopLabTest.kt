package app.kestrel.core.input.glide

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.Random

/**
 * R33 scratch lab (REAL_TABLET=1): how fast does a kept look-alike (ave for and) grow into the first choice with the personal model as it was? A path halfway between the clean "and" and the clean "ave", decoded with the
 * real word list after k kept uses of "ave" ([PersonalModel.add] with weight 1, exactly what a kept glide taught before R33).
 */
class KeptRareLoopLabTest {
    @Test fun lab() {
        assumeTrue("set REAL_TABLET=1", System.getenv("REAL_TABLET") == "1")
        val lex = GlideEval.loadLexicon()
        val keys = GlideEval.keysFor(564f, 272f)
        val none = SimNoise(0f, 0f, 0f, 0f, 0f, "none")
        val sim = GestureSim(keys, Random(1))
        fun resample(p: GestureSim.Path, n: Int): Pair<FloatArray, FloatArray> {
            val d = FloatArray(p.xs.size); for (i in 1 until p.xs.size) d[i] = d[i - 1] + Math.hypot((p.xs[i] - p.xs[i - 1]).toDouble(), (p.ys[i] - p.ys[i - 1]).toDouble()).toFloat()
            val xs = FloatArray(n); val ys = FloatArray(n); var j = 1
            for (i in 0 until n) { val t = d.last() * i / (n - 1); while (j < d.size - 1 && d[j] < t) j++; val a = d[j - 1]; val b = d[j]; val f = if (b > a) (t - a) / (b - a) else 0f; xs[i] = p.xs[j - 1] + f * (p.xs[j] - p.xs[j - 1]); ys[i] = p.ys[j - 1] + f * (p.ys[j] - p.ys[j - 1]) }
            return xs to ys
        }
        val pa = resample(sim.path("and", none)!!, 60); val pv = resample(sim.path("ave", none)!!, 60)
        val params = GlideParams.forPitchDp(keys.pitch)
        for (mix in listOf(0.2f, 0.35f, 0.5f, 0.65f)) {
            val xs = FloatArray(60) { (1 - mix) * pa.first[it] + mix * pv.first[it] }; val ys = FloatArray(60) { (1 - mix) * pa.second[it] + mix * pv.second[it] }
            val line = StringBuilder("LAB mix(ave)=$mix:")
            for (k in listOf(0, 1, 2, 4, 10)) {
                val pm = PersonalModel(); repeat(k) { pm.add(null, "ave", 1f) }
                val dec = GlideDecoder(keys, params); dec.personal = if (k == 0) null else pm
                val c = dec.decode(xs, ys, xs.size, listOf<GlideLexicon>(lex), null, 3)
                line.append("  k=$k -> ${c.take(3).joinToString("/") { it.text }}")
            }
            println(line)
        }
        println("LAB ln: and=%.1f ave=%.1f".format(lex.logProb(lex.idOf("and")), lex.logProb(lex.idOf("ave"))))
    }
}
