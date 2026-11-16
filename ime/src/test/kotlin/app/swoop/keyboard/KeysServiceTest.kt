package app.swoop.keyboard

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodInfo
import androidx.test.core.app.ApplicationProvider
import app.kestrel.core.input.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** KB-100: the keyboard is declared the way Android needs it, the view types, the setup screen opens. Runs on the tablet's API level (29). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class KeysServiceTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Test fun serviceIsDeclaredAsAnInputMethod() {
        val found = app.packageManager.queryIntentServices(Intent("android.view.InputMethod"), PackageManager.GET_META_DATA)
        val mine = found.firstOrNull { it.serviceInfo.packageName == app.packageName }
        assertNotNull("no input method service for this package", mine)
        assertEquals("android.permission.BIND_INPUT_METHOD", mine!!.serviceInfo.permission)
        assertTrue("the service must be exported so the system can bind it", mine.serviceInfo.exported)
    }

    @Test fun methodXmlHasOneEnglishSubtypeAndNoSpanish() {
        val ri = app.packageManager.queryIntentServices(Intent("android.view.InputMethod"), PackageManager.GET_META_DATA).first { it.serviceInfo.packageName == app.packageName }
        val imi = InputMethodInfo(app, ri)
        assertEquals(1, imi.subtypeCount)
        assertEquals("en_US", imi.getSubtypeAt(0).locale)
        assertEquals("keyboard", imi.getSubtypeAt(0).mode)
        assertEquals("app.swoop.keyboard.SetupActivity", imi.settingsActivity)
    }

    @Test fun onlySetupAndServiceAreExported() {
        val pi = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS)
        val exported = ArrayList<String>()
        pi.activities?.filter { it.exported }?.forEach { exported.add(it.name) }
        pi.services?.filter { it.exported }?.forEach { exported.add(it.name) }
        pi.receivers?.filter { it.exported }?.forEach { exported.add(it.name) }
        pi.providers?.filter { it.exported }?.forEach { exported.add(it.name) }
        assertEquals(listOf("app.swoop.keyboard.SetupActivity", "app.swoop.keyboard.KeysService"), exported.sorted().reversed().sortedBy { if (it.endsWith("SetupActivity")) 0 else 1 })
    }

    @Test fun noPermissionsAreRequested() {
        val pi = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
        assertTrue("the keyboard must request no permission yet (tools/expected-permissions.txt): ${pi.requestedPermissions?.toList()}", pi.requestedPermissions == null || pi.requestedPermissions!!.isEmpty())
    }

    private class Rec : KeysView.Listener { val keys = ArrayList<Key>(); override fun onKey(key: Key) { keys.add(key) } }

    private fun layout(v: KeysView, w: Int = 1080) {
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        v.layout(0, 0, v.measuredWidth, v.measuredHeight)
    }

    private fun tap(v: View, x: Float, y: Float) {
        val t = SystemClock.uptimeMillis()
        v.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0))
        v.dispatchTouchEvent(MotionEvent.obtain(t, t + 40, MotionEvent.ACTION_UP, x, y, 0))
    }

    @Test fun tappingTheMiddleOfTheTopLeftKeyTypesQ_andTheLastKeyOfRowOneP() {
        val v = KeysView(app); val rec = Rec(); v.listener = rec; layout(v)
        val pad = 4f * app.resources.displayMetrics.density
        val rowH = 50f * app.resources.displayMetrics.density
        tap(v, pad + 5f, pad + rowH / 2)                 // inside the first key of row 1
        tap(v, v.width - pad - 5f, pad + rowH / 2)       // inside the last key of row 1
        assertEquals(listOf("q", "p"), rec.keys.map { (it as Key.Ch).text })
    }

    @Test fun shiftMakesTheNextLetterCapitalThenGoesBack() {
        val v = KeysView(app); val rec = Rec(); v.listener = rec; layout(v)
        val d = app.resources.displayMetrics.density
        val pad = 4f * d; val rowH = 50f * d; val gap = 4f * d
        tap(v, pad + 8f, pad + 2 * (rowH + gap) + rowH / 2)           // the shift key (first of row 3)
        assertTrue("shift must not be reported to the listener", rec.keys.isEmpty())
        tap(v, pad + 5f, pad + rowH / 2)                              // q
        val q = rec.keys.single() as Key.Ch
        assertEquals("Q", v.labelOf(q.let { Key.Ch(it.text.uppercase()) }).uppercase())
        tap(v, pad + 5f, pad + rowH / 2)                              // q again, shift is spent
        assertEquals(2, rec.keys.size)
    }

    @Test fun serviceCreatesItsViewAndIgnoresKeysWithoutAnEditor() {
        val s = Robolectric.setupService(KeysService::class.java)
        val view = s.onCreateInputView()
        assertTrue(view is KeysView)
        s.onKey(Key.Ch("a")); s.onKey(Key.Backspace); s.onKey(Key.Go)   // no input connection: must not crash
        assertTrue(!s.onEvaluateFullscreenMode())
    }

    @Test fun setupActivityOpensAndTellsWhereTheOwnerStands() {
        val a = Robolectric.buildActivity(SetupActivity::class.java).setup().get()
        assertEquals("Set up Swoop", a.title.toString())
        assertTrue(!SetupActivity.isEnabled(a) && !SetupActivity.isCurrent(a))
    }
}
