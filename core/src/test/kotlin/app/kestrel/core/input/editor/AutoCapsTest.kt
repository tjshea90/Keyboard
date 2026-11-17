package app.kestrel.core.input.editor

import org.junit.Assert.assertEquals
import org.junit.Test

/** KB-104: the field's own capitalisation request decides whether Shift is on when typing starts and after Enter (per-keystroke auto-capitalisation is KB-203). */
class AutoCapsTest {
    @Test fun aSentenceFieldStartsWithShiftOnAndTheFirstLetterSpendsIt() {
        val r = Rig(caps = CapFlags.SENTENCES); assertEquals(ShiftState.ONCE, r.c.shift)
        r.typeAll("h"); assertEquals("H", r.text); assertEquals(ShiftState.OFF, r.c.shift)
    }
    @Test fun aWordsFieldStartsWithShiftOnAtTheStart() { assertEquals(ShiftState.ONCE, Rig(caps = CapFlags.WORDS).c.shift) }
    @Test fun aCharactersFieldAlwaysAsksForCapitals() { assertEquals(ShiftState.ONCE, Rig("abc", caps = CapFlags.CHARACTERS).c.shift) }
    @Test fun aFieldThatAskedForNothingStartsLowercase() { assertEquals(ShiftState.OFF, Rig().c.shift) }
    @Test fun inTheMiddleOfASentenceShiftStaysOff() { assertEquals(ShiftState.OFF, Rig("hello", caps = CapFlags.SENTENCES).c.shift) }
    @Test fun afterAFullStopAndASpaceShiftIsOn() { assertEquals(ShiftState.ONCE, Rig("Hello. ", caps = CapFlags.SENTENCES).c.shift) }
    @Test fun afterANewlineShiftIsOn() { assertEquals(ShiftState.ONCE, Rig("one\n", caps = CapFlags.SENTENCES).c.shift) }
    @Test fun enterStartsANewParagraphSoShiftComesBack() {
        val r = Rig(caps = CapFlags.SENTENCES).typeAll("hi"); assertEquals(ShiftState.OFF, r.c.shift)
        r.act { enter() }; assertEquals("Hi\n", r.text); assertEquals(ShiftState.ONCE, r.c.shift)
    }
    @Test fun capsLockIsNeverSwitchedOffByTheField() { val r = Rig(caps = CapFlags.SENTENCES); r.act { lockShift() }; r.act { autoCaps() }; assertEquals(ShiftState.LOCK, r.c.shift) }
    @Test fun aDeadEditorMeansNoCapitals() { val r = Rig(caps = CapFlags.SENTENCES); r.act { setShift(ShiftState.OFF) }; r.ed.failing = true; r.c.autoCaps(); assertEquals(ShiftState.OFF, r.c.shift) }
}
