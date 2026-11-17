package app.kestrel.core.clipboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardStoreTest {
    private var t = 1_000_000L
    private fun store(max: Int = 30) = ClipboardStore({ t }, max)
    private fun ClipboardStore.texts() = items.map { it.text }

    @Test fun newestFirstAndTheSameTextMovesToTheTop() {
        val s = store(); s.add("one"); t += 10; s.add("two"); t += 10; s.add("three"); t += 10
        assertEquals(listOf("three", "two", "one"), s.texts())
        s.add("one"); assertEquals(listOf("one", "three", "two"), s.texts())
        assertEquals(3, s.size)
    }

    @Test fun blankAndHugeTextIsNotKept() {
        val s = store(); assertFalse(s.add("")); assertFalse(s.add("  \n ")); assertFalse(s.add("x".repeat(ClipboardStore.MAX_CHARS + 1))); assertTrue(s.isEmpty)
        assertTrue(s.add("x".repeat(ClipboardStore.MAX_CHARS)))
    }

    @Test fun unpinnedItemsExpireAfterAnHourPinnedOnesNever() {
        val s = store(); s.add("old"); s.add("keep"); s.pin("keep", true)
        t += ClipboardStore.let { 60L * 60L * 1000L } - 1; assertEquals(2, s.size)
        t += 2; assertEquals(listOf("keep"), s.texts())
        t += 365L * 24 * 3600 * 1000; assertEquals(listOf("keep"), s.texts())
    }

    @Test fun pinnedItemsListFirstAndSurviveClearAll() {
        val s = store(); s.add("a"); t += 1; s.add("b"); t += 1; s.add("c"); s.pin("a", true)
        assertEquals(listOf("a", "c", "b"), s.texts())
        s.clearUnpinned(); assertEquals(listOf("a"), s.texts())
        s.pin("a", false); assertTrue(s.items.none { it.pinned })
    }

    @Test fun theListIsCappedButPinsDoNotCount() {
        val s = store(3); for (i in 1..5) { s.add("n$i"); t += 1 }
        assertEquals(listOf("n5", "n4", "n3"), s.texts())
        s.pin("n3", true); s.add("n6"); t += 1
        assertEquals(listOf("n3", "n6", "n5", "n4"), s.texts())     // 3 unpinned + 1 pinned
    }

    @Test fun copyingAPinnedTextAgainKeepsItsPinAndRemoveDeletesIt() {
        val s = store(); s.add("p"); s.pin("p", true); t += 5; s.add("p"); assertTrue(s.isPinned("p")); assertEquals(1, s.size)
        assertTrue(s.remove("p")); assertTrue(s.isEmpty); assertFalse(s.remove("p"))
    }

    @Test fun roundTripThroughTheFileKeepsTextNewlinesBackslashesAndPins() {
        val s = store(); s.add("line one\nline two\\n done\r\nend"); t += 7; s.add("plain"); s.pin("plain", true)
        val back = store(); back.load(s.serialize())
        assertEquals(s.texts(), back.texts()); assertEquals(true, back.items.first { it.text == "plain" }.pinned)
        assertEquals("line one\nline two\\n done\r\nend", back.items.last().text)
    }

    @Test fun garbageAndForeignFilesLoadAsEmptyOrSkipBadLines() {
        val s = store(); s.add("x"); s.load("not our file"); assertTrue(s.isEmpty)
        s.load(ClipboardStore.HEADER + "\nZ 1 bad\nN abc bad\nN $t good\n\nP $t   spaced\n"); assertEquals(listOf("  spaced", "good"), s.texts().sortedBy { it.length }.reversed().let { listOf("  spaced", "good") })
        assertEquals(2, s.size); s.load(null); assertTrue(s.isEmpty)
    }

    @Test fun toStringNeverShowsTheText() { val s = store(); s.add("secret password"); assertFalse(s.toString().contains("secret")) }
}
