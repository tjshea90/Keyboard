package app.kestrel.core.translate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The 30-sentence casual list (plan 9.6, KB-604). The Spanish is hand-written in the style MyMemory returns with onlyprivate=1 and
 * the friendly "tu" (a recorded-style sample, not a live call: no test touches the network).
 */
class FriendlyTuTest {
    private val casual = listOf(
        "Can you drive?" to "¿Sabes conducir?",
        "Do you want to eat?" to "¿Quieres comer?",
        "Are you coming?" to "¿Vienes?",
        "Can you pick me up at 5?" to "¿Puedes recogerme a las 5?",
        "Where are you?" to "¿Dónde estás?",
        "What are you doing?" to "¿Qué haces?",
        "Do you have the keys?" to "¿Tienes las llaves?",
        "Did you eat yet?" to "¿Ya comiste?",
        "Can you call me later?" to "¿Puedes llamarme más tarde?",
        "Are you busy?" to "¿Estás ocupado?",
        "Do you need anything?" to "¿Necesitas algo?",
        "Will you be home tonight?" to "¿Estarás en casa esta noche?",
        "Can you send me the address?" to "¿Puedes mandarme la dirección?",
        "Do you know what time it is?" to "¿Sabes qué hora es?",
        "How are you feeling?" to "¿Cómo te sientes?",
        "Did you see my message?" to "¿Viste mi mensaje?",
        "Do you want me to bring something?" to "¿Quieres que lleve algo?",
        "Are you okay?" to "¿Estás bien?",
        "Thank you so much" to "Muchas gracias a ti",
        "I love you" to "Te quiero",
        "Can you help me?" to "¿Me puedes ayudar?",
        "Do you remember me?" to "¿Te acuerdas de mí?",
        "What do you think?" to "¿Qué piensas?",
        "Have you eaten?" to "¿Ya comiste?",
        "You look great" to "Te ves muy bien",
        "Tell me when you get here" to "Dime cuando llegues",
        "Let me know if you can come" to "Avísame si puedes venir",
        "Do you like it?" to "¿Te gusta?",
        "Are you free tomorrow?" to "¿Estás libre mañana?",
        "Can you believe it?" to "¿Lo puedes creer?"
    )

    @Test fun thirtyCasualSentencesStayInTuForm() {
        assertEquals(30, casual.size)
        for ((en, es) in casual) assertFalse("$en -> $es", FriendlyTu.looksFormal(en, es))
    }

    @Test fun theSameSentencesInUstedFormAreFlagged() {
        val formal = mapOf(
            "Can you drive?" to "¿Puede conducir?",
            "Do you want to eat?" to "¿Quiere comer?",
            "Do you have the keys?" to "¿Tiene las llaves?",
            "Could you help me?" to "¿Podría ayudarme?",
            "Do you know the way?" to "¿Sabe el camino?",
            "Are you coming?" to "¿Usted viene?",
            "Excuse you" to "Disculpe, usted"
        )
        for ((en, es) in formal) assertTrue("$en -> $es", FriendlyTu.looksFormal(en, es))
        assertTrue(FriendlyTu.containsFormalMarker("Hola. ¿ Puede venir?"))
    }

    @Test fun textsThatDoNotAddressSomeoneAreNeverChecked() {
        assertFalse(FriendlyTu.looksFormal("Can he drive?", "¿Puede conducir?"))
        assertFalse(FriendlyTu.looksFormal("Is the store open?", "¿Está abierta la tienda?"))
        assertTrue(FriendlyTu.addressesYou("r u there"))
        assertFalse(FriendlyTu.addressesYou("a youth group"))
    }
}
