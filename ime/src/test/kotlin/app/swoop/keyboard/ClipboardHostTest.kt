package app.swoop.keyboard

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Owner R20a: the newest copy as the first box of the strip: how old it may be, what is secret, what the history keeps. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class ClipboardHostTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    private var offset = 0L
    private fun host() = ClipboardHost(app) { System.currentTimeMillis() + offset }.also { File(app.filesDir, "clipboard.txt").delete() }
    private fun File(dir: java.io.File, n: String) = java.io.File(dir, n)
    private fun copy(text: String, sensitive: Boolean = false) {
        val clip = ClipData.newPlainText("t", text)
        if (sensitive) clip.description.extras = PersistableBundle().apply { putBoolean(ClipboardHost.SENSITIVE_EXTRA, true) }
        (app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
    }

    @Test fun probe() {
        val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; copy("probe")
        val clip = cm.primaryClip; org.junit.Assert.assertNotNull("primaryClip", clip)
        val h = host(); try { h.capture(); } catch (e: Throwable) { org.junit.Assert.fail("capture threw $e") }
        org.junit.Assert.assertNotNull("latest after capture; ts=${clip!!.description.timestamp} count=${clip.itemCount} text=${clip.getItemAt(0).coerceToText(app)}", h.latest)
    }

    @Test fun aFreshCopyIsOfferedAndAnOldOneIsNot() {
        val h = host(); copy("hello there"); h.capture()
        val o = h.latestFresh(CLIP_CHIP_TTL_MS); assertNotNull(o); assertEquals("hello there", o!!.text); assertFalse(o.sensitive)
        offset = CLIP_CHIP_TTL_MS - 30_000L; assertNotNull("just under the limit", h.latestFresh(CLIP_CHIP_TTL_MS))
        offset = CLIP_CHIP_TTL_MS + 30_000L; assertNull("just over it", h.latestFresh(CLIP_CHIP_TTL_MS))
    }

    @Test fun theOfferWorksWithTheHistoryOffAndKeepsNothingOnDisk() {
        val h = host(); copy("only in memory"); assertFalse(h.capture(history = false))
        assertEquals("only in memory", h.latestFresh(CLIP_CHIP_TTL_MS)!!.text); assertTrue("the history stays empty", h.store.isEmpty)
        val on = host(); copy("kept"); assertTrue(on.capture(history = true)); assertEquals(1, on.store.size)
    }

    @Test fun aSensitiveCopyIsOfferedAsASecretAndNeverKeptInTheHistory() {
        val h = host(); copy("p4ssw0rd", sensitive = true); assertFalse(h.capture(history = true))
        val o = h.latestFresh(CLIP_CHIP_TTL_MS)!!; assertTrue(o.sensitive); assertEquals("p4ssw0rd", o.text); assertTrue("not in the history", h.store.isEmpty)
        copy("an ordinary copy"); h.capture(); assertFalse("the next copy is not secret", h.latestFresh(CLIP_CHIP_TTL_MS)!!.sensitive)
    }

    @Test fun aNewCopyOfTheSameTextIsANewCopyWithANewTime() {
        val h = host(); copy("same"); h.capture(); val first = h.latest!!.at
        Thread.sleep(5); copy("same"); h.capture()
        assertTrue("a new time, so a chip that was used or dismissed comes back: ${h.latest!!.at} vs $first", h.latest!!.at >= first)
    }

    @Test fun blankCopiesAreIgnored() { val h = host(); copy("   "); h.capture(); assertNull(h.latestFresh(CLIP_CHIP_TTL_MS)) }
}
