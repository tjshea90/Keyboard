package app.swoop.keyboard

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import app.kestrel.core.input.ImeLayout
import app.kestrel.core.input.Page
import app.kestrel.core.input.editor.FieldKind
import app.kestrel.core.input.editor.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * What the keys view really PAINTS (native graphics, real pixels): the keys exist where the touch code says they are, the lit key and the bubble show up,
 * the dark palette differs. With SWOOP_LOOK_DIR set the frames are also written there as PNG files (for a human to look at; they are never committed).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [29])
class KeysViewLookTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val dp get() = app.resources.displayMetrics.density

    private fun view(w: Int = 564, kind: FieldKind = FieldKind.TEXT, action: String = "Send"): KeysView {
        val v = KeysView(app); v.setField(kind, action)
        val px = (w * dp).toInt()
        v.measure(View.MeasureSpec.makeMeasureSpec(px, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        v.layout(0, 0, v.measuredWidth, v.measuredHeight)
        return v
    }
    private fun render(v: View, name: String): Bitmap {
        val b = Bitmap.createBitmap(v.width, v.height + 60, Bitmap.Config.ARGB_8888)
        val c = Canvas(b); c.drawColor(0xFF888888.toInt()); c.translate(0f, 60f); v.draw(c)    // 60 px of "app" above it so a bubble that overflows upward would be visible too
        System.getenv("SWOOP_LOOK_DIR")?.let { d -> File(d).mkdirs(); File(d, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        return b
    }
    private fun pixelAtKey(b: Bitmap, v: KeysView, label: String, dx: Float = 0f, dy: Float = 0f): Int { val (x, y) = v.centerOf(label)!!; return b.getPixel((x + dx).toInt(), (y + dy).toInt() + 60) }
    private fun corner(b: Bitmap) = b.getPixel(1, 61)          // the padding of the view: the background colour
    private fun touch(v: View, a: Int, x: Float, y: Float) { val t = SystemClock.uptimeMillis(); v.dispatchTouchEvent(MotionEvent.obtain(t, t, a, x, y, 0)) }

    @Test fun keysArePaintedExactlyWhereTouchesFindThem() {
        val v = view(); val b = render(v, "light-letters")
        val bg = corner(b)
        for (c in "qpazm") {
            val (x, y) = v.centerOf(c.toString())!!
            // just off the label glyph (the label is in the middle): inside the key near its left edge the colour is the key colour, not the background
            val p = b.getPixel((x - 12 * dp).toInt(), (y).toInt() + 60)
            assertNotEquals("key '$c' is not painted where touches find it", bg, p)
        }
        assertEquals("the pad around the keys is the background", 0xFFE4EDED.toInt(), bg)
    }

    @Test fun theActionKeyIsTheAccentColourAndSaysWhatEnterDoes() {
        val v = view(action = "Search"); val b = render(v, "light-search")
        assertEquals(0xFF00DDE6.toInt(), pixelAtKey(b, v, "Search", dx = 14 * dp, dy = -12 * dp))
    }

    @Test fun aPressedKeyLightsUpThenDims() {
        val v = view(); val (x, y) = v.centerOf("g")!!
        val before = pixelAtKey(render(v, "before"), v, "g", dx = -14 * dp)
        touch(v, MotionEvent.ACTION_DOWN, x, y)
        val during = pixelAtKey(render(v, "pressed-g"), v, "g", dx = -14 * dp)
        assertNotEquals("a pressed key must look pressed", before, during)
        touch(v, MotionEvent.ACTION_UP, x, y); shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(KeysView.FLASH_MS + 20))
        assertEquals(before, pixelAtKey(render(v, "after"), v, "g", dx = -14 * dp))
    }

    @Test fun theBubbleAppearsAboveAMiddleRowKeyAndShowsTheLetter() {
        val v = view(); val (x, y) = v.centerOf("h")!!
        val rest = render(v, "no-bubble")
        touch(v, MotionEvent.ACTION_DOWN, x, y)
        val b = render(v, "bubble-h")
        // the bubble sits in the row above (centre of the bubble, a key-height above the pressed key)
        val above = b.getPixel(x.toInt(), (y - 56 * dp).toInt() + 60)
        assertNotEquals("nothing was painted above the held key", rest.getPixel(x.toInt(), (y - 56 * dp).toInt() + 60), above)
        assertEquals("h", v.bubbleTextForTest())
    }

    @Test fun capitalsAppearOnTheKeysWhenShiftIsOn() {
        val v = view(); v.setShift(ShiftState.ONCE); v.requestLayout(); render(v, "light-shift")
        assertTrue(v.centerOf("Q") != null && v.centerOf("q") == null)
    }

    @Test fun theNumberKeypadAndUrlRowsAreDrawn() { render(view(kind = FieldKind.NUMBER), "light-number"); render(view(kind = FieldKind.URL, action = "Go"), "light-url") }

    @Test @Config(qualifiers = "night", sdk = [29]) fun theDarkPaletteIsDark() {
        val v = view(); val b = render(v, "dark-letters")
        assertEquals(0xFF0D1D1D.toInt(), corner(b))
    }


    /** Owner M1: the dark letters page IS the photo (Gboard, dark teal). The colours below were measured from the photo; the geometry is checked in ImeLayoutTest. */
    @Test @Config(qualifiers = "night-xxhdpi", sdk = [29]) fun theDarkLettersPageHasThePhotosColours() {
        val v = view(w = 412, action = "\u23CE"); val b = render(v, "photo-dark-phone")
        assertEquals("background", 0xFF0D1D1D.toInt(), corner(b))
        assertEquals("a letter key", 0xFF253535.toInt(), pixelAtKey(b, v, "w", dx = -12 * dp))
        assertEquals("the space bar", 0xFF253535.toInt(), pixelAtKey(b, v, "space", dx = 40 * dp))
        assertEquals("Shift is the lighter slate", 0xFF35494A.toInt(), pixelAtKey(b, v, "\u21E7", dx = -22 * dp))
        assertEquals("Backspace is the lighter slate", 0xFF35494A.toInt(), pixelAtKey(b, v, "\u232B", dx = 22 * dp))
        assertEquals("the comma key is the lighter slate", 0xFF35494A.toInt(), pixelAtKey(b, v, ",", dy = -14 * dp))
        assertEquals("the full stop key is the lighter slate", 0xFF35494A.toInt(), pixelAtKey(b, v, ".", dy = -14 * dp))
        assertEquals("?123 is the pale blue round key", 0xFFA4CEDC.toInt(), pixelAtKey(b, v, "?123", dx = -24 * dp))
        assertEquals("Enter is the cyan round key", 0xFF00DDE6.toInt(), pixelAtKey(b, v, "\u23CE", dx = -24 * dp))
        assertNull("no number row: the digits are hints", v.centerOf("5"))
        val rows = listOf("q", "a", "z", "space").map { v.centerOf(it)!!.second }
        assertTrue("four rows from the top: $rows", rows[0] < rows[1] && rows[1] < rows[2] && rows[2] < rows[3])
        val (qx, _) = v.centerOf("q")!!; val (zx, _) = v.centerOf("z")!!; val (sx, _) = v.centerOf("s")!!
        assertEquals("Z stands under S like the photo", sx, zx, 0.5f); assertTrue(qx < sx)
        // the round keys are really round: the corner of the Enter key's box is the background, its middle is not
        val (ex, ey) = v.centerOf("\u23CE")!!
        assertEquals("the corner of the Enter key's box is outside the round key", 0xFF0D1D1D.toInt(), b.getPixel((ex + 24 * dp).toInt(), (ey - 24 * dp).toInt() + 60))
    }

    @Test @Config(qualifiers = "night-xxhdpi", sdk = [29]) fun theSymbolsPagesAreDrawnToo() {
        val v = view(w = 412)
        val (px, py) = v.centerOf("?123")!!; fun tapAt(x: Float, y: Float) { touch(v, MotionEvent.ACTION_DOWN, x, y); touch(v, MotionEvent.ACTION_UP, x, y) }
        tapAt(px, py); render(v, "photo-dark-symbols1")
        val (mx, my) = v.centerOf(ImeLayout.MORE_SYMBOLS)!!; tapAt(mx, my); render(v, "photo-dark-symbols2")
        assertEquals(Page.SYMBOLS2, v.pageForTest())
    }

    @Test @Config(qualifiers = "night-xxhdpi", sdk = [29]) fun theTabletWidthIsDrawnToo() { render(view(w = 564), "photo-dark-tablet"); render(view(w = 564, action = "Search"), "photo-dark-tablet-search") }

    @Test fun thePhoneWidthStillGivesWholeKeys() { val v = view(w = 411); render(v, "light-phone"); val (x0, _) = v.centerOf("q")!!; val (x1, _) = v.centerOf("w")!!; assertTrue(x1 - x0 > 30 * dp) }
}
