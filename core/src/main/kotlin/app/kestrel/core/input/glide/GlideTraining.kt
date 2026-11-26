package app.kestrel.core.input.glide

import java.util.Random

/**
 * R36 (owner 2026-11-26: "Can you make a much longer glide training that I can glide full paragraphs and problematic words several times and the keyboard learns how I glide?"): the script of the Glide training screen.
 *
 * It is the long sibling of the 40-word [GlidePractice] drill: EVERYDAY PARAGRAPHS the person glides word by word (the keyboard decodes each glide with the real words before it, like in a message, so the language
 * helps and fails exactly as it does in daily use), and DRILL pages with the words that are easy to mix up (the / there / they, and / any / access, for / from, or / our / out ...), each glided several times and
 * never twice in a row. After the scheduled pages comes one more page built from the words the keyboard got wrong during this training, together with what it typed instead (so a pair is practised both ways: a
 * keyboard that only hears "our" learns to answer "our" for everything). Every glide is kept in the same private file as the drill (format 2: with the two words before it and its page), and - when the screen
 * lets the keyboard learn - taught to the engine ([GlideTeaching]).
 *
 * Only the PROMPTED words are ever kept. One-letter words ("I", "a") are shown and typed by the person's own tap, never prompted. Pure Kotlin, no Android.
 */
class GlideTraining(
    paragraphs: List<String> = PARAGRAPHS,
    drills: List<Pair<String, List<String>>> = DRILLS,
    private val repeats: Int = 4,
    extraWords: List<String> = emptyList(),
    private val seed: Long = 11L,
) {
    enum class Kind { TEXT, DRILL, MISSED }

    /** One word of a page as it is shown ([shown], with its punctuation) and what is asked for ([word], lower-case letters; null for a one-letter word or a bare mark: nothing to glide). */
    class Token(val shown: String, val word: String?) {
        /** What happened to it: [PENDING], [RIGHT] (the keyboard typed it), [NEAR] (among the keyboard's first three), [MISS], [SKIPPED], [FREE] (not prompted). */
        var state = if (word == null) FREE else PENDING
    }

    class Page(val kind: Kind, val title: String, val tokens: List<Token>) { val prompts: Int get() = tokens.count { it.word != null } }

    /** How a page went: glides answered, first guess right, right word among the first three. */
    class Score(val answered: Int, val right: Int, val inThree: Int)

    private val pageList = ArrayList<Page>()
    private class Slot(val page: Int, val token: Int)
    private val slots = ArrayList<Slot>()
    /** Every prompted word in order, the finger's paths and what the keyboard answered (the file). */
    val practice: GlidePractice
    private var missedAdded = false
    /** True while the person has just finished a page and has not asked for the next one: no glide counts. */
    var atBreak = false; private set
    private var finishedEarly = false

    init {
        // the scheduled pages: two paragraphs, a drill, two paragraphs, a drill ... (the person's own words after the second paragraph)
        val order = ArrayList<Page>()
        var pi = 0; var di = 0; var textNo = 0
        fun text() { if (pi < paragraphs.size) { textNo++; order.add(Page(Kind.TEXT, "Paragraph $textNo", tokensOf(paragraphs[pi++]))) } }
        fun drill() { if (di < drills.size) { order.add(Page(Kind.DRILL, drills[di].first, drillTokens(drills[di].second, di))); di++ } }
        while (pi < paragraphs.size || di < drills.size) {
            text(); text()
            if (pi == 2 && extraWords.isNotEmpty()) order.add(Page(Kind.DRILL, "Your own problem words", drillTokens(extraWords, 99)))
            drill()
        }
        for (p in order) addPage(p)
        practice = GlidePractice(slots.map { pageList[it.page].tokens[it.token].word!! })
    }

    private fun addPage(p: Page) {
        val idx = pageList.size
        pageList.add(p)
        for ((i, t) in p.tokens.withIndex()) if (t.word != null) slots.add(Slot(idx, i))
    }

    val pages: List<Page> get() = pageList
    val pageCount: Int get() = pageList.size
    val answered: Int get() = practice.answered
    val total: Int get() = practice.total
    val done: Boolean get() = practice.done || finishedEarly

    /** The slot of the word to glide now, or null when everything is answered. */
    private val slot: Slot? get() = if (finishedEarly) null else slots.getOrNull(practice.index)

    /** The page being worked on (the last one when everything is done). */
    val pageIndex: Int get() = slot?.page ?: (pageList.size - 1).coerceAtLeast(0)
    val page: Page? get() = pageList.getOrNull(pageIndex)
    /** The token to glide now (its position in the page: [tokenIndex]). */
    val token: Token? get() = slot?.let { pageList[it.page].tokens[it.token] }
    val tokenIndex: Int get() = slot?.token ?: -1
    val current: String? get() = token?.word
    /** Prompts answered or skipped on the current page / how many it has. */
    val pagePos: Int get() { val s = slot ?: return page?.prompts ?: 0; return slots.subList(0, practice.index).count { it.page == s.page } }
    /** The page that was just finished (while [atBreak]). */
    val breakPage: Int get() = if (atBreak) (slots.getOrNull(practice.index - 1)?.page ?: pageIndex) else -1

    /**
     * What the keyboard should see before the word to glide: the last few words of the paragraph as they are written (so it decodes with the real words before it, punctuation included), or nothing on a drill page
     * (a drill word is judged on its own shape). Ends with a space when it is not empty.
     */
    fun boxText(): String {
        val s = slot ?: return ""
        val p = pageList[s.page]
        if (p.kind != Kind.TEXT || s.token == 0) return ""
        val from = (s.token - CONTEXT_TOKENS).coerceAtLeast(0)
        return p.tokens.subList(from, s.token).joinToString(" ") { it.shown } + " "
    }

    /**
     * The glide of the current word: [n] points; [got] what the keyboard offered. [prev] and [prev2] are the words it was decoded with. Marks the token, moves on, and says whether the page is now finished (the
     * caller shows its score and waits for [continueNext]). Returns the word that was asked for (to teach), or null when nothing was asked for.
     */
    fun record(xs: FloatArray, ys: FloatArray, ts: FloatArray, n: Int, got: List<String>, prev: String? = null, prev2: String? = null): String? {
        val s = slot ?: return null
        if (atBreak) return null
        val t = pageList[s.page].tokens[s.token]
        val w = t.word ?: return null
        practice.record(xs, ys, ts, n, got, prev, prev2, "p${s.page + 1}")
        t.state = if (got.firstOrNull() == w) RIGHT else if (w in got) NEAR else MISS
        afterAnswer(s)
        return w
    }

    /** The person skipped the current word. */
    fun skip() {
        val s = slot ?: return
        if (atBreak) return
        pageList[s.page].tokens[s.token].state = SKIPPED
        practice.skip()
        afterAnswer(s)
    }

    private fun afterAnswer(s: Slot) {
        val next = slots.getOrNull(practice.index)
        if (next == null || next.page != s.page) atBreak = true
        if (next == null && !missedAdded) addMissedPage()
    }

    /** The words of this training that went wrong, with what was typed instead, as one more page (nothing when nothing went wrong). */
    private fun addMissedPage() {
        missedAdded = true
        val words = LinkedHashSet<String>()
        for ((asked, typed) in practice.misses()) {
            words.add(asked)
            if (typed.length >= 2 && typed.all { it in 'a'..'z' }) words.add(typed)
            if (words.size >= MAX_MISSED) break
        }
        if (words.isEmpty()) return
        val list = words.take(MAX_MISSED)
        val page = Page(Kind.MISSED, "Words that went wrong", drillTokens(list, 77, 3))
        addPage(page)
        practice.append(page.tokens.mapNotNull { it.word })
    }

    /** Go on after a finished page. */
    fun continueNext() { atBreak = false }

    /** The person stops here: what was answered so far is the result. */
    fun finishNow() { finishedEarly = true; atBreak = false; practice.finishNow() }

    /** How page [i] went (from what was recorded with its tag). */
    fun score(i: Int): Score {
        val tag = "p${i + 1}"
        val es = practice.entries.filter { it.tag == tag }
        return Score(es.size, es.count { it.got.firstOrNull() == it.word }, es.count { it.word in it.got })
    }

    fun totalScore(): Score = Score(practice.answered, practice.firstRight(), practice.inThree())

    /** The file of the whole training: format 2 ([GlidePractice.serialize] with [training]); [teaching] says whether the keyboard learned from the glides. */
    fun serialize(keys: GlideKeys, viewW: Int, viewH: Int, density: Float, version: String, teaching: Boolean): String = practice.serialize(keys, viewW, viewH, density, version, training = true, teaching = teaching)

    // ------------------------------------------------------------------ building pages

    private fun tokensOf(paragraph: String): List<Token> = paragraph.trim().split(Regex("\\s+")).map { t ->
        val letters = t.filter { it in 'a'..'z' || it in 'A'..'Z' }.lowercase()
        Token(t, if (letters.length >= 2 && letters.length == t.count { it.isLetter() }) letters else null)
    }

    /** [words] each [rep] times, in rounds: every round has each word once in a new order, and the last word of a round is never the first of the next (the same word twice in a row is not a repetition of a glide, it is a stutter). */
    private fun drillTokens(words: List<String>, salt: Int, rep: Int = repeats): List<Token> {
        val rnd = Random(seed * 131 + salt)
        val out = ArrayList<Token>()
        var last: String? = null
        val list = words.distinct()
        repeat(rep) {
            var round = list.shuffled(rnd)
            if (round.size > 1 && round.first() == last) round = round.drop(1) + round.first()
            for (w in round) out.add(Token(w, w))
            last = round.last()
        }
        return out
    }

    companion object {
        const val PENDING = 0; const val RIGHT = 1; const val NEAR = 2; const val MISS = 3; const val SKIPPED = 4; const val FREE = 5
        /** How many words before the one to glide the box shows (and the keyboard sees). */
        const val CONTEXT_TOKENS = 8
        /** The page of missed words has at most this many different words. */
        const val MAX_MISSED = 12

        /** Extra problem words typed by the person: lower-case letters only, 2 to 14 letters, no repeats, at most 10. */
        fun cleanWords(text: String): List<String> = text.lowercase().split(Regex("[^a-z]+")).filter { it.length in 2..14 }.distinct().take(10)

        /** Look-alike words: each group is a page, every word of it several times, in rounds. All of them are in the shipped word list (GlideTrainingTest). */
        val DRILLS: List<Pair<String, List<String>>> = listOf(
            "Words that look alike: the, there, they" to listOf("the", "there", "they", "then", "and", "any", "access"),
            "Words that look alike: for, or, our" to listOf("for", "from", "or", "our", "out", "you", "your"),
            "Short words that get mixed up" to listOf("it", "is", "in", "on", "to", "too", "of", "off"),
        )

        /** Everyday paragraphs in the way people write messages: no names, nothing personal, every word in the shipped list, no punctuation inside a word. */
        val PARAGRAPHS: List<String> = listOf(
            "I was thinking we could meet tomorrow after work and get something to eat. Let me know what time works for you and I will be there. If it rains we can stay home and watch a movie instead.",
            "Thank you for sending the report so quickly. I read through it this morning and it looks good to me. There are a few small things I would like to change, but nothing that cannot wait until next week.",
            "We are almost out of milk and bread, so can you pick some up on the way home? Also please check if the package arrived today. It should have been here by now, and I really need it before the weekend.",
            "The flight leaves early on Friday, so we should probably leave the house around five. I already packed most of our things. Do you think we need a jacket, or will the weather be warm enough all week?",
            "Honestly I think it was a great idea. People always say they never have enough time, but we found a way to make it work. Everyone seemed to enjoy the evening, and nobody wanted to leave when it was over.",
            "My phone keeps freezing every time I open the camera. I tried to restart it twice and nothing changed. Could you take a look when you have a minute? I can bring it over tonight if that is easier for you.",
            "Just a reminder that your appointment is on Tuesday at ten in the morning. Please arrive a few minutes early and bring your insurance card. If you cannot make it, call us as soon as possible so we can find another time.",
            "After dinner we walked down to the park and sat by the water for a while. The kids were running around, and the whole place felt calm. It was exactly what we needed after such a long and stressful week.",
        )
    }
}
