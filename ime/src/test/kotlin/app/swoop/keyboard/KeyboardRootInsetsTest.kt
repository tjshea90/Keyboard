package app.swoop.keyboard

import android.app.Application
import android.graphics.Insets
import android.view.View
import android.view.WindowInsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Android 15+ draws the navigation bar OVER the keyboard window (edge to edge). The Android 16 emulator run of 2026-11-17 showed the bottom row (?123, space, Enter) sitting under the bar. The keys must stand above
 * the bar's height, and the keyboard's colour must fill the strip behind it (the bar is see-through). Android 10 (the tablet) reports no overlap and must look exactly as before.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class KeyboardRootInsetsTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()

    private fun heightOf(root: KeyboardRoot): Int {
        root.measure(View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        return root.measuredHeight
    }
    private fun bar(px: Int) = WindowInsets.Builder().setInsets(WindowInsets.Type.navigationBars(), Insets.of(0, 0, 0, px)).build()

    @Test fun theKeysStandAboveTheNavigationBarAndTheKeyboardColourFillsBehindIt() {
        val root = KeyboardRoot(app)
        val plain = heightOf(root)
        root.dispatchApplyWindowInsets(bar(96))
        assertEquals("the window grew by the bar's height: the keys are above it", plain + 96, heightOf(root))
        val panel = root.getChildAt(0)
        assertNotNull("the keyboard's colour is painted under the bar too", panel.background)
        assertEquals(96, panel.paddingBottom)
        root.dispatchApplyWindowInsets(bar(0))
        assertEquals("and back when the bar goes (a different navigation mode)", plain, heightOf(root)); assertNull(panel.background)
    }

    @Test fun aFloatingKeyboardStaysAboveTheBarToo() {
        val root = KeyboardRoot(app); root.dispatchApplyWindowInsets(bar(96)); root.requestFloating(true)
        assertTrue(root.floating)
        root.measure(View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1280, View.MeasureSpec.EXACTLY)); root.layout(0, 0, 800, 1280)
        assertTrue("the panel's bottom (${root.panelRect().bottom}) is above the bar that starts at ${1280 - 96}", root.panelRect().bottom <= 1280 - 96)
    }
}
