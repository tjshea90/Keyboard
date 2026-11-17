package app.kestrel.core.input.glide

import java.util.Random

object TuiProbe3 {
    @JvmStatic fun main(args: Array<String>) {
        val a = GlideEval.parse(args)
        val lex = RareWordEval.load(a["new"]!!)
        val keys = GlideEval.keysFor()
        val rareW = a["rareW"]?.toFloat() ?: 0f
        val noise = GlideEval.noiseFrom(a)
        val sim = GestureSim(keys, Random(9)); val dec = GlideDecoder(keys, GlideParams(rareW = rareW))
        val counts = HashMap<String, Int>()
        for (w in (a["words"] ?: "tui").split(',')) { repeat(a["rep"]?.toInt() ?: 40) { val p = sim.path(w, noise) ?: return@repeat
            val c = dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), null, 3)
            val k = "$w -> " + (c.firstOrNull()?.text ?: "-") ; counts[k] = (counts[k] ?: 0) + 1
            if (it < 2) println("   $w: " + c.joinToString(" | ") { "%s cost %.2f prior %.1f score %.2f".format(it.text, it.cost, it.prior, it.score) } + " lm %.2f".format(dec.lastLm)) } }
        println(counts.entries.sortedByDescending { it.value })
    }
}
