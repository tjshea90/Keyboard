package app.kestrel.core.input.glide

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * R30: a TRAINED shape model for glide typing, run in plain Kotlin (kotlin-stdlib only, no Android, no allocation per key).
 *
 * The network is the layout-agnostic swipe encoder of the FUTO Swipe paper (arXiv 2606.25247): a temporal convolution network over the resampled touch path that emits 64 cosine
 * coefficients per frame; a key's score is that spatial pattern read out at the key's centre, so ANY keyboard geometry works without retraining (our phone and tablet keys included).
 * The weights are the open, MIT-licensed `swipe_encoder.bin` of github.com/xxparthparekhxx/keyboard (Copyright (c) 2026 Parth Parekh), trained on the MIT-licensed FUTO swipe
 * corpus (swipe.futo.org). This file is our own re-implementation of the published design (see research/R30-trained-shape-model.md section 5); it carries no upstream code.
 *
 * Use: [load] the weights once; per glide [trajectory] (resample the raw points), [emissions] (the network), then [ctcLogP] for each candidate word (the log-likelihood of the
 * word's letters given the whole path). NOT thread safe (scratch buffers are reused); one instance per decoding thread.
 */
class ShapeNet private constructor(private val w: Map<String, FloatArray>) {
    companion object {
        const val T_IN = 64
        const val T_OUT = 32
        const val DIM = 128
        const val HEAD = 256
        const val N_DCT = 8
        const val N_COEFF = N_DCT * N_DCT
        const val BLOCKS = 5
        private val DILATIONS = intArrayOf(1, 2, 3, 5, 8)
        private const val NEG = -1e30f

        /** Reads the `SWEN` weight file (version 1: [i32 nameLen][name][i32 ndim][i32 dims][f32 data] per tensor, little endian). Throws on a malformed file. */
        fun load(bytes: ByteArray): ShapeNet {
            val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            require(b.remaining() >= 12 && b.get().toInt() == 'S'.code && b.get().toInt() == 'W'.code && b.get().toInt() == 'E'.code && b.get().toInt() == 'N'.code) { "not a SWEN file" }
            require(b.int == 1) { "unsupported SWEN version" }
            val n = b.int
            val m = HashMap<String, FloatArray>()
            repeat(n) {
                val nameLen = b.int; val nameBytes = ByteArray(nameLen); b.get(nameBytes)
                val ndim = b.int; var size = 1
                repeat(ndim) { size *= b.int }
                val a = FloatArray(size); b.asFloatBuffer().get(a); b.position(b.position() + 4 * size)
                m[String(nameBytes, Charsets.UTF_8)] = a
            }
            for (name in listOf("savgol", "stem.w", "stem.b", "adapter.w", "adapter.b", "coeff.w", "coeff.b", "gate.w", "gate.b")) require(m.containsKey(name)) { "missing tensor $name" }
            for (i in 0 until BLOCKS) for (s in listOf("dw.w", "dw.b", "pw1.w", "pw1.b", "grn.g", "grn.b", "pw2.w", "pw2.b", "se1.w", "se1.b", "se2.w", "se2.b")) require(m.containsKey("b$i.$s")) { "missing tensor b$i.$s" }
            require(m.getValue("coeff.w").size == N_COEFF * HEAD && m.getValue("pw1.w".let { "b0.$it" }).size == 4 * DIM * DIM) { "unexpected tensor sizes" }
            return ShapeNet(m)
        }

        /**
         * The path the network wants: [T_IN] points evenly spaced IN TIME (a 60 Hz linear pass first, then a linear pass over the whole duration), as x[0..63] then y[0..63].
         * xs / ys are the touch points in 0..1 of the key area's width / height, ts in milliseconds. Null for a path with no duration.
         */
        fun trajectory(xs: FloatArray, ys: FloatArray, ts: FloatArray, n: Int): FloatArray? {
            if (n < 2) return null
            val t0 = ts[0]; val dur = ts[n - 1] - t0
            if (!(dur > 0f)) return null
            val n60 = max((dur / 1000f * 60f).toInt() + 1, 2)
            val x60 = FloatArray(n60); val y60 = FloatArray(n60)
            var j = 0
            for (i in 0 until n60) {
                val g = dur * i / (n60 - 1)
                while (j < n - 2 && ts[j + 1] - t0 < g) j++
                val a = ts[j] - t0; val c = ts[j + 1] - t0
                val f = if (c > a) ((g - a) / (c - a)).coerceIn(0f, 1f) else 0f
                x60[i] = xs[j] + (xs[j + 1] - xs[j]) * f; y60[i] = ys[j] + (ys[j + 1] - ys[j]) * f
            }
            val out = FloatArray(2 * T_IN)
            j = 0
            for (i in 0 until T_IN) {
                val pos = (n60 - 1).toFloat() * i / (T_IN - 1)
                val k = pos.toInt().coerceAtMost(n60 - 2); val f = pos - k
                out[i] = x60[k] + (x60[k + 1] - x60[k]) * f; out[T_IN + i] = y60[k] + (y60[k + 1] - y60[k]) * f
            }
            return out
        }

        /** The log-likelihood of [word] (key ids, 0 until K) under [em] ([T_OUT] frames of K+1 log-probabilities, blank = K): the CTC forward sum over all alignments. */
        fun ctcLogP(em: FloatArray, k: Int, word: IntArray): Float {
            val l = word.size
            if (l == 0) return NEG
            val s = 2 * l + 1; val stride = k + 1
            var prev = FloatArray(s) { NEG }; var cur = FloatArray(s)
            prev[0] = em[k]; if (s > 1) prev[1] = em[word[0]]
            for (t in 1 until T_OUT) {
                val row = t * stride
                for (i in 0 until s) {
                    val lab = if (i and 1 == 0) k else word[i shr 1]
                    var a = prev[i]
                    if (i >= 1) a = logAdd(a, prev[i - 1])
                    if (i >= 2 && lab != k && lab != word[(i - 2) shr 1]) a = logAdd(a, prev[i - 2])
                    cur[i] = if (a <= NEG / 2) NEG else a + em[row + lab]
                }
                val tmp = prev; prev = cur; cur = tmp
            }
            return logAdd(prev[s - 1], if (s >= 2) prev[s - 2] else NEG)
        }

        private fun logAdd(a: Float, b: Float): Float {
            if (a <= NEG / 2) return b
            if (b <= NEG / 2) return a
            return if (a > b) a + ln(1f + exp(b - a)) else b + ln(1f + exp(a - b))
        }
    }

    // scratch (one set per instance)
    private val feat = FloatArray(8 * T_IN)
    private var h = FloatArray(T_IN * DIM); private var h2 = FloatArray(T_IN * DIM)
    private val u = FloatArray(T_IN * 4 * DIM)
    private val g = FloatArray(T_IN * 2 * DIM)
    private val coeff = FloatArray(T_OUT * N_COEFF); private val gate = FloatArray(T_OUT)

    /** The 8 x [T_IN] feature rows (channel-major): smoothed x, y, velocity x, y, acceleration x, y, speed, curvature. Exposed for the parity test. */
    fun features(xy: FloatArray): FloatArray {
        val kern = w.getValue("savgol")   // 3 x 7, already flipped for correlation
        val out = FloatArray(8 * T_IN)
        val pos = FloatArray(2 * T_IN); val vel = FloatArray(2 * T_IN); val acc = FloatArray(2 * T_IN)
        for (c in 0 until 2) {
            val base = c * T_IN
            for (t in 0 until T_IN) {
                var p = 0f; var v = 0f; var a = 0f
                for (k in 0 until 7) {
                    val idx = (t + k - 3).coerceIn(0, T_IN - 1); val x = xy[base + idx]
                    p += kern[k] * x; v += kern[7 + k] * x; a += kern[14 + k] * x
                }
                pos[base + t] = p; vel[base + t] = v; acc[base + t] = a
            }
        }
        System.arraycopy(pos, 0, out, 0, 2 * T_IN); System.arraycopy(vel, 0, out, 2 * T_IN, 2 * T_IN); System.arraycopy(acc, 0, out, 4 * T_IN, 2 * T_IN)
        var prevTheta = 0f
        for (t in 0 until T_IN) {
            val vx = vel[t]; val vy = vel[T_IN + t]
            out[6 * T_IN + t] = sqrt(vx * vx + vy * vy + 1e-8f)
            val theta = atan2(vy, vx)
            var d = if (t == 0) 0f else theta - prevTheta
            prevTheta = theta
            val two = (2 * PI).toFloat()
            d = ((d + PI.toFloat()) % two + two) % two - PI.toFloat()   // Python-style remainder into (-pi, pi]
            out[7 * T_IN + t] = d.coerceIn(-2f, 2f)
        }
        return out
    }

    /**
     * The network: [xy] (x[0..63], y[0..63]) and [keys] (K pairs x, y in 0..1 of the same area) -> [T_OUT] x (K+1) log-probabilities (letters = key ids, blank = K).
     * Returns a new array of T_OUT * (K + 1) floats.
     */
    fun emissions(xy: FloatArray, keys: FloatArray, k: Int): FloatArray {
        val f = features(xy)
        System.arraycopy(f, 0, feat, 0, f.size)
        // stem: conv1d 8 -> DIM, kernel 5, pad 2; result h[t][c]
        val sw = w.getValue("stem.w"); val sb = w.getValue("stem.b")
        for (t in 0 until T_IN) for (o in 0 until DIM) {
            var s = sb[o]
            for (i in 0 until 8) for (kk in 0 until 5) {
                val tt = t + kk - 2; if (tt < 0 || tt >= T_IN) continue
                s += sw[(o * 8 + i) * 5 + kk] * feat[i * T_IN + tt]
            }
            h[t * DIM + o] = s
        }
        for (bi in 0 until BLOCKS) block(bi, DILATIONS[bi])
        // adapter: conv1d DIM -> HEAD, kernel 2, stride 2 (+ folded BN): T_IN -> T_OUT
        val aw = w.getValue("adapter.w"); val ab = w.getValue("adapter.b")
        val cw = w.getValue("coeff.w"); val cb = w.getValue("coeff.b"); val gw = w.getValue("gate.w"); val gb = w.getValue("gate.b")
        val a = FloatArray(HEAD)
        for (t in 0 until T_OUT) {
            for (o in 0 until HEAD) {
                var s = ab[o]
                val wo = o * DIM * 2
                for (i in 0 until DIM) s += aw[wo + i * 2] * h[(2 * t) * DIM + i] + aw[wo + i * 2 + 1] * h[(2 * t + 1) * DIM + i]
                a[o] = s
            }
            for (c in 0 until N_COEFF) { var s = cb[c]; val wo = c * HEAD; for (i in 0 until HEAD) s += cw[wo + i] * a[i]; coeff[t * N_COEFF + c] = s }
            var s = gb[0]; for (i in 0 until HEAD) s += gw[i] * a[i]; gate[t] = s
        }
        // spatial read-out: phi[k][u * 8 + v] = cos(pi u x_k) cos(pi v y_k)
        val phi = FloatArray(k * N_COEFF)
        for (kk in 0 until k) for (uu in 0 until N_DCT) {
            val cx = cos(PI * keys[2 * kk] * uu).toFloat()
            for (vv in 0 until N_DCT) phi[kk * N_COEFF + uu * N_DCT + vv] = cx * cos(PI * keys[2 * kk + 1] * vv).toFloat()
        }
        val out = FloatArray(T_OUT * (k + 1)); val z = FloatArray(k)
        for (t in 0 until T_OUT) {
            var mx = -1e30f
            for (kk in 0 until k) { var s = 0f; val po = kk * N_COEFF; val co = t * N_COEFF; for (c in 0 until N_COEFF) s += coeff[co + c] * phi[po + c]; z[kk] = s; if (s > mx) mx = s }
            var sum = 0f; for (kk in 0 until k) sum += exp(z[kk] - mx)
            val lse = mx + ln(sum)
            val gl = gate[t]
            val logLam = logSigmoid(gl); val logBlank = logSigmoid(-gl)
            for (kk in 0 until k) out[t * (k + 1) + kk] = z[kk] - lse + logLam
            out[t * (k + 1) + k] = logBlank
        }
        return out
    }

    private fun logSigmoid(x: Float): Float = if (x >= 0f) -ln(1f + exp(-x)) else x - ln(1f + exp(x))
    private fun sigmoid(x: Float): Float = 1f / (1f + exp(-x))

    private fun block(i: Int, d: Int) {
        val dww = w.getValue("b$i.dw.w"); val dwb = w.getValue("b$i.dw.b")
        val p1w = w.getValue("b$i.pw1.w"); val p1b = w.getValue("b$i.pw1.b")
        val gg = w.getValue("b$i.grn.g"); val gbeta = w.getValue("b$i.grn.b")
        val p2w = w.getValue("b$i.pw2.w"); val p2b = w.getValue("b$i.pw2.b")
        val s1w = w.getValue("b$i.se1.w"); val s1b = w.getValue("b$i.se1.b"); val s2w = w.getValue("b$i.se2.w"); val s2b = w.getValue("b$i.se2.b")
        val hid = 4 * DIM; val half = hid / 2   // 512 -> GLU -> 256
        // depthwise conv (kernel 7, dilation d, zero pad) into h2
        for (t in 0 until T_IN) for (c in 0 until DIM) {
            var s = dwb[c]
            for (kk in 0 until 7) { val tt = t + (kk - 3) * d; if (tt < 0 || tt >= T_IN) continue; s += dww[c * 7 + kk] * h[tt * DIM + c] }
            h2[t * DIM + c] = s
        }
        // pw1 (DIM -> 512) + GLU -> g[t][half]
        for (t in 0 until T_IN) {
            val xo = t * DIM
            for (o in 0 until half) {
                var a = p1b[o]; var b = p1b[half + o]
                val wa = o * DIM; val wb = (half + o) * DIM
                for (c in 0 until DIM) { val x = h2[xo + c]; a += p1w[wa + c] * x; b += p1w[wb + c] * x }
                g[t * half + o] = a * sigmoid(b)
            }
        }
        // GRN over time
        val gx = FloatArray(half)
        var mean = 0f
        for (c in 0 until half) { var s = 0f; for (t in 0 until T_IN) { val v = g[t * half + c]; s += v * v }; gx[c] = sqrt(s); mean += gx[c] }
        mean /= half
        for (c in 0 until half) { val nx = gx[c] / (mean + 1e-6f); for (t in 0 until T_IN) { val v = g[t * half + c]; g[t * half + c] = gg[c] * (v * nx) + gbeta[c] + v } }
        // pw2 (256 -> DIM) into u (reused as [t][DIM])
        for (t in 0 until T_IN) for (o in 0 until DIM) {
            var s = p2b[o]; val wo = o * half
            for (c in 0 until half) s += p2w[wo + c] * g[t * half + c]
            u[t * DIM + o] = s
        }
        // squeeze-excite
        val sq = FloatArray(DIM)
        for (c in 0 until DIM) { var s = 0f; for (t in 0 until T_IN) s += u[t * DIM + c]; sq[c] = s / T_IN }
        val se = 32; val mid = FloatArray(se)
        for (o in 0 until se) { var s = s1b[o]; for (c in 0 until DIM) s += s1w[o * DIM + c] * sq[c]; mid[o] = max(s, 0f) }
        val scale = FloatArray(DIM)
        for (c in 0 until DIM) { var s = s2b[c]; for (o in 0 until se) s += s2w[c * se + o] * mid[o]; scale[c] = sigmoid(s) }
        // residual
        for (t in 0 until T_IN) for (c in 0 until DIM) h[t * DIM + c] += u[t * DIM + c] * scale[c]
    }
}
