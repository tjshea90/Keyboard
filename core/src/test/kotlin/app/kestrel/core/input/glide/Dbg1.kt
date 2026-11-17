package app.kestrel.core.input.glide
import java.util.Random
object Dbg1 {
    @JvmStatic fun main(args: Array<String>) {
        val keys = GlideEval.keysFor()
        fun scaled(entries: List<Pair<String, Long>>): BinaryLexicon { val sum = entries.sumOf { it.second }; val filler = listOf("zzfiller" to 541_808_760_578L - sum)
            return LexiconFile.read(LexiconFile.build((entries + filler).map { LexiconFile.Entry(it.first, it.second) }, emptyList())) }
        val tui = scaled(listOf("to" to 16_000_000_000L, "too" to 2_000_000_000L, "top" to 1_200_000_000L, "toy" to 500_000_000L, "tie" to 300_000_000L, "tip" to 400_000_000L, "tin" to 100_000_000L,
            "the" to 28_000_000_000L, "this" to 4_000_000_000L, "try" to 900_000_000L, "tour" to 400_000_000L, "tui" to 390_000L, "toi" to 250_000L, "tuk" to 200_000L, "tuis" to 150_000L))
        val common = listOf("to", "too", "top", "toy", "tie", "tip", "tin", "try", "tour", "this", "the")
        val sim = GestureSim(keys, Random(21)); val dec = GlideDecoder(keys)
        for (noise in listOf(SimNoise.CALM, SimNoise.NORMAL, SimNoise.SLOPPY, SimNoise.BAD)) { val s2 = GestureSim(keys, Random(21))
            repeat(60) { for (w in common) { val p = s2.path(w, noise) ?: continue
                val c = dec.decode(p.xs, p.ys, p.xs.size, listOf(tui), null, 3)
                if (c.firstOrNull()?.text in setOf("tui","toi","tuk","tuis")) println("${noise.name} $w -> " + c.joinToString(" | ") { "%s cost %.2f prior %.1f score %.2f".format(it.text, it.cost, it.prior, it.score) } + " lm %.2f".format(dec.lastLm)) } } }
    }
}
