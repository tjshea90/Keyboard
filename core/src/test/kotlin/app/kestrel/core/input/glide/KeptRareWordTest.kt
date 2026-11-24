package app.kestrel.core.input.glide

import app.kestrel.core.input.Key
import app.kestrel.core.input.editor.Rig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import java.util.concurrent.Executor

/**
 * R33 (owner 2026-11-24: "it is still typing ave instead of and a lot"): with the REAL word list, a path halfway between "and" and "ave" gives "and" - until the keyboard has learned "ave" the way a kept glide used
 * to teach it (one full use made the first choice flip). Now a kept glide of a word that rare teaches only a light lesson, a correction takes the wrong word back, and a rare word's boost has a ceiling.
 */
class KeptRareWordTest {
    private val keys = GlideEval.keysFor(564f, 272f)
    private val none = SimNoise(0f, 0f, 0f, 0f, 0f, "none")
    private var clock = 1_000L

    private fun resample(p: GestureSim.Path, n: Int): Pair<FloatArray, FloatArray> {
        val d = FloatArray(p.xs.size); for (i in 1 until p.xs.size) d[i] = d[i - 1] + Math.hypot((p.xs[i] - p.xs[i - 1]).toDouble(), (p.ys[i] - p.ys[i - 1]).toDouble()).toFloat()
        val xs = FloatArray(n); val ys = FloatArray(n); var j = 1
        for (i in 0 until n) { val t = d.last() * i / (n - 1); while (j < d.size - 1 && d[j] < t) j++; val a = d[j - 1]; val b = d[j]; val f = if (b > a) (t - a) / (b - a) else 0f; xs[i] = p.xs[j - 1] + f * (p.xs[j] - p.xs[j - 1]); ys[i] = p.ys[j - 1] + f * (p.ys[j] - p.ys[j - 1]) }
        return xs to ys
    }

    /** A path 65 % of the way from the clean "and" to the clean "ave": with nothing learned "and" wins, by the language alone. */
    private fun halfway(): Pair<FloatArray, FloatArray> {
        val sim = GestureSim(keys, Random(1))
        val a = resample(sim.path("and", none)!!, 60); val v = resample(sim.path("ave", none)!!, 60)
        return FloatArray(60) { 0.35f * a.first[it] + 0.65f * v.first[it] } to FloatArray(60) { 0.35f * a.second[it] + 0.65f * v.second[it] }
    }

    private inner class Env {
        val rig = Rig("")
        val engine = GlideTyping({ GlideEval.findAsset().readBytes() }, Executor { it.run() })
        val s = GlideSession(rig.c, { engine }, { clock })
        val sim = GestureSim(keys, Random(5))
        init { engine.prepare(keys, null, null); assertTrue(engine.ready); s.ui = object : GlideSession.Ui { override fun showCandidates(words: List<String>) {}; override fun clearCandidates() {}; override fun showNoWord() {} } }
        fun glide(word: String) {
            val p = sim.path(word, none)!!
            val (prev2, prev) = s.context(); s.glideLifted(); s.beforeKey(Key.Ch("~glide"))
            var res: GlideResult? = null
            engine.decode(p.xs, p.ys, p.xs.size, prev, prev2) { res = it }
            s.onDecoded(res!!, prev, prev2); rig.sync(); rig.check()
        }
        fun glidePath(xs: FloatArray, ys: FloatArray) {
            val (prev2, prev) = s.context(); s.glideLifted(); s.beforeKey(Key.Ch("~glide"))
            var res: GlideResult? = null
            engine.decode(xs, ys, xs.size, prev, prev2) { res = it }
            s.onDecoded(res!!, prev, prev2); rig.sync(); rig.check()
        }
        fun space() { s.beforeKey(Key.Ch(" ")); rig.c.type(" "); rig.sync(); s.afterKey(Key.Ch(" ")); rig.check() }
        fun firstFor(xs: FloatArray, ys: FloatArray): String { var r: GlideResult? = null; engine.decode(xs, ys, xs.size, null, null) { r = it }; return r!!.candidates.first().text }
    }

    @Test fun theOldLessonFlippedThePathToAveAndOneKeptGlideNowDoesNot() {
        val (xs, ys) = halfway()
        val lex = GlideEval.loadLexicon()
        val old = PersonalModel(); old.add(null, "ave", 1f)                       // the old teaching, exactly: a kept "ave" was one full use
        val dec = GlideDecoder(keys, GlideParams.forPitchDp(keys.pitch)); dec.personal = null
        assertEquals("nothing learned: the language decides", "and", dec.decode(xs, ys, xs.size, listOf<GlideLexicon>(lex), null, 3)[0].text)
        dec.personal = old
        assertEquals("one full use of ave (the old lesson) flips it: this is the loop", "ave", dec.decode(xs, ys, xs.size, listOf<GlideLexicon>(lex), null, 3)[0].text)
        val e = Env()
        assertEquals("and", e.firstFor(xs, ys))
        e.glide("ave"); e.space()                                                  // one ave glide that is only kept
        assertEquals("a kept glide of a rare word teaches only a light lesson: the halfway path is still and", "and", e.firstFor(xs, ys))
    }

    @Test fun correctingAveToAndHealsDamageAnEarlierVersionLearned() {
        val (xs, ys) = halfway()
        val e = Env()
        repeat(6) { e.engine.learn(null, "ave", 1f) }                              // what six kept glides taught before R33
        assertEquals("precondition: the damage makes the halfway path ave", "ave", e.firstFor(xs, ys))
        var fixes = 0
        while (e.firstFor(xs, ys) == "ave" && fixes < 6) {
            e.glidePath(xs, ys); assertTrue("the glide gave ave: '${e.rig.text}'", e.rig.text.trimEnd().endsWith("ave"))
            assertTrue(e.s.pick("and")); e.space(); fixes++
        }
        assertTrue("the halfway path is and again after $fixes corrections", e.firstFor(xs, ys) == "and" && fixes in 1..5)
    }

    @Test fun pickingTheMeantWordTakesTheWrongOneBack() {
        val e = Env()
        repeat(3) { e.glide("ave"); e.space() }
        val before = e.engine.contextLn("ave", null, null)!!
        e.glide("ave")                                       // a fourth ave, and this time the strip is used: and is picked
        assertTrue(e.s.pick("and")); e.space()
        val after = e.engine.contextLn("ave", null, null)!!
        assertTrue("ave is less likely after the correction than before ($after vs $before)", after < before)
        assertTrue(e.rig.text.trimEnd().endsWith("and"))
    }

    @Test fun keepTrustFollowsHowRareTheWordIs() {
        val e = Env()
        assertTrue(e.engine.keepTrust("ave") < 0.15f); assertTrue(e.engine.keepTrust("fur") < 0.15f)
        assertEquals(1f, e.engine.keepTrust("and"), 1e-6f); assertEquals(1f, e.engine.keepTrust("the"), 1e-6f); assertTrue(e.engine.keepTrust("hello") > 0.8f)
        assertEquals("a word the list lacks is the person's own", 1f, e.engine.keepTrust("zzyzxq"), 1e-6f)
        assertTrue(e.engine.keepTrust("access") in 0.5f..1f)
        assertFalse(e.engine.keepTrust("maggie") > 0.5f)
    }

}
