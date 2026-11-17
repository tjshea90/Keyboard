package app.kestrel.core.input.glide

import java.util.Random

object TuiProbe2 {
    @JvmStatic fun main(args: Array<String>) {
        val a = GlideEval.parse(args)
        val lex = RareWordEval.load(a["new"]!!)
        val keys = GlideEval.keysFor()
        val target = a["target"] ?: "tui"
        val rareW = a["rareW"]?.toFloat() ?: 0f
        val ws = (a["words"] ?: "to,too,two,top,toy,tie,tin,tip,tell,thank,this,that,they,their,there,thing,time,today,tonight,tomorrow,to,too,tuesday,tui,try,toll,total,town,trip,true,tour").split(',')
        for (nm in listOf("calm", "normal", "sloppy", "bad")) {
            val noise = GlideEval.noiseFrom(mapOf("noise" to nm))
            val sim = GestureSim(keys, Random(9)); val dec = GlideDecoder(keys, GlideParams(rareW = rareW))
            val hits = HashMap<String, Int>(); var n = 0
            repeat(150) { for (w in ws) { val p = sim.path(w, noise) ?: continue; n++
                val c = dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), null, 3).firstOrNull() ?: continue
                if (c.text == target) hits[w] = (hits[w] ?: 0) + 1 } }
            println("$nm: '$target' first in ${hits.values.sum()} of $n glides: $hits")
        }
    }
}
