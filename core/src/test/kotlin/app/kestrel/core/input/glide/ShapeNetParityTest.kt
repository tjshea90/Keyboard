package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * R30: the Kotlin forward pass against the PyTorch reference the upstream author checked in (`swipe_reference.bin`: xy, keys, features, log-emissions, gate from seed 0).
 * Reads the weights that ship in the app and the committed reference numbers, so it runs in every build (a missing file FAILS: the shipped network must never go unchecked).
 */
class ShapeNetParityTest {
    /** The shipped weights (the app's assets) and the upstream reference numbers (test resources); core/build/r30 (R30FetchTest) is a fallback. */
    private fun first(vararg names: String): File? = names.map { File(it) }.firstOrNull { it.isFile }
    private fun files(): Pair<File, File>? {
        val w = first("../ime/src/main/assets/glide/swipe_encoder.bin", "ime/src/main/assets/glide/swipe_encoder.bin", "../geckoapp/src/main/assets/glide/swipe_encoder.bin", "geckoapp/src/main/assets/glide/swipe_encoder.bin", "build/r30/swipe_encoder.bin")
        val r = first("src/test/resources/swipe_reference.bin", "core/src/test/resources/swipe_reference.bin", "build/r30/swipe_reference.bin")
        return if (w != null && r != null) w to r else null
    }

    private fun readRef(f: File): List<FloatArray> {
        val b = ByteBuffer.wrap(f.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals("SWRF", String(ByteArray(4).also { b.get(it) }))
        assertEquals(1, b.int)
        val out = ArrayList<FloatArray>()
        repeat(5) { val n = b.int; val a = FloatArray(n); b.asFloatBuffer().get(a); b.position(b.position() + 4 * n); out.add(a) }
        return out
    }

    @Test fun kotlinMatchesTorch() {
        val (wf, rf) = files() ?: error("swipe_encoder.bin / swipe_reference.bin not found")
        val net = ShapeNet.load(wf.readBytes())
        val (xy, keys, feats, logp, gate) = readRef(rf).let { listOf(it[0], it[1], it[2], it[3], it[4]) }
        val f = net.features(xy)
        var df = 0f; for (i in f.indices) df = maxOf(df, abs(f[i] - feats[i]))
        val t0 = System.nanoTime()
        val em = net.emissions(xy, keys, 26)
        val ms = (System.nanoTime() - t0) / 1e6
        var de = 0f; for (i in em.indices) de = maxOf(de, abs(em[i] - logp[i]))
        println("R30 parity: features max|diff| = $df, log-emissions max|diff| = $de over ${em.size} values; one forward pass ${"%.1f".format(ms)} ms (cold JIT)")
        assertTrue("features differ by $df", df < 1e-3f)
        assertTrue("log-emissions differ by $de", de < 2e-3f)
        // every frame's probabilities sum to one
        for (t in 0 until ShapeNet.T_OUT) { var s = 0.0; for (k in 0..26) s += Math.exp(em[t * 27 + k].toDouble()); assertEquals(1.0, s, 1e-3) }
    }

    @Test fun ctcPrefersTheRightWord() {
        // a hand-made emission table: frames favour letters 0,1,2 in order, blank in between
        val k = 3; val em = FloatArray(ShapeNet.T_OUT * (k + 1)) { Math.log(0.01).toFloat() }
        for (t in 0 until ShapeNet.T_OUT) { val want = if (t < 10) 0 else if (t < 20) 1 else 2; em[t * (k + 1) + want] = Math.log(0.97).toFloat() }
        val good = ShapeNet.ctcLogP(em, k, intArrayOf(0, 1, 2)); val bad = ShapeNet.ctcLogP(em, k, intArrayOf(0, 2, 1))
        assertTrue("good $good bad $bad", good > bad + 5f)
        // repeated letters need a blank between them
        assertTrue(ShapeNet.ctcLogP(em, k, intArrayOf(0, 0)) < good)
    }
}
