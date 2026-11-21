package app.kestrel.core.input.glide

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * R30 step 3: does the OPEN trained swipe encoder ([ShapeNet]) beat, or help, the SHIPPED decoder on REAL swipes?
 * Skipped unless REAL_SWIPES=1 and core/build/r30/swipe_encoder.bin exists (R30_FETCH=1 R30FetchTest downloads it). Test-side only: nothing is committed, the data stays in core/build.
 *
 * For every real swipe: the shipped decoder gives its top [CAND] words (score = higher is better); the network gives log P(word | path) by the CTC forward sum.
 * Hybrid = shipped score + w * log P. Reported: oracle recall of the candidate list, shipped, net alone (rank by log P), hybrid for a grid of w.
 * Two ways to hand the path to the net:  A = the corpus' own 0..1 canvas (key centres mapped back with the calibration fit), B = THIS keyboard's area (x / width, y / height: what the IME would do).
 * Slice A (core/build/real-swipes.jsonl, first 8 MB of dev.jsonl) picks w; slice B (a later byte range = other donors) is only read at the chosen w.
 */
class ShapeNetEvalTest {
    private val keys = GlideEval.keysFor()
    private val lex = GlideEval.loadLexicon()
    private val ev = RealSwipeEval()
    private val cand = System.getenv("R30_CAND")?.toIntOrNull() ?: 40
    private val areaW = 292f; private val areaH = 161f      // GlideEval.keysFor() defaults

    class Row(val score: FloatArray, val hit: BooleanArray, val len: IntArray, val ctcA: FloatArray, val ctcB: FloatArray, val meantLen: Int)

    private class Ctx(val net: ShapeNet, val dec: GlideDecoder)

    private fun letters(t: String) = IntArray(t.count { it in 'a'..'z' }).also { var i = 0; for (c in t) if (c in 'a'..'z') it[i++] = c - 'a' }

    private fun rows(its: List<RealSwipeEval.Item>, fit: RealSwipeEval.Fit, weights: ByteArray, label: String): List<Row?> {
        val threads = (System.getenv("REAL_THREADS")?.toIntOrNull() ?: Runtime.getRuntime().availableProcessors()).coerceAtLeast(1)
        val pool = Executors.newFixedThreadPool(threads)
        val local = ThreadLocal.withInitial { Ctx(ShapeNet.load(weights), GlideDecoder(keys, GlideParams.forPitchDp(keys.pitchDp))) }
        // key centres in the two coordinate systems (x then y interleaved)
        val kA = FloatArray(52); val kB = FloatArray(52)
        for (k in 0 until 26) { kA[2 * k] = (keys.cx[k] - fit.bx) / fit.ax; kA[2 * k + 1] = (keys.cy[k] - fit.by) / fit.ay; kB[2 * k] = keys.cx[k] / areaW; kB[2 * k + 1] = keys.cy[k] / areaH }
        val t0 = System.nanoTime()
        val jobs = its.map { item -> pool.submit(Callable<Row?> {
            val c = local.get()
            val cs = c.dec.decode(item.xs, item.ys, item.xs.size, listOf(lex), item.prev, cand)
            if (cs.isEmpty() || item.ts.size < 2) return@Callable null
            val n = item.xs.size
            val ctc = Array(2) { FloatArray(cs.size) { -1e9f } }
            for (mode in 0..1) {
                val xs = FloatArray(n) { i -> if (mode == 0) (item.xs[i] - fit.bx) / fit.ax else item.xs[i] / areaW }
                val ys = FloatArray(n) { i -> if (mode == 0) (item.ys[i] - fit.by) / fit.ay else item.ys[i] / areaH }
                val tr = ShapeNet.trajectory(xs, ys, item.ts, n) ?: continue
                val em = c.net.emissions(tr, if (mode == 0) kA else kB, 26)
                for ((i, x) in cs.withIndex()) { val w = letters(x.text); if (w.isNotEmpty()) ctc[mode][i] = ShapeNet.ctcLogP(em, 26, w) }
            }
            Row(FloatArray(cs.size) { i -> cs[i].score }, BooleanArray(cs.size) { i -> String(GlideLetters.keyOf(cs[i].text)) == item.key }, IntArray(cs.size) { i -> cs[i].text.length }, ctc[0], ctc[1], item.word.length)
        }) }
        val out = jobs.map { it.get() }
        pool.shutdown()
        println("R30 $label: ${out.count { it != null }} swipes through decoder + net in %.1f s (%d threads, candidates %d)".format((System.nanoTime() - t0) / 1e9, threads, cand))
        return out
    }

    class Cnt(var n: Int = 0, var t1: Int = 0, var t3: Int = 0)

    /** mode: 0 = shipped only, 1 = net A, 2 = net B; w: weight of the log P (null = rank by the net alone); perLetter: log P / letters. */
    private fun rank(rs: List<Row?>, mode: Int, w: Float?, perLetter: Boolean): Cnt {
        val c = Cnt()
        for (r in rs) {
            if (r == null) continue
            c.n++
            val m = r.score.size
            val s = FloatArray(m) { i ->
                val l = if (mode == 1) r.ctcA[i] else if (mode == 2) r.ctcB[i] else 0f
                val lp = if (perLetter) l / r.len[i].coerceAtLeast(1) else l
                if (mode == 0) r.score[i] else if (w == null) lp else r.score[i] + w * lp
            }
            val order = (0 until m).sortedByDescending { s[it] }
            if (r.hit[order[0]]) c.t1++
            if (order.take(3).any { r.hit[it] }) c.t3++
        }
        return c
    }

    private fun best(r: Row, mode: Int, w: Float): Int {
        val m = r.score.size
        var bi = 0; var bs = Float.NEGATIVE_INFINITY
        for (i in 0 until m) { val v = if (mode == 0) r.score[i] else r.score[i] + w * (if (mode == 1) r.ctcA[i] else r.ctcB[i]); if (v > bs) { bs = v; bi = i } }
        return bi
    }

    /** First choice a word 2+ letters LONGER than the short (<= 4 letters) word meant: the owner's the -> there, and -> access, or -> our. */
    private fun s2l(rs: List<Row?>, mode: Int, w: Float): Int { var n = 0; for (r in rs) { if (r == null || r.meantLen > 4) continue; val b = best(r, mode, w); if (!r.hit[b] && r.len[b] - r.meantLen >= 2) n++ }; return n }
    private fun shortN(rs: List<Row?>) = rs.count { it != null && it.meantLen <= 4 }
    private fun shortHit(rs: List<Row?>, mode: Int, w: Float) = rs.count { r -> r != null && r.meantLen <= 4 && r.hit[best(r, mode, w)] }

    /** Shipped margin = best score minus second best (the shipped list is sorted best first). Use the net only where the shipped answer is NOT clearly ahead. */
    private fun gate(label: String, rs: List<Row?>, w: Float) {
        val ok = rs.filterNotNull().filter { it.score.size >= 2 }
        val margins = ok.map { r -> val sorted = r.score.sortedDescending(); sorted[0] - sorted[1] }.sorted()
        val sb = StringBuilder("R30 [$label] confidence gate (net only where shipped margin < threshold; w=$w, B form): ")
        for (q in listOf(0.1, 0.25, 0.5, 0.75, 1.0)) {
            val th = if (q >= 1.0) Float.MAX_VALUE else margins[((margins.size - 1) * q).toInt()]
            var hit = 0
            for (r in ok) { val sorted = r.score.sortedDescending(); val useNet = (sorted[0] - sorted[1]) < th; if (r.hit[best(r, if (useNet) 2 else 0, w)]) hit++ }
            sb.append("| net on %.0f%% of swipes: top1 %.1f%% ".format(q * 100, 100.0 * hit / ok.size))
        }
        println(sb.toString())
    }

    private fun fmt(c: Cnt) = "top1 %.1f%% top3 %.1f%%".format(100.0 * c.t1 / c.n, 100.0 * c.t3 / c.n)

    private fun load(name: String, fileEnv: String, def: String, mb: Long, offsetMb: Long, url: String = RealSwipe.DEV_URL): List<RealSwipe.Swipe> {
        val f = File(System.getenv(fileEnv) ?: def)
        if (!f.exists()) println("fetched " + RealSwipe.fetch(url, mb * 1_000_000, f, offsetMb * 1_000_000) + " bytes for $name")
        return RealSwipe.read(f, System.getenv("R30_MAXN")?.toIntOrNull() ?: Int.MAX_VALUE)
    }

    private val chosenW = System.getenv("R30_W")?.toFloatOrNull() ?: 0.7f

    private fun report(label: String, rs: List<Row?>, grid: List<Float>) {
        val n = rs.count { it != null }
        val oracle = rs.count { r -> r != null && r.hit.any { it } }
        println("R30 [$label] n=$n  oracle recall of the top-$cand list %.1f%%".format(100.0 * oracle / n))
        println("R30 [$label] shipped                    ${fmt(rank(rs, 0, null, false))}")
        println("R30 [$label] net A alone (canvas)       ${fmt(rank(rs, 1, null, false))}   per letter ${fmt(rank(rs, 1, null, true))}")
        println("R30 [$label] net B alone (our area)     ${fmt(rank(rs, 2, null, false))}   per letter ${fmt(rank(rs, 2, null, true))}")
        val sn = shortN(rs)
        println("R30 [$label] CHOSEN w=$chosenW: shipped ${fmt(rank(rs, 0, null, false))} | hybrid A ${fmt(rank(rs, 1, chosenW, false))} | hybrid B (keyboard area) ${fmt(rank(rs, 2, chosenW, false))}")
        println("R30 [$label] short words (<=4 letters, n=$sn): shipped hit %.1f%% s2l(short meant, 2+ longer chosen) %d | hybrid B hit %.1f%% s2l %d".format(100.0 * shortHit(rs, 0, 0f) / sn, s2l(rs, 0, 0f), 100.0 * shortHit(rs, 2, chosenW) / sn, s2l(rs, 2, chosenW)))
        gate(label, rs, chosenW)
        for (w in grid) println("R30 [$label] hybrid w=%-5.2f  A: %s | B: %s | A/letter: %s | B/letter: %s".format(w, fmt(rank(rs, 1, w, false)), fmt(rank(rs, 2, w, false)), fmt(rank(rs, 1, w, true)), fmt(rank(rs, 2, w, true))))
    }

    @Test fun netAgainstShipped() {
        assumeTrue("set REAL_SWIPES=1", System.getenv("REAL_SWIPES") == "1")
        val wf = File("build/r30/swipe_encoder.bin")
        assumeTrue("no build/r30/swipe_encoder.bin (R30_FETCH=1 R30FetchTest)", wf.exists())
        val weights = wf.readBytes()
        val sw = load("slice A", "REAL_SWIPES_FILE", "build/real-swipes.jsonl", 8, 0)
        val fit = ev.calibrate(sw)
        val its = ev.items(sw, fit)
        println("R30 slice A: ${sw.size} swipes read, ${its.size} in the word list; fit x' = %.2f x + %.2f, y' = %.2f y + %.2f".format(fit.ax, fit.bx, fit.ay, fit.by))
        val rs = rows(its, fit, weights, "slice A")
        val grid = (System.getenv("R30_GRID") ?: "0.05,0.1,0.2,0.35,0.5,1,2,4").split(',').map { it.toFloat() }
        report("A", rs, grid)

        // timing: one thread, warm
        val net = ShapeNet.load(weights)
        val kA = FloatArray(52) { k -> if (k % 2 == 0) (keys.cx[k / 2] - fit.bx) / fit.ax else (keys.cy[k / 2] - fit.by) / fit.ay }
        val few = its.take(60)
        var ms = 0.0; var cnt = 0
        for ((j, item) in few.withIndex()) {
            val xs = FloatArray(item.xs.size) { i -> (item.xs[i] - fit.bx) / fit.ax }; val ys = FloatArray(item.xs.size) { i -> (item.ys[i] - fit.by) / fit.ay }
            val tr = ShapeNet.trajectory(xs, ys, item.ts, item.xs.size) ?: continue
            val t = System.nanoTime(); net.emissions(tr, kA, 26); val d = (System.nanoTime() - t) / 1e6
            if (j >= 10) { ms += d; cnt++ }
        }
        println("R30 warm forward on this JVM, one thread: %.1f ms per swipe (n=%d)".format(ms / cnt.coerceAtLeast(1), cnt))

        if (System.getenv("R30_C") != "0") {
            // the FUTO TEST split: the cleanest held-out (the network's checkpoint may have been picked on dev)
            try {
                val sc = load("slice C (test split)", "R30_C_FILE", "build/real-swipes-c.jsonl", 12, 0, System.getenv("R30_C_URL") ?: RealSwipe.DEV_URL.replace("dev.jsonl", "test.jsonl"))
                val fitC = ev.calibrate(sc); val itsC = ev.items(sc, fitC)
                println("R30 slice C (test split): ${sc.size} swipes read, ${itsC.size} in the word list")
                report("C", rows(itsC, fitC, weights, "slice C"), grid)
            } catch (e: Exception) { println("R30 slice C failed: $e") }
        }
        if (System.getenv("R30_B") != "0") {
            val sb = load("slice B", "R30_B_FILE", "build/real-swipes-b.jsonl", 12, System.getenv("R30_B_OFFSET_MB")?.toLongOrNull() ?: 100L)
            val fitB = ev.calibrate(sb)
            val itsB = ev.items(sb, fitB)
            println("R30 slice B: ${sb.size} swipes read, ${itsB.size} in the word list; fit x' = %.2f x + %.2f, y' = %.2f y + %.2f".format(fitB.ax, fitB.bx, fitB.ay, fitB.by))
            val rb = rows(itsB, fitB, weights, "slice B")
            report("B", rb, grid)
        }
    }
}
