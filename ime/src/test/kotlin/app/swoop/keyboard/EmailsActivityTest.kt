package app.swoop.keyboard

import android.app.Application
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** R34b: the screen where the person types their own e-mail addresses once. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class EmailsActivityTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()

    private fun all(v: View, out: MutableList<View> = ArrayList()): MutableList<View> { out.add(v); if (v is ViewGroup) for (i in 0 until v.childCount) all(v.getChildAt(i), out); return out }
    private fun texts(a: EmailsActivity) = all(a.window.decorView).filterIsInstance<TextView>().filter { it !is EditText }.map { it.text.toString() }
    private fun button(a: EmailsActivity, label: String) = all(a.window.decorView).filterIsInstance<TextView>().first { it.text.toString() == label }

    @Test fun anAddressCanBeAddedAndTakenOutAgainAndItReachesTheKeyboardAtOnce() {
        val book = Engine.host(app).emails; book.clear()
        val a = Robolectric.buildActivity(EmailsActivity::class.java).setup().get()
        assertTrue("nothing yet", texts(a).any { it.startsWith("None yet") })
        val input = all(a.window.decorView).filterIsInstance<EditText>().first()
        input.setText("  TJ@Outlook.com "); button(a, "Add address").performClick()
        assertEquals(listOf("tj@outlook.com"), book.addresses()); assertEquals("the box is empty again", "", input.text.toString())
        assertTrue("it is listed", texts(a).contains("tj@outlook.com"))
        assertEquals("it leads the offers of an e-mail box from the first day", listOf("tj@outlook.com"), book.suggest(""))
        button(a, "Remove").performClick()
        assertTrue(book.isEmpty); assertTrue(texts(a).any { it.startsWith("None yet") })
    }

    @Test fun somethingThatIsNoAddressIsRefused() {
        val book = Engine.host(app).emails; book.clear()
        val a = Robolectric.buildActivity(EmailsActivity::class.java).setup().get()
        val input = all(a.window.decorView).filterIsInstance<EditText>().first()
        for (bad in listOf("", "john", "john@", "a b@c.com", "@x.com")) { input.setText(bad); button(a, "Add address").performClick() }
        assertTrue(book.isEmpty); assertFalse(texts(a).any { it.contains("@") && !it.contains("name@example") && !it.startsWith("Type your") && !it.startsWith("None yet") })
    }
}
