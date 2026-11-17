package app.kestrel.core.translate

import java.util.Locale

/**
 * Pure translation logic, ported from LinkMeter's Translation.java (read-only reference, commit 283d63ee): no Android, no sockets.
 * Builds the questions for the two services, reads their answers, cuts long texts into pieces, writes texting shorthand out and makes
 * Spanish right for Mexico. The connection itself lives in app.swoop.keyboard.translate.
 *
 * Services (docs/TRANSLATE.md): MyMemory first (machine translation only, onlyprivate=1, target es-MX), Google's unofficial
 * translate endpoint when MyMemory is used up, busy or out of reach.
 */
object Translation {
    /** Which way a text is translated. */
    enum class Way(val myMemoryFrom: String, val myMemoryTo: String, val googleFrom: String, val googleTo: String, val label: String) {
        /** The owner's English, for a friend in Mexico. */
        TO_MEXICAN("en", "es-MX", "en", "es", "Spanish (Mexico)"),

        /** A message the owner received, into English (Google finds the language by itself). */
        TO_ENGLISH("es-MX", "en", "auto", "en", "English")
    }

    private val ES: Locale = Locale.forLanguageTag("es")

    const val MYMEMORY = "MyMemory"
    const val GOOGLE = "Google Translate"

    /** The two hosts, and the only two the keyboard may ever talk to. */
    const val HOST_MYMEMORY = "api.mymemory.translated.net"
    const val HOST_GOOGLE = "translate.googleapis.com"

    /** MyMemory takes at most 500 bytes in one question; a piece stays below that. */
    const val MAX_PIECE_BYTES = 450

    /** Google takes much more (sent in the body, not the address). */
    const val MAX_GOOGLE_BYTES = 4000

    /** Longest text the keyboard translates in one go (plan 9.3). */
    const val MAX_CHARS = 1000

    // ------------------------------------------------------------------------------------------------------ pieces

    fun bytes(s: String): Int = s.toByteArray(Charsets.UTF_8).size

    /**
     * The text cut into pieces of at most [maxBytes] (UTF-8), at a line break where possible, else after the end of a sentence, else
     * between words, else between letters (never inside a character that takes two chars). Put back together they give the text exactly.
     */
    fun pieces(text: String?, maxBytes: Int): List<String> {
        val out = ArrayList<String>()
        if (text == null || text.isEmpty()) return out
        if (bytes(text) <= maxBytes) {
            out.add(text)
            return out
        }
        val cur = StringBuilder()
        for (unit in units(text)) {
            if (bytes(unit) > maxBytes) {
                flush(cur, out)
                for (word in words(unit)) {
                    if (bytes(word) > maxBytes) {
                        flush(cur, out)
                        cutLetters(word, maxBytes, out)
                    } else {
                        add(cur, word, maxBytes, out)
                    }
                }
            } else {
                add(cur, unit, maxBytes, out)
            }
        }
        flush(cur, out)
        return out
    }

    private fun add(cur: StringBuilder, part: String, maxBytes: Int, out: MutableList<String>) {
        if (bytes(cur.toString()) + bytes(part) > maxBytes) flush(cur, out)
        cur.append(part)
    }

    private fun flush(cur: StringBuilder, out: MutableList<String>) {
        if (cur.isNotEmpty()) {
            out.add(cur.toString())
            cur.setLength(0)
        }
    }

    /** Sentences and lines, each with the spaces and line breaks after it. */
    private fun units(text: String): List<String> {
        val out = ArrayList<String>()
        var start = 0
        var i = 0
        while (i < text.length) {
            val c = text[i]
            val end = c == '\n' || ((c == '.' || c == '!' || c == '?' || c == '…') && i + 1 < text.length && text[i + 1].isWhitespace())
            i++
            if (end) {
                while (i < text.length && text[i].isWhitespace()) i++
                out.add(text.substring(start, i))
                start = i
            }
        }
        if (start < text.length) out.add(text.substring(start))
        return out
    }

    /** Words, each with the spaces after it. */
    private fun words(text: String): List<String> {
        val out = ArrayList<String>()
        var start = 0
        var i = 0
        while (i < text.length) {
            val space = text[i].isWhitespace()
            i++
            if (space) {
                while (i < text.length && text[i].isWhitespace()) i++
                out.add(text.substring(start, i))
                start = i
            }
        }
        if (start < text.length) out.add(text.substring(start))
        return out
    }

    private fun cutLetters(word: String, maxBytes: Int, out: MutableList<String>) {
        val cur = StringBuilder()
        var i = 0
        while (i < word.length) {
            val cp = word.codePointAt(i)
            val one = String(Character.toChars(cp))
            if (bytes(cur.toString()) + bytes(one) > maxBytes && cur.isNotEmpty()) {
                out.add(cur.toString())
                cur.setLength(0)
            }
            cur.append(one)
            i += Character.charCount(cp)
        }
        if (cur.isNotEmpty()) out.add(cur.toString())
    }

    /** Does a piece hold anything to translate (letters)? Spaces, line breaks, emoji and numbers alone are kept as they are. */
    fun hasWords(s: String): Boolean {
        for (ch in s) if (Character.isLetter(ch)) return true
        return false
    }

    /** The spaces before, the words, the spaces after: a service may drop the spaces, which must stay where they were. */
    fun trimmed(s: String): Array<String> {
        var a = 0
        while (a < s.length && s[a].isWhitespace()) a++
        var b = s.length
        while (b > a && s[b - 1].isWhitespace()) b--
        return arrayOf(s.substring(0, a), s.substring(a, b), s.substring(b))
    }

    // ------------------------------------------------------------------------------------------------------ URLs

    private const val HEX = "0123456789ABCDEF"

    /** application/x-www-form-urlencoded, UTF-8 (written here so that the core imports no networking package). */
    fun enc(s: String): String {
        val sb = StringBuilder()
        for (b in s.toByteArray(Charsets.UTF_8)) {
            val c = b.toInt() and 0xff
            val ch = c.toChar()
            when {
                (ch in 'a'..'z') || (ch in 'A'..'Z') || (ch in '0'..'9') || ch == '.' || ch == '-' || ch == '*' || ch == '_' -> sb.append(ch)
                ch == ' ' -> sb.append('+')
                else -> sb.append('%').append(HEX[c shr 4]).append(HEX[c and 15])
            }
        }
        return sb.toString()
    }

    /** The question for one piece. onlyprivate=1: the machine translation only, never a sentence from the shared memory. */
    fun myMemoryUrl(piece: String, way: Way): String =
        "https://" + HOST_MYMEMORY + "/get?q=" + enc(piece) + "&langpair=" + enc(way.myMemoryFrom + "|" + way.myMemoryTo) + "&onlyprivate=1&mt=1"

    /** The address of a Google question; the text goes in the body ([googleForm]): a long text in the address is refused. */
    fun googleUrl(way: Way): String =
        "https://" + HOST_GOOGLE + "/translate_a/single?client=gtx&sl=" + way.googleFrom + "&tl=" + way.googleTo + "&dt=t"

    fun googleForm(text: String): String = "q=" + enc(text)

    // ------------------------------------------------------------------------------------------------------ answers

    private const val USED_UP = "MyMemory's free translations for today are used up."

    /** MyMemory's answer (HTTP code and body). */
    fun parseMyMemory(code: Int, body: String?): TranslateResult {
        if (code == 429) return TranslateResult.failed(USED_UP, true, MYMEMORY, ErrorCode.BUSY)
        if (code != 200) {
            return TranslateResult.failed("MyMemory answered with an error ($code).", code >= 500, MYMEMORY, ErrorCode.SERVICE_ERROR)
        }
        val m: Map<String, Any?> = try {
            Json.parseObject(body ?: "")
        } catch (e: Json.JsonException) {
            return TranslateResult.failed("MyMemory's answer could not be read.", false, MYMEMORY, ErrorCode.BAD_ANSWER)
        }
        var text = ""
        val rd = m["responseData"]
        if (rd is Map<*, *>) {
            val t = rd["translatedText"]
            text = if (t is String) t else ""
        }
        if (m["quotaFinished"] == true || text.uppercase(Locale.ROOT).startsWith("MYMEMORY WARNING")) {
            return TranslateResult.failed(USED_UP, true, MYMEMORY, ErrorCode.BUSY)
        }
        val status = number(m["responseStatus"], 200)
        if (status == 429L) return TranslateResult.failed(USED_UP, true, MYMEMORY, ErrorCode.BUSY)
        if (status != 200L) {
            val d = m["responseDetails"]
            val details = if (d is String) d.trim() else ""
            return TranslateResult.failed(
                "MyMemory could not translate it" + (if (details.isEmpty()) "" else " (" + shortly(details) + ")") + ".",
                status >= 500, MYMEMORY, ErrorCode.SERVICE_ERROR
            )
        }
        text = unescape(text)
        if (text.trim().isEmpty()) return TranslateResult.failed("MyMemory gave no translation.", false, MYMEMORY, ErrorCode.BAD_ANSWER)
        return TranslateResult.ok(text, MYMEMORY)
    }

    private fun number(o: Any?, def: Long): Long = when (o) {
        is Number -> o.toLong()
        is String -> o.trim().toLongOrNull() ?: def
        else -> def
    }

    private fun shortly(s: String): String {
        val one = s.replace(Regex("\\s+"), " ")
        return if (one.length > 120) one.substring(0, 117) + "..." else one
    }

    /** MyMemory sometimes writes an apostrophe or a quote as an HTML entity. */
    fun unescape(s: String): String {
        if (s.indexOf('&') < 0) return s
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val semi = if (c == '&') s.indexOf(';', i) else -1
            if (semi > i && semi - i <= 10) {
                val name = s.substring(i + 1, semi)
                var rep: String? = when {
                    name == "amp" -> "&"
                    name == "quot" -> "\""
                    name == "apos" -> "'"
                    name == "lt" -> "<"
                    name == "gt" -> ">"
                    name.startsWith("#") -> {
                        val cp = if (name.length > 1 && (name[1] == 'x' || name[1] == 'X')) name.substring(2).toIntOrNull(16)
                        else name.substring(1).toIntOrNull()
                        if (cp != null && cp > 0 && Character.isValidCodePoint(cp)) String(Character.toChars(cp)) else null
                    }
                    else -> null
                }
                if (rep != null) {
                    out.append(rep)
                    i = semi + 1
                    continue
                }
            }
            out.append(c)
            i++
        }
        return out.toString()
    }

    /** Google's answer: HTTP code, where a redirect points (null if none), body. */
    fun parseGoogle(code: Int, location: String?, body: String?): TranslateResult {
        val b = body ?: ""
        if (code == 429 || code == 503 || (location != null && location.contains("/sorry")) || b.contains("google.com/sorry")) {
            return TranslateResult.failed("Google Translate asks this connection to wait (it sees too many questions from it).", true, GOOGLE, ErrorCode.BUSY)
        }
        if (code != 200) {
            return TranslateResult.failed("Google Translate answered with an error ($code).", code >= 500, GOOGLE, ErrorCode.SERVICE_ERROR)
        }
        val o: Any? = try {
            Json.parse(b)
        } catch (e: Json.JsonException) {
            return TranslateResult.failed("Google Translate's answer could not be read.", false, GOOGLE, ErrorCode.BAD_ANSWER)
        }
        val none = TranslateResult.failed("Google Translate gave no translation.", false, GOOGLE, ErrorCode.BAD_ANSWER)
        if (o !is List<*> || o.isEmpty() || o[0] !is List<*>) return none
        val sb = StringBuilder()
        for (seg in o[0] as List<*>) {
            if (seg is List<*> && seg.isNotEmpty() && seg[0] is String) sb.append(seg[0] as String)
        }
        if (sb.toString().trim().isEmpty()) return none
        return TranslateResult.ok(sb.toString(), GOOGLE)
    }

    // ------------------------------------------------------------------------------------------------------ shorthand

    private val SHORT: Map<String, String> = run {
        val pairs = arrayOf(
            "u", "you", "r", "are", "k", "OK", "kk", "OK", "pls", "please", "plz", "please", "thx", "thanks", "thnx", "thanks", "tnx", "thanks",
            "ty", "thank you", "np", "no problem", "idk", "I don't know", "idc", "I don't care", "ikr", "I know, right", "ily", "I love you",
            "tbh", "to be honest", "ngl", "not going to lie", "imo", "in my opinion", "imho", "in my opinion", "btw", "by the way",
            "omg", "oh my God", "lmk", "let me know", "hmu", "text me", "wyd", "what are you doing", "hbu", "how about you",
            "wbu", "how about you", "ttyl", "talk to you later", "brb", "be right back", "nvm", "never mind", "ofc", "of course",
            "jk", "just kidding", "omw", "on my way", "fyi", "for your information", "asap", "as soon as possible", "cya", "see you",
            "2nite", "tonight", "2night", "tonight", "tonite", "tonight", "tmrw", "tomorrow", "tmr", "tomorrow", "tmrrw", "tomorrow",
            "2moro", "tomorrow", "2morrow", "tomorrow", "2day", "today", "rn", "right now", "b4", "before", "gr8", "great", "l8r", "later",
            "bday", "birthday", "ppl", "people", "bc", "because", "cuz", "because", "coz", "because", "bcuz", "because",
            "prolly", "probably", "thru", "through", "tho", "though", "msg", "message", "txt", "text", "pic", "picture", "pics", "pictures"
        )
        val m = HashMap<String, String>()
        var i = 0
        while (i < pairs.size) { m[pairs[i]] = pairs[i + 1]; i += 2 }
        m
    }

    /**
     * Texting shorthand written out, for Google (MyMemory reads it well by itself). Whole words only; a capital at the start stays a
     * capital. "w/" and "w/o" are with and without.
     */
    fun expandShorthand(english: String?): String {
        if (english == null || english.isEmpty()) return ""
        val out = StringBuilder()
        var i = 0
        while (i < english.length) {
            val c = english[i]
            if (!Character.isLetterOrDigit(c)) {
                out.append(c)
                i++
                continue
            }
            var j = i
            while (j < english.length && Character.isLetterOrDigit(english[j])) j++
            val w = english.substring(i, j)
            if ((w == "w" || w == "W") && j < english.length && english[j] == '/') {
                val without = j + 1 < english.length && (english[j + 1] == 'o' || english[j + 1] == 'O') &&
                    (j + 2 >= english.length || !Character.isLetterOrDigit(english[j + 2]))
                val rep = if (without) "without" else "with"
                out.append(if (w == "W") capital(rep) else rep)
                i = if (without) j + 2 else j + 1
                continue
            }
            val rep = SHORT[w.lowercase(Locale.ROOT)]
            val apostrophe = (i > 0 && english[i - 1] == '\'') || (j < english.length && english[j] == '\'')
            if (rep == null || apostrophe) out.append(w)
            else out.append(if (Character.isUpperCase(w[0])) capital(rep) else rep)
            i = j
        }
        return out.toString()
    }

    private fun capital(s: String): String = if (s.isEmpty()) s else Character.toUpperCase(s[0]) + s.substring(1)

    // ------------------------------------------------------------------------------------------------------ Mexican Spanish

    /*
     * The safety net over a translation into Spanish: words from Spain that read wrong in Mexico, two of them badly ("coger" is vulgar
     * there, "chaqueta" is slang for something else, "zapatillas" are a woman's heels). Only whole words are changed, only where they
     * can mean nothing else, and a word whose gender changes takes its article and its adjective along.
     */

    private val WORDS = HashMap<String, String>()
    private val GENDERED = HashMap<String, Array<String>>()
    private val TO_MASCULINE = HashMap<String, String>()
    private val TO_FEMININE = HashMap<String, String>()
    private val ADJECTIVES = HashSet<String>()
    private val LINKS = HashSet<String>()
    private val BETWEEN = HashSet<String>()
    private val VOSOTROS = HashMap<String, String>()

    init {
        val words = arrayOf(
            "patata", "papa", "patatas", "papas", "camarero", "mesero", "camarera", "mesera", "camareros", "meseros", "camareras", "meseras",
            "chaqueta", "chamarra", "chaquetas", "chamarras", "coche", "carro", "coches", "carros", "piscina", "alberca", "piscinas", "albercas",
            "apartamento", "departamento", "apartamentos", "departamentos", "zumo", "jugo", "zumos", "jugos", "melocotón", "durazno",
            "melocotones", "duraznos", "aparcamiento", "estacionamiento", "aparcamientos", "estacionamientos", "guay", "padre"
        )
        var i = 0
        while (i < words.size) { WORDS[words[i]] = words[i + 1]; i += 2 }
        // noun, Mexican noun, "f" if the Spanish noun is feminine (the Mexican one is then masculine) or "m"
        val gendered = arrayOf(
            "zapatillas", "tenis", "f", "zapatilla", "tenis", "f", "nevera", "refrigerador", "f", "neveras", "refrigeradores", "f",
            "gamba", "camarón", "f", "gambas", "camarones", "f", "tarta", "pastel", "f", "tartas", "pasteles", "f", "gafas", "lentes", "f",
            "bolígrafo", "pluma", "m", "bolígrafos", "plumas", "m", "boligrafo", "pluma", "m", "boligrafos", "plumas", "m",
            "ordenador", "computadora", "m", "ordenadores", "computadoras", "m"
        )
        i = 0
        while (i < gendered.size) { GENDERED[gendered[i]] = arrayOf(gendered[i + 1], gendered[i + 2]); i += 3 }
        val fm = arrayOf(
            "la", "el", "una", "un", "las", "los", "unas", "unos", "esta", "este", "esa", "ese", "aquella", "aquel", "estas", "estos",
            "esas", "esos", "aquellas", "aquellos", "nuestra", "nuestro", "nuestras", "nuestros", "otra", "otro", "otras", "otros",
            "mucha", "mucho", "muchas", "muchos", "poca", "poco", "pocas", "pocos", "toda", "todo", "todas", "todos", "alguna", "algún",
            "algunas", "algunos", "ninguna", "ningún", "misma", "mismo", "mismas", "mismos", "cuánta", "cuánto", "cuántas", "cuántos"
        )
        i = 0
        while (i < fm.size) { TO_MASCULINE[fm[i]] = fm[i + 1]; TO_FEMININE[fm[i + 1]] = fm[i]; i += 2 }
        ADJECTIVES.addAll(
            listOf(
                "nuevo", "viejo", "blanco", "negro", "rojo", "amarillo", "morado", "rosado", "roto", "bonito", "lindo",
                "feo", "caro", "barato", "pequeño", "chico", "chiquito", "lleno", "vacío", "cómodo", "incómodo", "deportivo", "limpio", "sucio",
                "usado", "favorito", "mojado", "seco", "abierto", "cerrado", "perdido", "bueno", "malo", "rico", "delicioso", "frío", "listo"
            )
        )
        LINKS.addAll(
            listOf(
                "está", "están", "estaba", "estaban", "estuvo", "estuvieron", "es", "son", "era", "eran", "fue",
                "fueron", "quedó", "quedaron", "parece", "parecen", "sigue", "siguen"
            )
        )
        BETWEEN.addAll(listOf("muy", "tan", "bien", "súper", "super", "bastante", "demasiado", "algo", "todavía", "aún", "ya", "medio"))
        val vos = arrayOf(
            "vosotros", "ustedes", "vosotras", "ustedes", "vuestro", "su", "vuestra", "su", "vuestros", "sus", "vuestras", "sus",
            "estáis", "están", "sois", "son", "tenéis", "tienen", "vais", "van", "queréis", "quieren", "podéis", "pueden", "habéis", "han",
            "sabéis", "saben", "venís", "vienen", "hacéis", "hacen", "decís", "dicen", "veis", "ven", "seréis", "serán", "estaréis", "estarán",
            "podríais", "podrían", "teníais", "tenían", "erais", "eran", "estabais", "estaban", "fuisteis", "fueron", "tuvisteis", "tuvieron",
            "hicisteis", "hicieron", "vinisteis", "vinieron", "visteis", "vieron", "dijisteis", "dijeron", "pudisteis", "pudieron"
        )
        i = 0
        while (i < vos.size) { VOSOTROS[vos[i]] = vos[i + 1]; i += 2 }
    }

    /** A word or the text between two words. */
    private class Tok(var text: String, val word: Boolean)

    private fun tokens(s: String): List<Tok> {
        val out = ArrayList<Tok>()
        var i = 0
        while (i < s.length) {
            val w = Character.isLetter(s[i])
            var j = i
            while (j < s.length && Character.isLetter(s[j]) == w) j++
            out.add(Tok(s.substring(i, j), w))
            i = j
        }
        return out
    }

    private fun prevWord(t: List<Tok>, i: Int): Int = if (i >= 2 && t[i - 1].text.trim().isEmpty() && t[i - 2].word) i - 2 else -1

    private fun nextWord(t: List<Tok>, i: Int): Int = if (i + 2 < t.size && t[i + 1].text.trim().isEmpty() && t[i + 2].word) i + 2 else -1

    private fun low(s: String): String = s.lowercase(ES)

    /** The replacement written like the word it replaces: all capitals, a capital first letter, or small. */
    fun like(original: String, rep: String): String {
        if (rep.isEmpty() || original.isEmpty()) return rep
        if (original.length > 1 && original == original.uppercase(ES) && original != low(original)) return rep.uppercase(ES)
        if (Character.isUpperCase(original[0])) return Character.toUpperCase(rep[0]) + rep.substring(1)
        return rep
    }

    /** Spanish from a translation, made right for Mexico (see above). */
    fun toMexican(spanish: String?): String {
        if (spanish == null || spanish.isEmpty()) return ""
        val t = tokens(spanish)
        for (i in t.indices) {
            val k = t[i]
            if (!k.word) continue
            val w = k.text
            val lw = low(w)
            var rep: String? = Verbs.mexican(lw, before(t, i), after(t, i))
            if (rep == null) rep = WORDS[lw]
            if (rep == null) rep = VOSOTROS[lw]
            if (rep == null && (lw == "móvil" || lw == "movil")) {
                rep = if (nounBefore(t, i, "mi", "tu", "su", "el", "un", "al", "del", "este", "ese", "nuestro", "otro", "teléfono", "telefono")) "celular" else null
            }
            if (rep == null && (lw == "móviles" || lw == "moviles")) {
                rep = if (nounBefore(t, i, "mis", "tus", "sus", "los", "unos", "estos", "esos", "nuestros", "otros", "teléfonos", "telefonos")) "celulares" else null
            }
            if (rep == null && lw == "vale" && interjection(t, i)) rep = "ok"
            if (rep != null) {
                k.text = like(w, rep)
                continue
            }
            val g = GENDERED[lw]
            if (g != null) {
                k.text = like(w, g[0])
                agree(t, i, g[1] == "f")
            }
        }
        val sb = StringBuilder()
        for (k in t) sb.append(k.text)
        return sb.toString()
    }

    private fun before(t: List<Tok>, i: Int): String {
        val p = prevWord(t, i)
        return if (p < 0) "" else low(t[p].text)
    }

    private fun after(t: List<Tok>, i: Int): String {
        val n = nextWord(t, i)
        return if (n < 0) "" else low(t[n].text)
    }

    private fun nounBefore(t: List<Tok>, i: Int, vararg words: String): Boolean {
        val b = before(t, i)
        for (w in words) if (w == b) return true
        return false
    }

    /** "Vale" said for "OK": first in its sentence, and followed by a comma, a full stop, ! or ? or nothing. */
    private fun interjection(t: List<Tok>, i: Int): Boolean {
        val prev = if (i == 0) "" else t[i - 1].text.replace(Regex("[\\s¡¿]"), "")
        var first = i == 0 || prev.isEmpty() || prev.endsWith(".") || prev.endsWith("!") || prev.endsWith("?") || prev.endsWith(",")
        if (i >= 2 && prev.isEmpty() && t[i - 2].word) first = false // a word right before it: "no vale", "cuanto vale"
        val next = if (i + 1 < t.size) t[i + 1].text else ""
        val alone = i + 1 >= t.size || next.startsWith(",") || next.startsWith(".") || next.startsWith("!") || next.startsWith("?") ||
            (next.trim().isEmpty() && i + 2 >= t.size)
        return first && alone
    }

    /** A noun changed its gender: its article before it and its adjective after it (or after "esta", "son"...) follow. */
    private fun agree(t: List<Tok>, i: Int, wasFeminine: Boolean) {
        val det = if (wasFeminine) TO_MASCULINE else TO_FEMININE
        val p = prevWord(t, i)
        if (p >= 0) {
            val lp = low(t[p].text)
            val pp = prevWord(t, p)
            val lpp = if (pp >= 0) low(t[pp].text) else ""
            if (wasFeminine && lp == "la" && (lpp == "de" || lpp == "a")) { // de la -> del, a la -> al
                t[pp].text = like(t[pp].text, if (lpp == "de") "del" else "al")
                t[pp + 1].text = ""
                t[p].text = ""
            } else if (!wasFeminine && (lp == "del" || lp == "al")) { // del -> de la, al -> a la
                t[p].text = like(t[p].text, if (lp == "del") "de la" else "a la")
            } else if (det.containsKey(lp)) {
                t[p].text = like(t[p].text, det.getValue(lp))
            }
        }
        val n = nextWord(t, i)
        if (n >= 0 && swapAdjective(t[n], wasFeminine)) return
        if (n >= 0 && LINKS.contains(low(t[n].text))) {
            var a = nextWord(t, n)
            if (a >= 0 && BETWEEN.contains(low(t[a].text))) a = nextWord(t, a)
            if (a >= 0) swapAdjective(t[a], wasFeminine)
        }
    }

    /** Turns a known adjective to the other gender; false if the word is not one. */
    private fun swapAdjective(k: Tok, toMasculine: Boolean): Boolean {
        val lw = low(k.text)
        val base: String
        val plural: String
        if (lw.endsWith("os") || lw.endsWith("as")) {
            base = lw.substring(0, lw.length - 2)
            plural = "s"
        } else if (lw.endsWith("o") || lw.endsWith("a")) {
            base = lw.substring(0, lw.length - 1)
            plural = ""
        } else {
            return false
        }
        if (!ADJECTIVES.contains(base + "o")) return false
        val isFeminine = lw.endsWith("a") || lw.endsWith("as")
        if (isFeminine != toMasculine) return false // it does not agree with the old noun: it belongs to something else
        k.text = like(k.text, base + (if (toMasculine) "o" else "a") + plural)
        return true
    }

    /**
     * The verbs that are said differently in Mexico, in all their forms: coger -> agarrar (vulgar in Mexico), conducir -> manejar,
     * alquilar -> rentar, aparcar -> estacionar, enfadar -> enojar. A pronoun on the end ("cogelo") stays, with the written accent the
     * new word needs. Mexico has no vosotros: those forms become the ustedes ones.
     */
    object Verbs {
        private val CLITICS = arrayOf(
            "selos", "selas", "selo", "sela", "melos", "melas", "melo", "mela", "telos", "telas", "telo",
            "tela", "noslos", "noslas", "noslo", "nosla", "les", "los", "las", "nos", "lo", "la", "le", "me", "te", "se"
        )

        /** Every ending of a regular -ar verb, without written accents (they are kept as they were written: alquile -> rente). */
        private val AR: Set<String> = hashSetOf(
            "ar", "o", "as", "a", "amos", "ais", "an", "e", "aste", "asteis", "aron", "aba", "abas", "abamos", "abais", "aban", "are", "aras",
            "ara", "aremos", "areis", "aran", "aria", "arias", "ariamos", "ariais", "arian", "es", "emos", "eis", "en", "aramos", "arais",
            "ase", "ases", "asemos", "aseis", "asen", "ado", "ada", "ados", "adas", "ando", "ad"
        )
        private val AR_VOS = HashMap<String, String>()
        private val COG = HashMap<String, String>()
        private val COJ = HashMap<String, String>()
        private val CONDUC = HashMap<String, String>()
        private val CONDUZC = HashMap<String, String>()
        private val CONDUJ = HashMap<String, String>()

        private fun fill(m: MutableMap<String, String>, vararg pairs: String) {
            var i = 0
            while (i < pairs.size) { m[pairs[i]] = pairs[i + 1]; i += 2 }
        }

        init {
            fill(AR_VOS, "ais", "an", "asteis", "aron", "abais", "aban", "areis", "arán", "ariais", "arían", "eis", "en", "arais", "aran", "aseis", "asen", "ad", "en")
            fill(
                COG, "er", "ar", "es", "as", "e", "a", "emos", "amos", "eis", "an", "en", "an", "i", "é", "iste", "aste", "io", "ó",
                "imos", "amos", "isteis", "aron", "ieron", "aron", "ia", "aba", "ias", "abas", "iamos", "ábamos", "iais", "aban", "ian", "aban",
                "ere", "aré", "eras", "arás", "era", "ará", "eremos", "aremos", "ereis", "arán", "eran", "arán", "eria", "aría", "erias", "arías",
                "eriamos", "aríamos", "eriais", "arían", "erian", "arían", "iera", "ara", "ieras", "aras", "ieramos", "áramos", "ierais", "aran",
                "ieran", "aran", "iese", "ase", "ieses", "ases", "iesen", "asen", "ido", "ado", "ida", "ada", "idos", "ados", "idas", "adas",
                "iendo", "ando", "ed", "en"
            )
            fill(COJ, "o", "o", "a", "e", "as", "es", "amos", "emos", "ais", "en", "an", "en")
            fill(
                CONDUC, "ir", "ar", "es", "as", "e", "a", "imos", "amos", "is", "an", "en", "an", "ia", "aba", "ias", "abas", "iamos", "ábamos",
                "iais", "aban", "ian", "aban", "ire", "aré", "iras", "arás", "ira", "ará", "iremos", "aremos", "ireis", "arán", "iran", "arán",
                "iria", "aría", "irias", "arías", "iriamos", "aríamos", "iriais", "arían", "irian", "arían", "ido", "ado", "ida", "ada", "idos",
                "ados", "idas", "adas", "iendo", "ando", "id", "en"
            )
            fill(CONDUZC, "o", "o", "a", "e", "as", "es", "amos", "emos", "ais", "en", "an", "en")
            fill(
                CONDUJ, "e", "é", "iste", "aste", "o", "ó", "imos", "amos", "isteis", "aron", "eron", "aron", "era", "ara", "eras", "aras",
                "eramos", "áramos", "erais", "aran", "eran", "aran", "ese", "ase", "eses", "ases", "esen", "asen"
            )
        }

        /** Without written accents, to look a form up. */
        fun plain(s: String): String {
            val sb = StringBuilder()
            for (c in s) {
                val k = "áéíóú".indexOf(c)
                sb.append(if (k >= 0) "aeiou"[k] else c)
            }
            return sb.toString()
        }

        private fun accented(s: String): Boolean = plain(s) != s

        /** The stem with a written accent on its last vowel: an order with a pronoun on the end (agarralo -> agarralo with accent). */
        private fun stressed(stem: String): String {
            for (i in stem.length - 1 downTo 0) {
                val k = "aeiou".indexOf(stem[i])
                if (k >= 0) return stem.substring(0, i) + "áéíóú"[k] + stem.substring(i + 1)
            }
            return stem
        }

        /**
         * The Mexican word for this one, or null if it is not one of these verbs. before / after: the words around it (lower case, ""
         * if none), because "conduce a la playa" (leads to) and "esta cojo" (limps) are left alone.
         */
        fun mexican(w: String, before: String, after: String): String? {
            var r = irregular(w, before, after)
            if (r == null) r = regular(w, "alquil", "rent")
            if (r == null) r = regular(w, "enfad", "enoj")
            if (r == null) r = regular(w, "aparqu", "estacion")
            if (r == null) r = regular(w, "aparc", "estacion")
            return r
        }

        /** A regular -ar verb whose stem changes and whose endings stay as written (alquilar -> rentar, alquile -> rente). */
        private fun regular(w: String, oldStem: String, newStem: String): String? {
            if (w.length <= oldStem.length || !plain(w).startsWith(oldStem)) return null
            val ec = split(w.substring(oldStem.length), AR, null) ?: return null
            val ending = ec[0]
            val clitic = ec[1]
            val pe = plain(ending)
            if (clitic.isNotEmpty() && !takesPronoun(pe)) return null
            val stemAccent = accented(w.substring(0, oldStem.length))
            val stem = if (stemAccent && clitic.isNotEmpty()) stressed(newStem) else newStem
            val out = if (AR_VOS.containsKey(pe) && clitic.isEmpty()) AR_VOS.getValue(pe) else ending
            return stem + out + clitic
        }

        private val LAME_BEFORE: Set<String> = hashSetOf(
            "está", "esta", "estoy", "estás", "estas", "están", "estaba", "estaban", "es", "soy", "eres", "son", "era", "quedó", "quedé", "quedo",
            "el", "la", "los", "las", "un", "una", "unos", "unas", "muy", "medio", "anda", "ando", "andaba"
        )

        /** coger -> agarrar and conducir -> manejar, through their tables of endings. */
        private fun irregular(w: String, before: String, after: String): String? {
            val p = plain(w)
            val table: Map<String, String>
            val stemLen: Int
            val newStem: String
            when {
                p.startsWith("cog") -> { table = COG; stemLen = 3; newStem = "agarr" }
                p.startsWith("coj") -> { table = COJ; stemLen = 3; newStem = "agarr" }
                p.startsWith("conduzc") -> { table = CONDUZC; stemLen = 7; newStem = "manej" }
                p.startsWith("conduj") -> { table = CONDUJ; stemLen = 6; newStem = "manej" }
                p.startsWith("conduc") -> { table = CONDUC; stemLen = 6; newStem = "manej" }
                else -> return null
            }
            if (w.length <= stemLen) return null
            val ec = split(w.substring(stemLen), null, table) ?: return null
            val ending = ec[0]
            val clitic = ec[1]
            if (table === COJ && clitic.isEmpty() && LAME_BEFORE.contains(before)) return null // "esta cojo": limping, not taking
            if (newStem == "manej" && clitic.isEmpty() && (after == "a" || after == "al" || after == "hacia")) return null // "conduce a la playa"
            var mapped = table.getValue(plain(ending))
            if (clitic.isNotEmpty() && !takesPronoun(plain(mapped))) return null
            val stemAccent = accented(w.substring(0, stemLen))
            val stem = if (stemAccent && clitic.isNotEmpty()) stressed(newStem) else newStem
            if (clitic.isNotEmpty() && accented(ending)) { // the pronoun put the accent on the ending
                mapped = if (mapped == "ando") "ándo" else if (mapped == "ar") "ár" else mapped
            }
            return stem + mapped + clitic
        }

        /** The ending and the pronoun(s) on the end, or null when the rest of the word is not a form of the verb. */
        private fun split(rest: String, set: Set<String>?, table: Map<String, String>?): Array<String>? {
            if (has(plain(rest), set, table)) return arrayOf(rest, "")
            for (c in CLITICS) {
                if (rest.length > c.length && plain(rest).endsWith(c)) {
                    val ending = rest.substring(0, rest.length - c.length)
                    if (has(plain(ending), set, table)) return arrayOf(ending, rest.substring(rest.length - c.length))
                }
            }
            return null
        }

        private fun has(plainEnding: String, set: Set<String>?, table: Map<String, String>?): Boolean =
            if (set != null) set.contains(plainEnding) else table!!.containsKey(plainEnding)

        /** Only the infinitive, the gerund and an order take a pronoun on the end. */
        private fun takesPronoun(e: String): Boolean = e == "ar" || e == "ando" || e == "a" || e == "e" || e == "en"
    }
}
