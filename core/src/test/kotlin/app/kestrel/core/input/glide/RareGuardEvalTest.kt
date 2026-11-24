package app.kestrel.core.input.glide

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * R32e: [CommonGuard] measured on REAL swipes through the whole shipped pipeline (decoder, then the trained shape network where the shipped answer is not clearly ahead, then the guard).
 * Skipped unless REAL_SWIPES=1 and REAL_RARE=1. REAL_SWIPES_FILE picks the slice (default build/real-swipes-A.jsonl). Prints, for a grid of gaps and margins, how many glides the guard flips, how many of those were
 * mended (the common word is what was meant) and spoiled (the rare first guess was right), for all words and for everyday words (the 5000 commonest of the list).
 */
class RareGuardEvalTest {
    private val keys = GlideEval.keysFor()
    private val lex = GlideEval.loadLexicon()
    private val ev = RealSwipeEval()

    private class Ctx(val dec: GlideDecoder, val resc: ShapeRescorer)
    private class Row(val word: String, val key: String, val c: List<GlideCandidate>, val everyday: Boolean)

    @Test fun guardOnRealSwipes() {
        assumeTrue("set REAL_SWIPES=1 REAL_RARE=1", System.getenv("REAL_SWIPES") == "1" && System.getenv("REAL_RARE") == "1")
        val f = File(System.getenv("REAL_SWIPES_FILE") ?: "build/real-swipes-A.jsonl")
        val sw = RealSwipe.read(f, System.getenv("REAL_SWIPES_N")?.toIntOrNull() ?: Int.MAX_VALUE)
        val fit = ev.calibrate(sw); val its = ev.items(sw, fit)
        val weights = File("../ime/assets/glide/swipe_encoder.bin").readBytes()
        val net = ShapeNet.load(weights)
        val top = (0 until lex.size).sortedByDescending { lex.logProb(it) }.take(5000).toHashSet()
        val threads = (System.getenv("REAL_THREADS")?.toIntOrNull() ?: Runtime.getRuntime().availableProcessors()).coerceAtLeast(1)
        val pool = Executors.newFixedThreadPool(threads)
        val local = ThreadLocal.withInitial { Ctx(GlideDecoder(keys, GlideParams.forPitchDp(keys.pitchDp)), ShapeRescorer(ShapeNet.load(weights), keys)) }
        val t0 = System.nanoTime()
        val jobs = its.map { item -> pool.submit(Callable<Row?> {
            val c = local.get()
            val cs = c.dec.decode(item.xs, item.ys, item.xs.size, listOf(lex), item.prev, 40)
            if (cs.isEmpty()) return@Callable null
            val o = if (item.ts.size >= item.xs.size) c.resc.rescore(item.xs, item.ys, item.ts, item.xs.size, cs) else null
            Row(item.word, item.key, (o?.candidates ?: cs).take(5), item.lexId in top)
        }) }
        val rows = jobs.mapNotNull { it.get() }; pool.shutdown()
        println("RAREGUARD ${rows.size} swipes through decoder + network in %.0f s (%d threads) from ${f.name}".format((System.nanoTime() - t0) / 1e9, threads))
        fun ok(c: GlideCandidate, r: Row) = String(GlideLetters.keyOf(c.text)) == r.key
        for (scope in listOf("all", "everyday")) {
            val rs = if (scope == "all") rows else rows.filter { it.everyday }
            println("RAREGUARD $scope n=${rs.size} shipped first=%.2f%% top3=%.2f%%".format(100.0 * rs.count { ok(it.c[0], it) } / rs.size, 100.0 * rs.count { r -> r.c.take(3).any { ok(it, r) } } / rs.size))
            for (g in listOf(4f, 5f, 6f, 7f)) {
                val line = StringBuilder("RAREGUARD $scope G=$g:")
                for (m in listOf(1f, 2f, 3f, 5f, 8f, 12f, 20f)) {
                    var mended = 0; var spoiled = 0; var flips = 0
                    for (r in rs) {
                        val g2 = CommonGuard.apply(r.c, g, m)
                        if (g2[0] !== r.c[0]) { flips++; if (ok(g2[0], r)) { if (!ok(r.c[0], r)) mended++ } else if (ok(r.c[0], r)) spoiled++ }
                    }
                    line.append("  M=$m flips=$flips +$mended -$spoiled (net ${mended - spoiled})")
                }
                println(line)
            }
        }
        // the owner's words: what the shipped pipeline gives first, and what the chosen guard gives
        for (w in listOf("and", "for", "the", "or", "to", "of")) {
            val rs = rows.filter { it.word == w }; if (rs.isEmpty()) continue
            val before = rs.count { ok(it.c[0], it) }; val after = rs.count { ok(CommonGuard.apply(it.c)[0], it) }
            println("RAREGUARD word '$w': n=${rs.size} first before=%.1f%% after the guard=%.1f%%".format(100.0 * before / rs.size, 100.0 * after / rs.size))
        }
    }
}
