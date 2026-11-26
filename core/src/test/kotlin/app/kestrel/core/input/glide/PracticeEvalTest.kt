package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/** R34a2: the lab that will read the owner's practice file works end to end: simulated glides on tablet-sized keys are written to the file format, read back and decoded on the recorded key positions. */
class PracticeEvalTest {
    @Test fun aPracticeFileOfSimulatedTabletGlidesIsReadBackAndDecodedLikeTheKeyboardDidIt() {
        val keys = GlideEval.keysFor(564f, 272f)
        val sim = GestureSim(keys, Random(7))
        val practice = GlidePractice()
        for (w in practice.words) {
            val p = sim.path(w, SimNoise.aim(8f)) ?: continue
            val n = p.xs.size
            practice.record(p.xs, p.ys, FloatArray(n) { it * 12f }, n, listOf(w))
        }
        assertTrue("the simulator drew every word", practice.answered >= 38)
        val text = practice.serialize(keys, 564, 272, 1f, "test")
        val parsed = GlidePractice.parse(text)
        assertNotNull(parsed); assertEquals(practice.answered, parsed!!.entries.size)
        val lines = ArrayList<String>()
        val r = PracticeEval.report(parsed, GlideEval.loadLexicon(), null, sweep = false) { lines.add(it) }
        assertTrue("the report says what the keyboard did and what the decoder does: $lines", lines.any { it.contains("the keyboard's own answers") } && lines.any { it.contains("decoder alone") })
        assertTrue("the shipped decoder reads most simulated glides on big keys right (${r.second} of ${r.first})", r.second >= 0.80 * r.first)
        assertTrue(r.third >= r.second)
    }
}
