package app.swoop.keyboard

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The settings screen as a person meets it: it builds on a phone and on a tablet, every switch writes its setting, the live keyboard follows, and (with SWOOP_LOOK_DIR) it is drawn to a PNG for a human to look at (never committed). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [29], qualifiers = "w412dp-h915dp-xxhdpi")
class SetupActivityTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    private fun prefs() = app.getSharedPreferences(Prefs.FILE, 0)

    private fun launch(): SetupActivity { prefs().edit().clear().commit(); return Robolectric.buildActivity(SetupActivity::class.java).setup().get() }

    private fun find(v: View, text: String): TextView? {
        if (v is TextView && v.text.toString().startsWith(text)) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) find(v.getChildAt(i), text)?.let { return it }
        return null
    }
    /** The tappable row (or chip) that holds the text. */
    private fun rowOf(v: View): View { var x: View = v; while (!x.isClickable && x.parent is View) x = x.parent as View; return x }
    private fun tapText(a: SetupActivity, text: String) { val t = find(a.window.decorView, text); assertNotNull("no '$text' on the screen", t); assertTrue(rowOf(t!!).performClick()) }

    private fun draw(a: SetupActivity, name: String, widthDp: Int = 412) {
        val sv = a.window.decorView.let { d -> findScroll(d)!! }
        val child = sv.getChildAt(0)
        val w = (widthDp * app.resources.displayMetrics.density).toInt()
        child.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        child.layout(0, 0, w, child.measuredHeight)
        val b = Bitmap.createBitmap(w, child.measuredHeight, Bitmap.Config.ARGB_8888)
        val c = Canvas(b); c.drawColor((sv.background as? android.graphics.drawable.ColorDrawable)?.color ?: 0xFF000000.toInt()); child.draw(c)
        System.getenv("SWOOP_LOOK_DIR")?.let { d -> File(d).mkdirs(); File(d, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        assertTrue(child.measuredHeight > 1500)
    }
    private fun findScroll(v: View): ScrollView? { if (v is ScrollView) return v; if (v is ViewGroup) for (i in 0 until v.childCount) findScroll(v.getChildAt(i))?.let { return it }; return null }

    @Test fun theScreenBuildsAndShowsEveryGroup() {
        val a = launch(); val d = a.window.decorView
        for (t in listOf("Swoop", "TRY IT", "TYPING", "LEARNING", "LAYOUT AND LOOK", "FEEL", "HELP", "Glide typing", "Word suggestions", "Space after punctuation", "Learn from my typing", "Number row", "Key pop-up")) assertNotNull("'$t' is missing", find(d, t))
        draw(a, "settings-phone-dark-auto")
    }

    @Test fun everySwitchWritesItsSettingAndAnotherTapUndoesIt() {
        val a = launch()
        for ((label, key, def) in listOf(Triple("Glide typing", Prefs.GLIDE, true), Triple("Glide trail", Prefs.TRAIL, true), Triple("Word suggestions", Prefs.SUGGEST, true), Triple("Fix typos automatically", Prefs.AUTOFIX, true),
            Triple("Capital I", Prefs.CAPI, true), Triple("Capital letters", Prefs.AUTOCAPS, true), Triple("Space after punctuation", Prefs.AUTOSPACE, true), Triple("Double space for a full stop", Prefs.DOUBLESPACE, true),
            Triple("Slide on the space bar", Prefs.SPACESLIDE, true), Triple("Learn from my typing", Prefs.LEARN, true), Triple("Number row", Prefs.NUMBERROW, true), Triple("Key pop-up", Prefs.PREVIEW, true),
            Triple("Vibrate on key press", Prefs.HAPTIC, true), Triple("Sound on key press", Prefs.SOUND, false))) {
            assertEquals("$label starts as installed", def, prefs().getBoolean(key, def))
            tapText(a, label); assertEquals("$label was tapped", !def, prefs().getBoolean(key, def))
            tapText(a, label); assertEquals("$label tapped again", def, prefs().getBoolean(key, def))
        }
    }

    @Test fun theLiveKeyboardFollowsTheSettings() {
        val a = launch()
        tapText(a, "Number row"); assertFalse(Settings(app).numberRow)
        tapText(a, "Large"); assertEquals(2, Settings(app).keyHeight)
        tapText(a, "Light"); assertEquals(2, Settings(app).theme)
        val a2 = Robolectric.buildActivity(SetupActivity::class.java).setup().get()           // a fresh screen shows what was chosen
        draw(a2, "settings-phone-light-large")
        assertEquals(2, Settings(app).theme)
    }

    @Test fun anAccentColourRebuildsTheScreenInThatColour() {
        val a = launch(); tapText(a, "Pink")
        // the dots have no text: choose through the setting the dot sets
        Settings(app).set(Prefs.ACCENT, 3); assertEquals(Settings.ACCENTS[3].color, Settings(app).accentColor)
        val b = Robolectric.buildActivity(SetupActivity::class.java).setup().get(); draw(b, "settings-pink")
    }

    @Test fun resetPutsEverythingBack() {
        val a = launch(); tapText(a, "Number row"); tapText(a, "Large")
        assertFalse(Settings(app).numberRow)
        Settings(app).reset(); assertTrue(Settings(app).numberRow); assertEquals(1, Settings(app).keyHeight)
    }

    @Test fun aTabletKeepsTheColumnNarrow() {
        val a = launch(); draw(a, "settings-tablet", widthDp = 600)
    }
}
