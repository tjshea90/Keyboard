package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * R36: what a glide of a KNOWN word teaches ([GlideTeaching]). A made-up hand with a habit (its "o" of "for" always lands towards "i", so the plain keyboard reads "fir") glides the asked-for word again and again; after each
 * glide the keyboard is taught the word, exactly as the Glide training screen does it.
 */
class GlideTeachingTest {
    private val lex = GlideEval.loadLexicon()

    /** [n] glides of [word] by [hand]; after every one the decoder is taught (when [teach]) the way the training screen teaches it. Returns what came first each time. */
    private fun repeat(keys: GlideKeys, hand: GlideHabitEval.Hand, word: String, n: Int, teach: Boolean, model: PersonalModel = PersonalModel(), parts: GlideTeaching.Parts = GlideTeaching.Parts()): List<String> {
        val dec = GlideDecoder(keys, GlideParams.forPitchDp(keys.pitchDp))
        dec.personal = model
        val out = ArrayList<String>()
        repeat(n) {
            val path = hand.path(word) ?: return@repeat
            val c = dec.decode(path.xs, path.ys, path.xs.size, listOf(lex), null, 3)
            val first = c.firstOrNull()?.text ?: "-"
            out.add(first)
            if (teach) GlideTeaching.teach(dec, lex, lex.idOf(word), model, keys, word, c.firstOrNull()?.text, dec.lastPrint, parts)
        }
        return out
    }

    @Test fun explore() {
        for ((nm, wh) in listOf("phone" to (292f to 161f), "tablet" to (564f to 272f))) {
            val keys = GlideEval.keysFor(wh.first, wh.second)
            for (pit in listOf(1.2f, 1.6f, 2.0f)) for (aim in listOf(4f, 8f)) {
                val habit = GlideHabitEval.forHabit(keys, pit)
                val plain = repeat(keys, GlideHabitEval.Hand(keys, SimNoise.aim(aim), 5L, 0f, habit), "for", 12, teach = false)
                val taught = repeat(keys, GlideHabitEval.Hand(keys, SimNoise.aim(aim), 5L, 0f, habit), "for", 12, teach = true)
                println("TEACH $nm shift $pit aim $aim plain ${plain.count { it == "for" }}/12 ${plain.take(6)}  taught ${taught.count { it == "for" }}/12 $taught")
            }
        }
    }

    @Test fun aHabitualMissIsFixedByAFewTaughtGlidesAndStaysWrongWithoutThem() {
        // phone-sized keys: the "o" of "for" lands 0.9 of a key towards "i" every time
        val keys = GlideEval.keysFor(292f, 161f)
        val habit = GlideHabitEval.forHabit(keys, 0.9f)
        val noise = SimNoise.aim(6f)
        val plain = repeat(keys, GlideHabitEval.Hand(keys, noise, 5L, 0f, habit), "for", 12, teach = false)
        val taught = repeat(keys, GlideHabitEval.Hand(keys, noise, 5L, 0f, habit), "for", 12, teach = true)
        println("TEACH for-habit plain: $plain")
        println("TEACH for-habit taught: $taught")
        assertTrue("the made-up hand really makes the plain keyboard miss (${plain.count { it == "for" }} of 12 right)", plain.count { it == "for" } <= 6)
        assertTrue("taught glides lift the late answers: ${taught.takeLast(6)} vs plain ${plain.takeLast(6)}", taught.takeLast(6).count { it == "for" } > plain.takeLast(6).count { it == "for" })
    }

    @Test fun teachingNeverTouchesTheWordCountsPairsOrTriples() {
        val keys = GlideEval.keysFor(564f, 272f)
        val model = PersonalModel()
        val hand = GlideHabitEval.Hand(keys, SimNoise.aim(8f), 3L, 0f)
        repeat(keys, hand, "there", 6, teach = true, model = model)
        assertEquals("a training text is not what the person writes", 0, model.wordCount)
        assertEquals(0, model.pairCount)
        assertEquals(0, model.tripleCount)
        assertTrue("but the hand was learned: the shape of the word", model.patterns.patternCount > 0)
    }

    @Test fun aGlideFarFromItsWordIsNotRememberedAsItsShape() {
        val keys = GlideEval.keysFor(564f, 272f)
        val dec = GlideDecoder(keys, GlideParams.forPitchDp(keys.pitchDp)); val model = PersonalModel(); dec.personal = model
        // the path of "access" taught as "the": nowhere near the straight line through t-h-e
        val sim = GestureSim(keys, java.util.Random(1))
        val p = sim.path("access", SimNoise.CALM)!!
        dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), null, 3)
        val l = GlideTeaching.teach(dec, lex, lex.idOf("the"), model, keys, "the", "access", dec.lastPrint)
        assertTrue("too wild to keep as the shape of 'the'", l.wild)
        assertFalse(l.shape)
        assertEquals(0, model.patterns.patternCount)
        assertTrue("what it was mistaken for is still counted", l.confusion)
        assertEquals(1f, model.confusionWeight("access", "the"), 0f)
    }

    @Test fun theLessonsCanBeSwitchedOffOneByOne() {
        val keys = GlideEval.keysFor(564f, 272f)
        val hand = GlideHabitEval.Hand(keys, SimNoise.aim(6f), 9L, 0f)
        val m1 = PersonalModel()
        repeat(keys, hand, "and", 3, teach = true, model = m1, parts = GlideTeaching.Parts(thumb = false, shape = false, confusion = true))
        assertEquals("no shape lesson", 0, m1.patterns.patternCount)
        val m2 = PersonalModel()
        repeat(keys, hand, "and", 3, teach = true, model = m2, parts = GlideTeaching.Parts(thumb = false, shape = true, confusion = false))
        assertTrue(m2.patterns.patternCount > 0)
        assertEquals(0, m2.confusionCount)
    }

    @Test fun aWordTheListDoesNotHaveTeachesNoThumb() {
        val keys = GlideEval.keysFor(564f, 272f)
        val dec = GlideDecoder(keys, GlideParams.forPitchDp(keys.pitchDp)); val model = PersonalModel(); dec.personal = model
        val p = GestureSim(keys, java.util.Random(2)).path("there", SimNoise.CALM)!!
        dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), null, 3)
        val l = GlideTeaching.teach(dec, null, -1, model, keys, "there", "the", dec.lastPrint)
        assertFalse(l.thumb)
        assertNotNull(l)
    }
}
