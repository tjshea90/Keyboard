package app.swoop.keyboard

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.view.inputmethod.InputMethodInfo
import app.kestrel.core.input.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** KB-100: the keyboard is declared the way Android needs it, the view types, the setup screen opens. Runs on the tablet's API level (29). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class KeysServiceTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()

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
        val exported = HashSet<String>()
        pi.activities?.filter { it.exported }?.forEach { exported.add(it.name) }
        pi.services?.filter { it.exported }?.forEach { exported.add(it.name) }
        pi.receivers?.filter { it.exported }?.forEach { exported.add(it.name) }
        pi.providers?.filter { it.exported }?.forEach { exported.add(it.name) }
        assertEquals(setOf("app.swoop.keyboard.SetupActivity", "app.swoop.keyboard.KeysService"), exported)
    }

    @Test fun noPermissionsAreRequested() {
        val pi = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
        assertTrue("the keyboard must request no permission yet (tools/expected-permissions.txt): ${pi.requestedPermissions?.toList()}", pi.requestedPermissions == null || pi.requestedPermissions!!.isEmpty())
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
