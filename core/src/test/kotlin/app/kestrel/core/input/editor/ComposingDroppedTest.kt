package app.kestrel.core.input.editor

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * R40-3 (owner 2026-11-28: "i glide a word and the keyboard gets it wrong so i delete half the word and press the correct word which is suggested, but it leaves the half typed word and simply adds the correct word to it").
 * Many apps end the composing word on their own while the cursor stays where it was (a web page's script, a React / Flutter / Compose field that re-sets its state). Android tells the keyboard in the same report that carries
 * the composing region (-1..-1). The keyboard must believe it: a word it still thinks it is "composing" would be replaced with setComposingText, which with NO composing region in the editor inserts NEXT TO the letters.
 */
class ComposingDroppedTest {
    @Test fun aStripWordReplacesTheHalfTypedLettersEvenWhenTheAppEndedTheComposingWord() {
        val r = Rig("")
        r.typeAll("acce")
        r.ed.appEndsComposing(); r.sync(); r.check()
        r.act { pickWord("access") }
        assertEquals("access ", r.text)
    }

    @Test fun backspaceStillDeletesWhenTheAppEndedTheComposingWord() {
        val r = Rig("")
        r.typeAll("acce")
        r.ed.appEndsComposing(); r.sync(); r.check()
        r.act { backspace() }
        assertEquals("acc", r.text)
        r.act { backspace() }
        assertEquals("ac", r.text)
    }

    @Test fun anAutomaticFixReplacesTheTypoEvenWhenTheAppEndedTheComposingWord() {
        val r = Rig("")
        r.typeAll("teh")
        r.ed.appEndsComposing(); r.sync(); r.check()
        r.act { typeCorrected("the", " ") }
        assertEquals("the ", r.text)
    }

    @Test fun lettersTypedAfterTheAppEndedTheComposingWordStartANewOneAndStayAfterIt() {
        val r = Rig("")
        r.typeAll("ac")
        r.ed.appEndsComposing(); r.sync(); r.check()
        r.typeAll("ce")
        assertEquals("acce", r.text)
        r.act { pickWord("access") }
        assertEquals("access ", r.text)
    }
}
