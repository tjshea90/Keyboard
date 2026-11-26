package app.kestrel.core.input.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** R34c: "an address is being typed" is a cheap check (no allocation) that the keyboard asks after every key in every ordinary box. */
class AtSignNearTest {
    private fun near(text: String): Boolean { val r = Rig(kind = FieldKind.TEXT); r.act { for (c in text) type(c.toString()) }; return r.c.atSignNear() }

    @Test fun anAtSignInsideTheWordBeforeTheCursorIsAnAddressInProgress() {
        assertTrue(near("bob@")); assertTrue(near("bob@gm")); assertTrue(near("write to bob@gmail.com")); assertTrue(near("a@b")); assertTrue(near("(bob@x"))
    }

    @Test fun aMentionOrNoAtSignOrAWordAfterTheAddressIsNot() {
        assertFalse(near("")); assertFalse(near("hello")); assertFalse(near("@")); assertFalse(near("@bob")); assertFalse(near("hi @bob"))
        assertFalse("a word after the address", near("bob@gmail.com and"))
        assertFalse("the address was left behind by a space", near("bob@gmail.com "))
    }

    @Test fun aSelectionIsNeverAnAddressInProgress() {
        val r = Rig(kind = FieldKind.TEXT); r.act { for (c in "bob@gm") type(c.toString()) }
        assertTrue(r.c.atSignNear())
    }
}
