package app.kestrel.core.input.glide

import java.util.Random

/** scratch probe: which rare words come first for glides of common words */
object TuiProbe {
    @JvmStatic fun main(args: Array<String>) {
        val a = GlideEval.parse(args)
        val lex = RareWordEval.load(a["new"]!!)
        val keys = GlideEval.keysFor()
        val noise = GlideEval.noiseFrom(a)
        val rareW = a["rareW"]?.toFloat() ?: 0f
        val words = RareWordEval.midWords(lex, lex, a["n"]?.toInt() ?: 6000, 3, top = a["top"]?.toInt() ?: 8000, minLetters = 2)
        val sim = GestureSim(keys, Random(5)); val dec = GlideDecoder(keys, GlideParams(rareW = rareW))
        val bad = HashMap<String, MutableList<String>>()
        var n = 0
        for (w in words) {
            val p = sim.path(w, noise) ?: continue; n++
            val c = dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), null, 3).firstOrNull() ?: continue
            if (String(GlideLetters.keyOf(c.text)) == String(GlideLetters.keyOf(w))) continue
            val id = lex.idOf(c.text)
            if (id >= 0 && lex.logProb(id) < (a["thr"]?.toFloat() ?: -14f)) bad.getOrPut(c.text) { ArrayList() }.add(w)
        }
        println("n=$n very-rare winners: ${bad.values.sumOf { it.size }}")
        for ((k, v) in bad.entries.sortedByDescending { it.value.size }.take(25)) println("   $k (lp %.1f) <- %s".format(lex.logProb(lex.idOf(k)), v.joinToString(",")))
    }
}
