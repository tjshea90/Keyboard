package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/**
 * Owner: "several times it predicted the word tui. I've never used this word in my life. It shouldn't ever predict rare words unless the glide pattern is very clear or if I use the rare words
 * enough that the keyboard learns them." A rare word must lose to a common word of similar path fit, may win when its path is clearly better, and is not rare any more once the person uses it.
 */
class RareWordTest {
    private val keys = GlideEval.keysFor()

    /** A list on the scale of the shipped one (the counts add up to about 5.4e11) with common words and rare lookalikes of them. */
    private fun scaled(entries: List<Pair<String, Long>>, pairs: List<LexiconFile.Pair> = emptyList()): BinaryLexicon {
        val sum = entries.sumOf { it.second }
        val filler = if (sum < 541_808_760_578L) listOf("zzfiller" to 541_808_760_578L - sum) else emptyList()
        return LexiconFile.read(LexiconFile.build((entries + filler).map { LexiconFile.Entry(it.first, it.second) }, pairs))
    }

    private val tui = scaled(listOf(
        "to" to 16_000_000_000L, "too" to 2_000_000_000L, "top" to 1_200_000_000L, "toy" to 500_000_000L, "tie" to 300_000_000L, "tip" to 400_000_000L, "tin" to 100_000_000L,
        "the" to 28_000_000_000L, "this" to 4_000_000_000L, "try" to 900_000_000L, "tour" to 400_000_000L,
        "tui" to 390_000L, "toi" to 250_000L, "tuk" to 200_000L, "tuis" to 150_000L,
    ))

    private fun firstWord(dec: GlideDecoder, lex: GlideLexicon, p: GestureSim.Path, prev: String? = null) = dec.decode(p.xs, p.ys, p.xs.size, listOf(lex), prev, 3).firstOrNull()?.text

    // ------------------------------------------------------------------ (a) a rare lookalike never beats the common word

    @Test fun rareLookalikesOfCommonWordsAreNeverFirstForNoisyGlides() {
        val common = listOf("to", "too", "top", "toy", "tie", "tip", "tin", "try", "tour", "this", "the")
        val rareWords = setOf("tui", "toi", "tuk", "tuis")
        for (noise in listOf(SimNoise.CALM, SimNoise.NORMAL, SimNoise.SLOPPY, SimNoise.BAD)) {
            val sim = GestureSim(keys, Random(21)); val dec = GlideDecoder(keys)
            var rareFirst = 0; var n = 0
            repeat(60) { for (w in common) { val p = sim.path(w, noise) ?: continue; n++; if (firstWord(dec, tui, p) in rareWords) rareFirst++ } }
            assertEquals("a rare lookalike came first in $rareFirst of $n glides of common words (${noise.name})", 0, rareFirst)
        }
    }

    @Test fun theRarityPriceIsWhatKeepsThemOut() {
        // the same glides without the price: the rare lookalikes do come first now and then (so the test above proves something)
        val common = listOf("to", "too", "top", "toy", "tie", "tip", "tin", "try", "tour", "this", "the")
        val rareWords = setOf("tui", "toi", "tuk", "tuis")
        var without = 0; var with = 0
        for (noise in listOf(SimNoise.NORMAL, SimNoise.SLOPPY, SimNoise.BAD)) {
            val sim1 = GestureSim(keys, Random(33)); val sim2 = GestureSim(keys, Random(33))
            val off = GlideDecoder(keys, GlideParams(rareW = 0f)); val on = GlideDecoder(keys)
            repeat(80) { for (w in common) {
                val a = sim1.path(w, noise) ?: continue; val b = sim2.path(w, noise) ?: continue
                if (firstWord(off, tui, a) in rareWords) without++
                if (firstWord(on, tui, b) in rareWords) with++
            } }
        }
        assertTrue("with the price $with rare firsts, without $without", with <= without)
    }

    @Test fun onTheShippedListRareWordsStopStealingTheFirstPlace() {
        val lex = GlideEval.loadLexicon()
        val words = RareWordEval.midWords(lex, lex, 1500, 77L)
        val off = RareWordEval.run(lex, lex, keys, GlideParams(rareW = 0f), SimNoise.BAD, words, 5L)
        val on = RareWordEval.run(lex, lex, keys, GlideParams(), SimNoise.BAD, words, 5L)
        assertTrue("very rare words in first place for common words: $on <- $off", on.deepSteals * 2 <= off.deepSteals || on.deepSteals <= 2)
        assertTrue("the price must not cost accuracy: $on <- $off", on.pct(on.top1) >= off.pct(off.top1) - 0.8)
        val sloppy = RareWordEval.run(lex, lex, keys, GlideParams(), SimNoise.SLOPPY, words, 5L)
        assertTrue("sloppy: $sloppy", sloppy.deepSteals <= 3)
    }

    @Test fun commonWordsAreStillFoundAsWellAsBefore() {
        val lex = GlideEval.loadLexicon()
        val words = RareWordEval.commonWords(lex, lex, 1500, 91L)
        val normal = RareWordEval.run(lex, lex, keys, GlideParams(), SimNoise.NORMAL, words, 9L)
        assertTrue("normal finger: $normal", normal.pct(normal.top1) >= 84.0 && normal.pct(normal.top3) >= 94.0)
        assertTrue("no rare word wins over a common one for a normal finger: $normal", normal.steals <= 3)
    }

    // ------------------------------------------------------------------ (b) a clear glide still finds a rare word

    @Test fun aCleanGlideOverARareRealWordStillGivesThatWord() {
        val clean = SimNoise(0.4f, 1.5f, 0f, 0.1f, 0f, "exact")
        val lex = GlideEval.loadLexicon()
        val rare = listOf("xylophone", "quixotic", "zucchini", "kaleidoscope", "obsequious", "pyrotechnics", "serendipity", "onomatopoeia", "ephemeral", "juxtaposition")
        var hit = 0; var tried = 0
        val dec = GlideDecoder(keys)
        for (w in rare) {
            val id = lex.idOf(w)
            if (id < 0) continue
            tried++
            val p = GestureSim(keys, Random(3)).path(w, clean) ?: continue
            if (firstWord(dec, lex, p) == w) hit++
        }
        assertTrue("only $tried of the test words are in the list", tried >= 7)
        assertTrue("a clean glide of a rare word: $hit of $tried", hit >= tried - 1)
        // the same on the small list: a rare word whose letters the finger plainly drew beats the common words that miss them
        val small = scaled(listOf("the" to 28_000_000_000L, "tie" to 300_000_000L, "tire" to 400_000_000L, "tile" to 300_000_000L, "tide" to 200_000_000L, "tithe" to 150_000L))
        val dec2 = GlideDecoder(keys)
        assertEquals("tithe", firstWord(dec2, small, GestureSim(keys, Random(3)).path("tithe", clean)!!))
    }

    // ------------------------------------------------------------------ (c) a word the person uses is not rare

    /** A glide that ends exactly between the keys r and t after s, a: "sat" and "sar" fit it equally well; only the language can decide. */
    private fun ambiguousPath(): GestureSim.Path {
        val ax = keys.cx['s' - 'a']; val ay = keys.cy['s' - 'a']
        val bx = keys.cx['a' - 'a']; val by = keys.cy['a' - 'a']
        val ex = (keys.cx['r' - 'a'] + keys.cx['t' - 'a']) / 2f; val ey = (keys.cy['r' - 'a'] + keys.cy['t' - 'a']) / 2f
        val xs = ArrayList<Float>(); val ys = ArrayList<Float>()
        for (i in 0..12) { xs.add(ax + (bx - ax) * i / 12f); ys.add(ay + (by - ay) * i / 12f) }
        for (i in 1..16) { xs.add(bx + (ex - bx) * i / 16f); ys.add(by + (ey - by) * i / 16f) }
        return GestureSim.Path(xs.toFloatArray(), ys.toFloatArray())
    }

    private val sat = scaled(listOf("sat" to 400_000_000L, "sar" to 380_000L, "the" to 28_000_000_000L))

    @Test fun aRareWordLosesATieToACommonOneUntilThePersonUsesIt() {
        val dec = GlideDecoder(keys)
        val p = ambiguousPath()
        assertEquals("sat", firstWord(dec, sat, p))
        val pm = PersonalModel()
        dec.personal = pm
        pm.add(null, "the", 1f)
        assertEquals("one use of a different word changes nothing", "sat", firstWord(dec, sat, p))
        repeat(6) { pm.add(null, "sar", 1f) }
        assertEquals("a word the person writes is no longer rare", "sar", firstWord(dec, sat, p))
    }

    @Test fun aFewUsesAreEnoughToLiftARareWordOverItsCommonLookalike() {
        val dec = GlideDecoder(keys)
        val pm = PersonalModel(); dec.personal = pm
        val p = ambiguousPath()
        var uses = 0
        while (firstWord(dec, sat, p) != "sar" && uses < 12) { pm.add(null, "sar", 1f); uses++ }
        assertTrue("took $uses uses", uses in 1..6)
    }

    @Test fun thePairWithTheWordBeforeAlsoMakesARareWordLikely() {
        // "hong kong": kong alone is rare, after hong it is what is meant
        val l = scaled(
            listOf("hong" to 30_000_000L, "kong" to 400_000L, "long" to 900_000_000L, "kind" to 300_000_000L, "song" to 300_000_000L, "the" to 28_000_000_000L),
            listOf(LexiconFile.Pair("hong", "kong", 2_000_000L), LexiconFile.Pair("hong", "long", 10L)),
        )
        val dec = GlideDecoder(keys)
        val p = GestureSim(keys, Random(4)).path("kong", SimNoise.NORMAL)!!
        val after = dec.decode(p.xs, p.ys, p.xs.size, listOf(l), "hong", 3).firstOrNull()?.text
        assertEquals("kong", after)
    }

    @Test fun rarityIsAboutTheListNotAboutTheLength() {
        assertEquals(0f, Rarity.below(-5f), 0f)
        assertEquals(0f, Rarity.below(Rarity.FLOOR), 0f)
        assertTrue(Rarity.below(-14f) > 1f && Rarity.below(-14f) < Rarity.below(-18f))
        assertEquals(7f, Rarity.below(-40f), 0f)
        assertFalse(Rarity.below(-14f) == 0f)
        assertNotEquals(0f, GlideParams().rareW)
    }
}
