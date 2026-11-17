package app.kestrel.core.input

import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.ImeActions
import app.kestrel.core.input.glide.GlideKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeLayoutTest {
    private fun keys(rows: List<List<KeySpec>>) = rows.flatten().mapNotNull { it.key }

    @Test fun everyLetterPageHasAllTwentySixLettersSoTheDecoderCanUseIt() {
        for (kind in listOf(FieldKind.TEXT, FieldKind.SEARCH, FieldKind.URL, FieldKind.EMAIL, FieldKind.PASSWORD)) for (shift in listOf(false, true)) {
            val rows = ImeLayout.rows(Page.LETTERS, shift, kind)
            val boxes = MiniKeyboardGeometry.boxes(rows, 564f, 220f, 3f)
            val gk = GlideKeys.from(rows, boxes)          // throws when a letter is missing
            assertEquals(26, gk.cx.size); assertTrue(gk.pitch > 40f)
        }
    }

    @Test fun theFirstThreeRowsAreTheBrowsersOwnSoTheEngineSeesTheSameGeometry() {
        val mine = ImeLayout.rows(Page.LETTERS, false, FieldKind.TEXT)
        val theirs = MiniKeyboardLayout.rows(false)
        for (i in 0..2) assertEquals(theirs[i].map { it.label to it.weight }, mine[i].map { it.label to it.weight })
    }

    @Test fun shiftMakesCapitalLetters() {
        val k = keys(ImeLayout.rows(Page.LETTERS, true, FieldKind.TEXT)).filterIsInstance<Key.Ch>().map { it.text }
        assertTrue("Q" in k && "M" in k && "q" !in k)
    }

    @Test fun theBottomRowDependsOnTheField() {
        fun bottom(kind: FieldKind) = ImeLayout.rows(Page.LETTERS, false, kind, "Go").last().map { it.label }
        assertEquals(listOf("globe", "123", ",", "space", ".", "Go"), bottom(FieldKind.TEXT))
        assertEquals(listOf("globe", "123", "/", "space", ".", ".com", "Go"), bottom(FieldKind.URL))
        assertEquals(listOf("globe", "123", "@", "space", ".", ".com", "Go"), bottom(FieldKind.EMAIL))
    }

    @Test fun everyBottomRowHasASpaceAndAnActionKeyAndNoLetterRowIsWiderThanTen() {
        for (kind in FieldKind.values()) for (page in Page.values()) {
            val rows = ImeLayout.rows(page, false, kind)
            val ks = keys(rows)
            if (kind != FieldKind.PHONE) assertTrue("$kind/$page needs a space", ks.any { it is Key.Ch && it.text == " " })   // a phone number has no spaces
            assertTrue("$kind/$page needs the action key", ks.any { it === Key.Go })
            assertTrue("$kind/$page needs backspace", ks.any { it === Key.Backspace })
            if (kind != FieldKind.NUMBER && kind != FieldKind.PHONE) for (r in rows) assertTrue(r.sumOf { it.weight.toDouble() } <= 10.001)
        }
    }

    @Test fun everyPageHasTheGlobeKeySoTheKeyboardCanAlwaysBeLeft() {
        for (kind in FieldKind.values()) for (page in Page.values()) assertTrue("$kind/$page", keys(ImeLayout.rows(page, false, kind)).any { it === Key.NextKeyboard })
    }

    @Test fun theSymbolsPageHasDigitsAndTheWayBack() {
        val rows = ImeLayout.rows(Page.SYMBOLS, false, FieldKind.TEXT)
        assertEquals("1234567890", rows[0].joinToString("") { it.label })
        assertTrue(keys(rows).any { it === Key.ToLetters }); assertTrue(keys(rows).none { it === Key.ToSymbols })
    }

    @Test fun numberAndPhoneFieldsGetAKeypadWhateverThePage() {
        val n = ImeLayout.rows(Page.LETTERS, true, FieldKind.NUMBER); assertEquals(4, n.size)
        assertEquals(listOf("1", "2", "3", "⌫"), n[0].map { it.label }); assertEquals("0", n[3][1].label)
        val p = ImeLayout.rows(Page.SYMBOLS, false, FieldKind.PHONE); assertEquals(listOf("globe", "0", ",", "⏎"), p[3].map { it.label })
        assertTrue(keys(n).filterIsInstance<Key.Ch>().none { it.text.length == 1 && it.text[0].isLetter() })
    }

    @Test fun theActionKeyIsLabelledForWhatEnterDoes() {
        assertEquals("Go", ImeLayout.actionLabel(ImeActions.GO, false)); assertEquals("Search", ImeLayout.actionLabel(ImeActions.SEARCH, false))
        assertEquals("Send", ImeLayout.actionLabel(ImeActions.SEND, false)); assertEquals("Next", ImeLayout.actionLabel(ImeActions.NEXT, false))
        assertEquals("Done", ImeLayout.actionLabel(ImeActions.DONE, false)); assertEquals("⏎", ImeLayout.actionLabel(ImeActions.UNSPECIFIED, false))
        assertEquals("⏎", ImeLayout.actionLabel(ImeActions.SEND, true)); assertNotNull(ImeLayout.actionLabel(99, false))
    }
}
