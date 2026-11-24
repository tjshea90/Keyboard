package app.kestrel.core.input.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * R32a (owner 2026-11-24: "keeps spaces after a word preceding a comma ... follow how popular keyboards work"). The rule of AOSP LatinIME / HeliBoard (the open-source keyboard Gboard descends from): a space that sits
 * right after a word is a WEAK space, and a comma, full stop, ! or ? typed on it takes its place ("word ," becomes "word,"), whether the space was put there by a glide, a strip word, or the person's own thumb.
 * research/R32-punctuation-suggestions.md
 */
class PunctuationSwapTest {
    private fun rig(text: String = "", kind: FieldKind = FieldKind.TEXT, autoSpace: Boolean = true, caps: Int = 0): Rig { val r = Rig(text, kind = kind, caps = caps); r.c.autoSpace = autoSpace; return r }

    @Test fun aSpaceTheThumbTypedIsTakenBackWhenACommaFollows() {
        for (p in listOf(",", ".", "!", "?")) {
            val r = rig(); r.typeAll("hello "); r.act { type(p) }
            assertEquals("hello$p", r.text)
        }
    }

    @Test fun afterTheSwapTheNextLetterGetsItsSpaceAndAFullStopGivesACapital() {
        val r = rig(caps = CapFlags.SENTENCES); r.typeAll("hello "); r.act { type(",") }; r.typeAll("world"); assertEquals("Hello, world", r.text)
        val s = rig(caps = CapFlags.SENTENCES); s.typeAll("hello "); s.act { type(".") }; s.typeAll("world"); assertEquals("Hello. World", s.text)
    }

    @Test fun aWordTakenFromTheStripLeavesNoSpaceBeforeTheComma() {
        val r = rig(); r.typeAll("helo"); r.act { pickWord("hello") }; assertEquals("hello ", r.text)
        r.act { type(",") }; assertEquals("hello,", r.text)
    }

    @Test fun aNextWordTakenFromTheStripLeavesNoSpaceBeforeThePeriod() {
        val r = rig("hello "); r.act { pickWord("world") }; assertEquals("hello world ", r.text)
        r.act { type(".") }; assertEquals("hello world.", r.text)
    }

    @Test fun aGlideStillGivesTheSameResult() {
        val r = rig(); r.act { commitGlide("hello") }; r.act { type(",") }; assertEquals("hello,", r.text)
    }

    @Test fun theSpaceIsTakenEvenWhenTheKeyboardLostTrackOfItsOwnGlide() {
        // an app rewrote the text or the reports got out of step: the keyboard no longer knows the glide, but the text still says "word ,"
        val r = rig(); r.act { commitGlide("hello") }; r.ed.userInsert(""); r.sync()
        val s = rig("see you "); s.act { type(",") }; assertEquals("see you,", s.text)
    }

    @Test fun anAutomaticFixThenSpaceThenCommaIsTidyToo() {
        val r = rig(); r.typeAll("helo"); r.act { typeCorrected("hello", " ") }; assertEquals("hello ", r.text)
        r.act { type(",") }; assertEquals("hello,", r.text)
    }

    @Test fun backspaceRightAfterTheSwapPutsTheSpaceBack() {
        val r = rig(); r.typeAll("hello "); r.act { type(",") }; assertEquals("hello,", r.text)
        r.act { backspace() }; assertEquals("hello ", r.text)
        r.act { backspace() }; assertEquals("hello", r.text)
    }

    @Test fun theSwapIsOnlyUndoneByTheBackspaceRightAfterIt() {
        val r = rig(); r.typeAll("hello "); r.act { type(",") }; r.typeAll("w"); r.act { backspace() }; r.act { backspace() }
        assertEquals("hello,", r.text)                       // (the second Backspace after the letter was removed deletes only the space the keyboard added, not the comma's swap)
    }

    // ------------------------------------------------------------------ what must NOT change

    @Test fun smileysAndSemicolonsKeepTheirSpace() {
        val r = rig(); r.typeAll("hello "); r.act { type(":") }; r.act { type(")") }; assertEquals("hello :)", r.text)
        val s = rig(); s.typeAll("ok "); s.act { type(";") }; s.act { type(")") }; assertEquals("ok ;)", s.text)
    }

    @Test fun twoSpacesAreNotTouched() {
        val r = rig(); r.typeAll("hello  "); r.act { type(",") }; assertEquals("hello  ,", r.text)
    }

    @Test fun nothingHappensAfterPunctuationOrAtTheStart() {
        val r = rig("hello. "); r.act { type(",") }; assertEquals("hello. ,", r.text)
        val s = rig(" "); s.act { type(",") }; assertEquals(" ,", s.text)
    }

    @Test fun numbersLoseTheirSpaceToo() {
        val r = rig(); r.typeAll("5 "); r.act { type(",") }; assertEquals("5,", r.text)
    }

    @Test fun theSwitchTurnsItOff() {
        val r = rig(autoSpace = false); r.typeAll("hello "); r.act { type(",") }; assertEquals("hello ,", r.text)
    }

    @Test fun addressAndPasswordBoxesAreLeftAlone() {
        for (k in listOf(FieldKind.URL, FieldKind.EMAIL, FieldKind.PASSWORD, FieldKind.OMNIBOX, FieldKind.NUMBER)) {
            val r = rig(kind = k); r.typeAll("ab "); r.act { type(",") }; assertEquals("kind $k", "ab ,", r.text)
        }
    }

    @Test fun aSelectionIsReplacedNotTidied() {
        val r = rig("hello world"); r.ed.userSelect(0, 5); r.sync(); r.act { type(",") }
        assertEquals(",", r.text.take(1))
    }

    @Test fun theSwapMarkIsForgottenWhenTheCursorMoves() {
        val r = rig(); r.typeAll("hello "); r.act { type(",") }; r.act { moveCursor(-3) }; r.act { moveCursor(3) }; r.act { backspace() }
        assertEquals("hello", r.text)                        // (a plain Backspace: the comma is deleted, no space is put back)
        assertNull(r.c.lastGlide)
    }
}
