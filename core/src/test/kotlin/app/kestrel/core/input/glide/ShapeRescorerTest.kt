package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.Executor

/** R30: the trained network as a second opinion ([ShapeRescorer]) and its place in the engine ([GlideTyping.shapeModel]). Uses the weights that ship in the app. */
class ShapeRescorerTest {
    private val keys = GlideEval.keysFor()
    private val weights: ByteArray by lazy {
        listOf("../ime/src/main/assets/glide/swipe_encoder.bin", "ime/src/main/assets/glide/swipe_encoder.bin", "../geckoapp/src/main/assets/glide/swipe_encoder.bin", "geckoapp/src/main/assets/glide/swipe_encoder.bin").map { File(it) }.first { it.isFile }.readBytes()      // (the Kestrel browser keeps the weights in its geckoapp assets: this file is shared with it)
    }

    /** A path through the centres of [word]'s letters: [per] points on each stroke, [dt] ms apart. */
    private fun path(word: String, per: Int = 10, dt: Float = 9f): Triple<FloatArray, FloatArray, FloatArray> {
        val xs = ArrayList<Float>(); val ys = ArrayList<Float>()
        for (i in 0 until word.length - 1) {
            val a = word[i] - 'a'; val b = word[i + 1] - 'a'
            for (s in 0 until per) { val t = s / per.toFloat(); xs.add(keys.cx[a] + (keys.cx[b] - keys.cx[a]) * t); ys.add(keys.cy[a] + (keys.cy[b] - keys.cy[a]) * t) }
        }
        val z = word.last() - 'a'; xs.add(keys.cx[z]); ys.add(keys.cy[z])
        return Triple(xs.toFloatArray(), ys.toFloatArray(), FloatArray(xs.size) { 1_000f + it * dt })
    }

    private fun c(text: String, score: Float) = GlideCandidate(text, score, 0f, 0f, text.length)

    @Test fun aClearlyAheadShippedAnswerIsLeftAlone() {
        val r = ShapeRescorer(ShapeNet.load(weights), keys)
        val (xs, ys, ts) = path("the")
        val list = listOf(c("the", 10f), c("cat", 2f))                    // margin 8 > gate
        val o = r.rescore(xs, ys, ts, xs.size, list)
        assertFalse(o.ran); assertSame(list, o.candidates); assertFalse(o.changedFirst)
    }

    @Test fun theNetworkPutsTheWordThePathDrewFirstWhenTheShippedScoresAreClose() {
        val r = ShapeRescorer(ShapeNet.load(weights), keys)
        val (xs, ys, ts) = path("the")
        val o = r.rescore(xs, ys, ts, xs.size, listOf(c("cat", 1.0f), c("the", 0.9f), c("why", 0.5f)))
        assertTrue(o.ran)
        assertEquals("the", o.candidates[0].text)
        assertTrue(o.changedFirst)
        assertEquals(3, o.candidates.size)
        // every candidate score is the shipped one plus the weighted network term (a log-probability, so never above it)
        for (x in o.candidates) assertTrue(x.score <= 1.0f + 1e-3f)
    }

    @Test fun noTimingOrTooFewPointsMeansNoChange() {
        val r = ShapeRescorer(ShapeNet.load(weights), keys)
        val (xs, ys, _) = path("the")
        val list = listOf(c("cat", 1.0f), c("the", 0.9f))
        assertFalse(r.rescore(xs, ys, FloatArray(0), xs.size, list).ran)
        assertFalse(r.rescore(xs, ys, FloatArray(xs.size) { 5f }, xs.size, list).ran)          // all touches at one instant: no duration
        assertFalse(r.rescore(xs, ys, FloatArray(xs.size) { it * 9f }, 3, list).ran)            // 3 points
        assertFalse(r.rescore(xs, ys, FloatArray(xs.size) { it * 9f }, xs.size, listOf(c("the", 1f))).ran)   // one word: nothing to rank
    }

    @Test fun theTimeOriginDoesNotMatter() {
        val r = ShapeRescorer(ShapeNet.load(weights), keys)
        val (xs, ys, ts) = path("hello")
        val list = listOf(c("jello", 1.0f), c("hello", 0.95f))
        val a = r.rescore(xs, ys, ts, xs.size, list)
        val b = r.rescore(xs, ys, FloatArray(ts.size) { ts[it] + 123_456f }, xs.size, list)
        assertEquals(a.candidates.map { it.text }, b.candidates.map { it.text })
        assertEquals(a.candidates[0].score, b.candidates[0].score, 1e-3f)
    }

    private fun engine(shape: ByteArray?): GlideTyping {
        val bytes = GlideEval.findAsset().readBytes()
        return GlideTyping({ bytes }, Executor { it.run() }, savePersonal = { }, loadShape = { shape }).also { it.prepare(keys, null, null) }
    }

    @Test fun theEngineAsksTheNetworkOnlyWhenTheSwitchIsOnAndSaysSoInDiagnostics() {
        val e = engine(weights)
        val (xs, ys, ts) = path("hello")
        var res: GlideResult? = null
        e.decode(xs, ys, xs.size, null, null, ts) { res = it }                // switch off (the default): the shipped decoder alone
        assertFalse(e.stats.line().contains("shape model"))
        e.shapeModel = true
        // a path that is two words at once (several close scores) gets asked about: draw a few and require that at least one was
        for (w in listOf("hello", "there", "which", "would", "those")) { val p = path(w); e.decode(p.first, p.second, p.first.size, null, null, p.third) { res = it } }
        assertTrue(res!!.candidates.isNotEmpty())
        val line = e.stats.line()
        assertTrue(line, line.contains("shape model loaded"))
        assertFalse(line, line.contains("SWITCHED ITSELF OFF"))
    }

    @Test fun aMissingWeightsFileJustMeansTheOldDecoder() {
        val e = engine(null)
        e.shapeModel = true
        val (xs, ys, ts) = path("hello")
        var res: GlideResult? = null
        e.decode(xs, ys, xs.size, null, null, ts) { res = it }
        assertTrue(res!!.candidates.isNotEmpty())
        assertFalse(e.stats.line().contains("shape model"))
    }

    @Test fun anUntimedGlideIsDecodedByTheShippedDecoderAlone() {
        val e = engine(weights); e.shapeModel = true
        val (xs, ys, _) = path("hello")
        var res: GlideResult? = null
        e.decode(xs, ys, xs.size, null, null) { res = it }
        assertEquals("hello", res!!.candidates[0].text)
        assertFalse(e.stats.line().contains("shape model loaded"))
    }
}
