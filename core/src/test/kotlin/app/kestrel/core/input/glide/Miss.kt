package app.kestrel.core.input.glide
import java.util.Random
object Miss {
    @JvmStatic fun main(args: Array<String>) {
        val a = GlideEval.parse(args)
        val old = RareWordEval.load(a["old"]!!); val new = RareWordEval.load(a["new"]!!)
        val keys = GlideEval.keysFor(); val noise = GlideEval.noiseFrom(a)
        val words = RareWordEval.midWords(old, new, a["n"]?.toInt() ?: 3000, 7919)
        val sim = GestureSim(keys, Random(1)); val dec = GlideDecoder(keys)
        var inOld = 0; var notOld = 0; var shown = 0
        for (w in words) { val p = sim.path(w, noise) ?: continue
            val c = dec.decode(p.xs, p.ys, p.xs.size, listOf(new), null, 3).firstOrNull() ?: continue
            if (String(GlideLetters.keyOf(c.text)) == String(GlideLetters.keyOf(w))) continue
            val inO = old.idOf(c.text) >= 0
            if (inO) inOld++ else { notOld++; if (shown++ < (a["show"]?.toInt() ?: 25)) println("  meant %s (new lp %.1f, old lp %.1f) got %s (new lp %.1f) cost %.2f".format(w, new.logProb(new.idOf(w)), old.logProb(old.idOf(w)), c.text, new.logProb(new.idOf(c.text)), c.cost)) }
        }
        println("misses whose winner is in the old list: $inOld, only in the new list: $notOld")
    }
}
