package app.kestrel.core.translate

import app.kestrel.core.translate.TranslateFlow.Do
import app.kestrel.core.translate.TranslateFlow.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class TranslateFlowTest {
    private fun ok(t: String, by: String = "MyMemory") = TranslateResult.ok(t, by)
    private fun fail(code: ErrorCode, why: String = "nope") = TranslateResult.failed(why, false, "", code)
    private fun on(auto: Long = 0, edits: Boolean = false) = TranslateFlow(auto, edits).also { it.setOn(true, 0) }

    @Test fun offEnterIsTheNormalEnter() {
        val f = TranslateFlow()
        assertEquals(State.OFF, f.state)
        assertEquals(Do.SEND, f.enter("hello", 0))
        assertEquals(State.OFF, f.state)
    }

    @Test fun theWholeHappyPath() {
        val f = on()
        assertEquals(State.IDLE, f.state)
        assertEquals(Do.TRANSLATE, f.enter("Can you drive?", 1))
        val n = f.start("Can you drive?", 1)
        assertEquals(State.TRANSLATING, f.state)
        assertEquals(Do.NOTHING, f.enter("Can you drive?", 2)) // a second Enter while waiting does nothing
        assertEquals("¿Sabes manejar?", f.answer(n, "Can you drive?", ok("¿Sabes manejar?"), 3))
        assertEquals(State.TRANSLATED, f.state)
        assertEquals("Can you drive?", f.english())
        assertEquals("MyMemory", f.by())
        assertEquals(Do.SEND, f.enter("¿Sabes manejar?", 4))
        f.sent()
        assertEquals(State.IDLE, f.state)
        assertTrue(f.isOn())
        assertNull(f.english())
    }

    @Test fun failureLeavesTheEnglishAndTheNextEnterTriesAgain() {
        val f = on()
        val n = f.start("Hello there", 0)
        assertNull(f.answer(n, "Hello there", fail(ErrorCode.NO_INTERNET, "no net"), 1))
        assertEquals(State.IDLE, f.state)
        assertEquals("no net", f.why())
        assertEquals(ErrorCode.NO_INTERNET, f.lastError())
        assertEquals(Do.TRANSLATE, f.enter("Hello there", 2))
        f.start("Hello there", 2)
        assertEquals("", f.why()) // cleared by the new request
    }

    @Test fun failureWithoutAReasonStillSaysSomething() {
        val f = on()
        val n = f.start("Hello", 0)
        assertNull(f.answer(n, "Hello", TranslateResult.failed("", false, "", ErrorCode.NONE), 1))
        assertEquals("The text could not be translated.", f.why())
        assertEquals(ErrorCode.UNEXPECTED, f.lastError())
    }

    @Test fun aChangedFieldDropsTheAnswer() {
        val f = on()
        val n = f.start("Hello", 0)
        assertNull(f.answer(n, "Hello world", ok("Hola"), 1))
        assertEquals(ErrorCode.FIELD_CHANGED, f.lastError())
        assertEquals(State.IDLE, f.state)
    }

    @Test fun aStaleAnswerIsIgnored() {
        val f = on()
        val n1 = f.start("one", 0)
        assertTrue(f.fieldChanged("one two", 1)) // the request is stale: cancel it
        assertEquals(State.IDLE, f.state)
        assertNull(f.answer(n1, "one two", ok("uno"), 2))
        assertEquals(State.IDLE, f.state)
        val n2 = f.start("one two", 3)
        assertNull(f.answer(n1, "one two", ok("uno"), 4)) // the old number never matches the new request
        assertEquals("uno dos", f.answer(n2, "one two", ok("uno dos"), 5))
    }

    @Test fun editingTheSameTextWhileWaitingDoesNotCancel() {
        val f = on()
        f.start("same", 0)
        assertFalse(f.fieldChanged("same", 1))
        assertEquals(State.TRANSLATING, f.state)
    }

    @Test fun undoGivesTheEnglishBack() {
        val f = on()
        val n = f.start("See you", 0)
        f.answer(n, "See you", ok("Nos vemos"), 1)
        assertEquals("See you", f.undo())
        assertEquals(State.IDLE, f.state)
        assertNull(f.undo())
    }

    @Test fun sendMyEnglish() {
        val f = on()
        val n = f.start("See you", 0)
        f.answer(n, "See you", ok("Nos vemos"), 1)
        assertEquals("See you", f.sendEnglish("Nos vemos"))
        assertEquals(State.IDLE, f.state)
        assertEquals("as is", f.sendEnglish("as is")) // after a failure: the field as it is
    }

    @Test fun editedSpanishIsStillSentByDefault() {
        val f = on()
        val n = f.start("See you", 0)
        f.answer(n, "See you", ok("Nos vemos"), 1)
        assertFalse(f.fieldChanged("Nos vemos pronto", 2))
        assertEquals(State.TRANSLATED, f.state)
        assertEquals(Do.SEND, f.enter("Nos vemos pronto", 3))
    }

    @Test fun anyEditAfterTranslatingEndsTheCheckWhenAsked() {
        val f = on(edits = true)
        val n = f.start("See you", 0)
        f.answer(n, "See you", ok("Nos vemos"), 1)
        assertFalse(f.fieldChanged("Nos vemos", 2)) // the text the flow put in is not an edit
        assertEquals(State.TRANSLATED, f.state)
        f.fieldChanged("Nos vemos!", 3)
        assertEquals(State.IDLE, f.state)
        assertEquals(Do.TRANSLATE, f.enter("Nos vemos!", 4))
    }

    @Test fun emptyingTheFieldEndsTheCheck() {
        val f = on()
        val n = f.start("See you", 0)
        f.answer(n, "See you", ok("Nos vemos"), 1)
        f.fieldChanged("  ", 2)
        assertEquals(State.IDLE, f.state)
    }

    @Test fun nothingToTranslateIsTheNormalEnter() {
        val f = on()
        assertEquals(Do.SEND, f.enter("", 0))
        assertEquals(Do.SEND, f.enter("   \n", 0))
        assertEquals(Do.SEND, f.enter("123 😀", 0))
        assertEquals(State.IDLE, f.state)
    }

    @Test fun tooLongIsRefusedWithAReason() {
        val f = on()
        assertEquals(Do.TOO_LONG, f.enter("a".repeat(1001), 0))
        assertEquals(ErrorCode.TOO_LONG, f.lastError())
        assertTrue(f.why().startsWith("Too long"))
        assertEquals(Do.TRANSLATE, f.enter("a".repeat(1000), 0))
    }

    @Test fun autoOffAfterInactivity() {
        val f = on(auto = 600_000)
        assertFalse(f.tick(599_999))
        assertEquals(State.IDLE, f.state)
        assertEquals(Do.TRANSLATE, f.enter("Hello", 599_999)) // activity restarts the timer
        assertFalse(f.tick(1_000_000))
        assertTrue(f.tick(599_999 + 600_000))
        assertEquals(State.OFF, f.state)
        assertEquals(Do.SEND, f.enter("Hello", 2_000_000))
    }

    @Test fun enterAfterTheTimerRanOutIsAPlainEnter() {
        val f = on(auto = 1000)
        assertEquals(Do.SEND, f.enter("Hello", 5000))
        assertEquals(State.OFF, f.state)
    }

    @Test fun neverAutoOffWhenZero() {
        val f = on(auto = 0)
        assertFalse(f.tick(Long.MAX_VALUE / 2))
    }

    @Test fun turningOffForgetsEverythingAndALateAnswerChangesNothing() {
        val f = on()
        val n = f.start("Hello", 0)
        f.setOn(false, 1)
        assertNull(f.answer(n, "Hello", ok("Hola"), 2))
        assertEquals(State.OFF, f.state)
        f.setOn(true, 3)
        assertEquals(State.IDLE, f.state)
    }

    @Test fun leavingTheFieldForgetsButStaysOn() {
        val f = on()
        val n = f.start("Hello", 0)
        f.leaveField()
        assertNull(f.answer(n, "Hello", ok("Hola"), 1))
        assertTrue(f.isOn()); assertEquals(State.IDLE, f.state)
        val m = f.start("Bye", 2)
        f.answer(m, "Bye", ok("Adios"), 3)
        f.leaveField()
        assertNull(f.english())
        assertEquals(State.IDLE, f.state)
    }

    @Test fun randomEventsNeverBreakTheInvariants() {
        val rnd = Random(5)
        val f = TranslateFlow(autoOffMs = 50_000, editsEndCheck = rnd.nextBoolean())
        var now = 0L
        var field = "Hi"
        var pending = -1
        for (step in 0 until 20_000) {
            now += rnd.nextInt(3000)
            when (rnd.nextInt(10)) {
                0 -> f.setOn(rnd.nextBoolean(), now)
                1, 2, 3 -> when (f.enter(field, now)) {
                    Do.TRANSLATE -> pending = f.start(field, now)
                    Do.SEND -> f.sent()
                    else -> {}
                }
                4, 5 -> if (pending >= 0) {
                    val r = if (rnd.nextInt(4) == 0) fail(ErrorCode.TIMEOUT) else ok("es:$field")
                    val put = f.answer(pending, field, r, now)
                    if (put != null) field = put
                    pending = -1
                }
                6 -> { field = if (rnd.nextBoolean()) "" else "Hi " + rnd.nextInt(5); f.fieldChanged(field, now) }
                7 -> f.undo()?.let { field = it }
                8 -> f.leaveField()
                9 -> f.tick(now)
            }
            // invariants
            if (f.state == State.OFF) { assertNull(f.english()) }
            if (f.state == State.TRANSLATED) { assertTrue(f.english() != null) }
            if (f.state == State.TRANSLATING) { assertTrue(f.isOn()) }
        }
    }
}
