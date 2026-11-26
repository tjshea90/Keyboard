package app.kestrel.core.input.glide

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** R32c: the memory of the e-mail addresses this person types. */
class EmailBookTest {
    @Test fun onlyPlainAddressesAreKept() {
        val b = EmailBook()
        for (ok in listOf("a@b.co", "john.smith+tag@mail.example.com", "x_y-z@sub-domain.org")) assertTrue(ok, b.learn(ok))
        for (bad in listOf("", "a@b", "@b.com", "a@.com", "a b@c.com", "john@@c.com", ".a@c.com", "a.@c.com", "a..b@c.com", "a@c..com", "a@c.c", "no at sign.com", "http://x.com", "a@" + "b".repeat(70) + ".com"))
            assertFalse("'$bad' must not be kept", EmailBook().learn(bad))
        assertEquals(3, b.size)
    }

    @Test fun capitalsAreFoldedAndTheMostUsedComesFirst() {
        val b = EmailBook(); b.learn("Me@Gmail.com"); b.learn("work@corp.example"); b.learn("me@gmail.com")
        assertEquals(listOf("me@gmail.com", "work@corp.example"), b.addresses())
    }

    @Test fun ofEquallyUsedAddressesTheNewestComesFirst() {
        val b = EmailBook(); b.learn("a@one.com"); b.learn("b@two.com"); assertEquals(listOf("b@two.com", "a@one.com"), b.addresses())
        b.learn("a@one.com"); b.learn("b@two.com"); b.learn("a@one.com"); b.learn("b@two.com")
        assertEquals("equal uses: the one used last", listOf("b@two.com", "a@one.com"), b.addresses())
    }

    @Test fun anEmptyBoxOffersTheBestThree() {
        val b = EmailBook(); for (a in listOf("a@x.com", "b@x.com", "c@x.com", "d@x.com")) b.learn(a); b.learn("a@x.com")
        assertEquals(listOf("a@x.com", "d@x.com", "c@x.com"), b.suggest(""))
    }

    @Test fun whatIsTypedNarrowsTheOffers() {
        val b = EmailBook(); b.learn("alice@gmail.com"); b.learn("alan@work.example"); b.learn("bob@gmail.com")
        assertEquals(setOf("alice@gmail.com", "alan@work.example"), b.suggest("al").toSet())
        assertEquals(listOf("alice@gmail.com"), b.suggest("ali"))
        assertEquals(listOf("alice@gmail.com"), b.suggest("ALI"))
        assertTrue(b.suggest("zed").isEmpty())
    }

    @Test fun anAddressTypedInFullIsNotOfferedAgain() {
        val b = EmailBook(); b.learn("alice@gmail.com"); assertTrue(b.suggest("alice@gmail.com").isEmpty())
    }

    @Test fun afterTheAtSignTheDomainsThisPersonUsesAreOffered() {
        val b = EmailBook(); b.learn("alice@gmail.com"); b.learn("alice@gmail.com"); b.learn("carol@work.example")
        assertEquals("his own domains first, then the first popular one", listOf("bob@gmail.com", "bob@work.example", "bob@outlook.com"), b.suggest("bob@"))
        assertEquals(listOf("bob@work.example"), b.suggest("bob@wo"))
        assertTrue("not for a second @", b.suggest("bob@x@").isEmpty())
        assertTrue("not for a local part that is no address", b.suggest("b o b@").isEmpty())
    }

    @Test fun theBookHoldsAtMostItsCapAndDropsTheLeastUsed() {
        val b = EmailBook(3); b.learn("a@x.com"); b.learn("a@x.com"); b.learn("b@x.com"); b.learn("c@x.com"); b.learn("d@x.com")
        assertEquals(3, b.size); assertTrue("the often used one stays", "a@x.com" in b.addresses()); assertFalse("the oldest single use went", "b@x.com" in b.addresses())
    }

    @Test fun saveAndLoadKeepEverythingAndOrder() {
        val b = EmailBook(); b.learn("a@x.com"); b.learn("b@y.org"); b.learn("b@y.org")
        val again = EmailBook.parse(b.serialize()); assertEquals(b.addresses(), again.addresses())
        again.learn("c@z.net"); assertEquals("a use after loading is newer than everything loaded", "c@z.net", again.addresses()[1])
    }

    @Test fun aDamagedFileGivesWhatCanBeRead() {
        val b = EmailBook.parse("good@x.com\t2\t5\nnot an address\t3\t1\n\n\tgarbage\nother@y.org\tnope\tnope")
        assertEquals(listOf("good@x.com", "other@y.org"), b.addresses())
        assertTrue(EmailBook.parse(null).isEmpty); assertTrue(EmailBook.parse("   ").isEmpty)
    }

    @Test fun forgetAndClearRemoveAndMarkTheBookChanged() {
        val b = EmailBook(); b.learn("a@x.com"); b.learn("b@x.com"); b.serialize(); assertFalse(b.dirty)
        assertTrue(b.forget("A@x.com")); assertTrue(b.dirty); assertEquals(listOf("b@x.com"), b.addresses())
        assertFalse(b.forget("missing@x.com")); b.serialize(); b.clear(); assertTrue(b.dirty); assertTrue(b.isEmpty)
    }

    // ---- R34c (owner 2026-11-26): "after the @ sign, suggest popular email domains such as outlook.com, gmail.com and proton.me"

    @Test fun withNothingSavedTheAtSignOffersOutlookGmailAndProton() {
        assertEquals(listOf("tj@outlook.com", "tj@gmail.com", "tj@proton.me"), EmailBook().suggest("tj@"))
        assertEquals("the popular list is the owner's order, then the other big ones", listOf("outlook.com", "gmail.com", "proton.me", "hotmail.com", "yahoo.com", "icloud.com"), EmailBook.POPULAR_DOMAINS.take(6))
    }

    @Test fun whatIsTypedOfTheDomainNarrowsTheOffers() {
        val b = EmailBook()
        assertEquals(listOf("tj@gmail.com"), b.suggest("tj@g")); assertEquals(listOf("tj@gmail.com"), b.suggest("tj@gm"))
        assertEquals(listOf("tj@proton.me", "tj@protonmail.com"), b.suggest("tj@pro")); assertEquals(listOf("tj@outlook.com"), b.suggest("tj@out"))
        assertEquals(listOf("tj@hotmail.com"), b.suggest("tj@ho")); assertEquals(listOf("tj@gmail.com"), b.suggest("tj@gmail.co"))
        assertTrue(b.suggest("tj@zzz").isEmpty()); assertTrue("a finished address is not offered again", b.suggest("tj@gmail.com").isEmpty())
    }

    @Test fun thePartBeforeTheAtSignKeepsItsCapitalsAndDots() {
        assertEquals(listOf("TJ.Shea+x@outlook.com", "TJ.Shea+x@gmail.com", "TJ.Shea+x@proton.me"), EmailBook().suggest("TJ.Shea+x@"))
    }

    @Test fun aDomainThePersonUsesComesBeforeThePopularOnes() {
        val b = EmailBook(); b.learn("me@proton.me"); b.learn("me@proton.me"); b.learn("old@yahoo.com")
        assertEquals(listOf("x@proton.me", "x@yahoo.com", "x@outlook.com"), b.suggest("x@"))
        assertEquals(listOf("x@proton.me", "x@protonmail.com"), b.suggest("x@pro"))
    }

    @Test fun anAtSignWithNothingBeforeItOrAnythingElseWrongOffersNothing() {
        val b = EmailBook()
        assertTrue("a mention, not an address", b.suggest("@bob").isEmpty()); assertTrue(b.suggest("@").isEmpty())
        assertTrue(b.suggest("a b@").isEmpty()); assertTrue(b.suggest("bob@@").isEmpty()); assertTrue(b.suggest("bob@x@").isEmpty())
        assertTrue("too long", b.suggest("x".repeat(70) + "@").isEmpty())
    }

    @Test fun aSavedAddressAndADomainOfferDoNotRepeatEachOther() {
        val b = EmailBook(); b.learn("tj@outlook.com"); b.learn("tj@outlook.com")
        assertEquals(listOf("tj@outlook.com", "tj@gmail.com", "tj@proton.me"), b.suggest("tj@"))
    }
}
