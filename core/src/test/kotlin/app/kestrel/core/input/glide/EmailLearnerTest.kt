package app.kestrel.core.input.glide

import app.kestrel.core.input.Key
import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.Rig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** R32c: only an address typed key by key is learned, and only where learning is allowed; a strip pick completes what was typed. */
class EmailLearnerTest {
    private class Setup(kind: FieldKind = FieldKind.EMAIL, noLearn: Boolean = false) {
        val r = Rig(kind = kind, noLearn = noLearn)
        val book = EmailBook()
        val l = EmailLearner(r.c) { book }
        fun key(k: String) { val key = if (k == "\n") Key.Go else Key.Ch(k); l.beforeKey(key); r.act { if (k == "\n") enter() else type(k) }; l.afterKey() }
        fun typeAll(s: String) { for (c in s) key(c.toString()) }
    }

    @Test fun anAddressTypedInAnEmailBoxIsLearnedWhenTheBoxIsLeft() {
        val s = Setup(); s.typeAll("john@gmail.com")
        assertTrue("not yet: the box is still open", s.book.isEmpty); s.l.finishField()
        assertEquals(listOf("john@gmail.com"), s.book.addresses())
    }

    @Test fun theActionKeyCountsToo() {
        val s = Setup(); s.typeAll("a.b@work.example"); s.key("\n"); assertEquals(listOf("a.b@work.example"), s.book.addresses())
        s.l.finishField(); assertEquals("the box was already counted: not twice", 1, s.book.suggest("").size)
    }

    @Test fun anAddressInAMessageCountsWhenASpaceEndsIt() {
        val s = Setup(FieldKind.TEXT); s.typeAll("write to bob@work.example please"); assertEquals(listOf("bob@work.example"), s.book.addresses())
    }

    @Test fun aSentenceEndingRightBehindTheAddressIsNotPartOfIt() {
        val s = Setup(FieldKind.TEXT); s.typeAll("mail me at me@home.org."); s.key(" "); assertEquals(listOf("me@home.org"), s.book.addresses())
    }

    @Test fun anAddressThatWasPastedIsNeverLearned() {
        val s = Setup(FieldKind.TEXT); s.r.act { paste("pasted@clip.com") }; s.key(" "); assertTrue(s.book.isEmpty)
        val e = Setup(); e.r.act { paste("auto@fill.com") }; e.l.afterKey(); e.l.finishField(); assertTrue(e.book.isEmpty)
    }

    @Test fun anAddressFinishedBySomethingElseThanTypingIsNotLearned() {
        val s = Setup(); s.typeAll("jo"); s.r.act { pickAddress(2, "john@gmail.com") }; s.l.afterKey(); s.l.finishField()
        assertTrue("a strip pick is not typing", s.book.isEmpty)
        s.l.picked("john@gmail.com"); assertEquals("but it counts one use", listOf("john@gmail.com"), s.book.addresses())
    }

    @Test fun nothingIsLearnedWhereLearningIsOff() {
        val p = Setup(FieldKind.PASSWORD); p.typeAll("john@gmail.com"); p.l.finishField(); assertTrue(p.book.isEmpty)
        val n = Setup(noLearn = true); n.typeAll("john@gmail.com"); n.l.finishField(); n.l.picked("john@gmail.com"); assertTrue(n.book.isEmpty)
    }

    @Test fun movingTheCursorOrDeletingBreaksTheTypedAddress() {
        val s = Setup(); s.typeAll("john@gmail.com"); s.r.act { moveCursor(-3) }; s.l.afterKey(); s.l.finishField(); assertTrue(s.book.isEmpty)
        val t = Setup(); t.typeAll("john@gmail.com"); repeat(4) { t.r.act { backspace() }; t.l.afterKey() }; t.l.finishField(); assertTrue("john@gmail is no address any more", t.book.isEmpty)
    }

    @Test fun typedEmailAndPickAddressWorkOnTheText() {
        val s = Setup(); s.typeAll("jo"); assertEquals("jo", s.r.c.addressPrefix()); assertNull(s.r.c.typedEmail())
        assertTrue(s.r.c.pickAddress(2, "john@gmail.com")); s.r.sync(); s.r.check(); assertEquals("john@gmail.com", s.r.text)
        assertFalse("more characters than there are before the cursor", s.r.c.pickAddress(50, "x@y.com")); assertEquals("john@gmail.com", s.r.text)
    }

    @Test fun anAddressAfterTextIsReplacedOnlyFromItsStart() {
        val s = Setup(FieldKind.TEXT); s.typeAll("to: bob@w"); assertEquals("bob@w", s.r.c.addressPrefix())
        assertTrue(s.r.c.pickAddress(5, "bob@work.example")); s.r.sync(); assertEquals("to: bob@work.example", s.r.text)
    }

    // ---- R34b (owner 2026-11-26: "it still isn't recognizing email fields"): in an ordinary box too, when the box is left

    @Test fun anAddressTypedInAnOrdinaryBoxIsLearnedWhenTheBoxIsLeft() {
        val s = Setup(FieldKind.TEXT); s.typeAll("john@gmail.com")
        assertTrue("not yet: nothing ended it", s.book.isEmpty); s.l.finishField()
        assertEquals(listOf("john@gmail.com"), s.book.addresses())
    }

    @Test fun aSearchBoxCountsTooAndAnAddressBarOrANumberBoxDoesNot() {
        val q = Setup(FieldKind.SEARCH); q.typeAll("bob@work.example"); q.l.finishField(); assertEquals(listOf("bob@work.example"), q.book.addresses())
        for (k in listOf(FieldKind.URL, FieldKind.OMNIBOX, FieldKind.NUMBER, FieldKind.PHONE)) { val s = Setup(k); s.typeAll("john@gmail.com"); s.l.finishField(); assertTrue("$k learns no address", s.book.isEmpty) }
    }

    @Test fun anAtMentionWithNothingBeforeItIsNoAddress() {
        val s = Setup(FieldKind.TEXT); s.typeAll("hello @bob how are you"); s.l.finishField(); assertTrue(s.book.isEmpty)
    }

    @Test fun finishingTheBoxTwiceLearnsItOnce() {
        val s = Setup(FieldKind.TEXT); s.typeAll("john@gmail.com"); s.l.finishField(); s.l.finishField(); assertEquals(1, s.book.suggest("").size)
        assertEquals("one use only", listOf("john@gmail.com"), s.book.addresses())
    }
}
