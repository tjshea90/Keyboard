package app.kestrel.core.translate

import java.util.Locale

/**
 * Friendly "tu" guard (plan 9.6, owner decision). English "you" has no formality, so a service may answer a casual question with a
 * formal "usted" form. This only DETECTS clear markers; it never rewrites verbs (rewriting breaks sentences). The engine uses it to
 * ask the other service once; if both answer formally the translation is shown unchanged and the answer is flagged for a counter.
 */
object FriendlyTu {
    /** Verbs that, right after an opening question mark, are a formal "you" question (puede, quiere, tiene, ...). */
    private val FORMAL_QUESTION_VERBS: Set<String> = hashSetOf(
        "puede", "pueden", "quiere", "quieren", "tiene", "tienen", "podria", "podrian", "desea", "desean", "sabe", "saben", "viene", "vienen",
        "necesita", "necesitan", "gustaria", "conoce", "conocen", "habla", "hablan", "trabaja", "trabajan", "vive", "viven", "come", "comen",
        "maneja", "manejan", "va", "van", "esta", "estan", "recuerda", "recuerdan", "prefiere", "prefieren", "piensa", "piensan",
        "cree", "creen", "hace", "hacen", "dice", "dicen", "ve", "ven", "acompana", "acompanan", "ayuda", "ayudan", "llama", "llaman", "espera",
        "esperan", "trae", "traen", "pasa", "pasan", "pudo", "pudieron", "quisiera", "quisieran", "sera", "seria"
    )

    private val USTED = Regex("(?iu)(^|[^\\p{L}])(usted|disculpe|perm\u00edtame|permitame)([^\\p{L}]|$)")

    /** Does the English talk to somebody ("you", "your", "u", an imperative question)? Only such texts are checked. */
    fun addressesYou(english: String): Boolean {
        val l = english.lowercase(Locale.ROOT)
        return Regex("(^|[^a-z])(you|your|yours|u|ur|you're|youre|y'all|ya)([^a-z]|$)").containsMatchIn(l)
    }

    /** Does this Spanish read formal where the English was casual? */
    fun looksFormal(english: String, spanish: String): Boolean {
        if (!addressesYou(english)) return false
        return containsFormalMarker(spanish)
    }

    fun containsFormalMarker(spanish: String): Boolean {
        val s = spanish.lowercase(Locale.forLanguageTag("es"))
        if (USTED.containsMatchIn(s)) return true
        // "¿Puede ...", "¿Quiere ...": opening question mark (or start of a sentence after it) then a formal verb
        var i = 0
        while (i < s.length) {
            val q = s.indexOf('¿', i)
            if (q < 0) break
            var j = q + 1
            while (j < s.length && s[j] == ' ') j++
            var e = j
            while (e < s.length && Character.isLetter(s[e])) e++
            if (e > j && FORMAL_QUESTION_VERBS.contains(Translation.Verbs.plain(s.substring(j, e)))) return true
            i = q + 1
        }
        return false
    }
}
