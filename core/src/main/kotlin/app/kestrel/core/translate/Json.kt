package app.kestrel.core.translate

/**
 * A tiny JSON reader for the two translation services' answers (the core cannot use org.json, which is an Android class).
 * Values: Map<String, Any?>, List<Any?>, String, Double/Long (Number), Boolean, null. Depth and size are bounded.
 */
object Json {
    class JsonException(message: String) : Exception(message)

    private const val MAX_DEPTH = 32

    @Throws(JsonException::class)
    fun parse(text: String): Any? {
        val p = Parser(text)
        p.ws()
        val v = p.value(0)
        p.ws()
        if (p.i != text.length) throw JsonException("trailing characters")
        return v
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(JsonException::class)
    fun parseObject(text: String): Map<String, Any?> {
        val v = parse(text)
        if (v !is Map<*, *>) throw JsonException("not an object")
        return v as Map<String, Any?>
    }

    private class Parser(val s: String) {
        var i = 0

        fun ws() {
            while (i < s.length && (s[i] == ' ' || s[i] == '\n' || s[i] == '\r' || s[i] == '\t')) i++
        }

        fun value(depth: Int): Any? {
            if (depth > MAX_DEPTH) throw JsonException("too deep")
            if (i >= s.length) throw JsonException("unexpected end")
            return when (val c = s[i]) {
                '{' -> obj(depth)
                '[' -> arr(depth)
                '"' -> str()
                't' -> lit("true", true)
                'f' -> lit("false", false)
                'n' -> lit("null", null)
                else -> if (c == '-' || c in '0'..'9') num() else throw JsonException("unexpected character")
            }
        }

        fun lit(word: String, v: Any?): Any? {
            if (!s.startsWith(word, i)) throw JsonException("bad literal")
            i += word.length
            return v
        }

        fun num(): Any {
            val st = i
            if (s[i] == '-') i++
            while (i < s.length && (s[i] in '0'..'9' || s[i] == '.' || s[i] == 'e' || s[i] == 'E' || s[i] == '+' || s[i] == '-')) i++
            val t = s.substring(st, i)
            return t.toLongOrNull() ?: t.toDoubleOrNull() ?: throw JsonException("bad number")
        }

        fun str(): String {
            i++ // opening quote
            val sb = StringBuilder()
            while (true) {
                if (i >= s.length) throw JsonException("unterminated string")
                val c = s[i++]
                when {
                    c == '"' -> return sb.toString()
                    c == '\\' -> {
                        if (i >= s.length) throw JsonException("bad escape")
                        when (val e = s[i++]) {
                            '"', '\\', '/' -> sb.append(e)
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000c')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 > s.length) throw JsonException("bad unicode escape")
                                val n = s.substring(i, i + 4).toIntOrNull(16) ?: throw JsonException("bad unicode escape")
                                sb.append(n.toChar())
                                i += 4
                            }
                            else -> throw JsonException("bad escape")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        fun arr(depth: Int): List<Any?> {
            i++
            val out = ArrayList<Any?>()
            ws()
            if (i < s.length && s[i] == ']') { i++; return out }
            while (true) {
                ws()
                out.add(value(depth + 1))
                ws()
                if (i >= s.length) throw JsonException("unterminated array")
                val c = s[i++]
                if (c == ']') return out
                if (c != ',') throw JsonException("expected , or ]")
            }
        }

        fun obj(depth: Int): Map<String, Any?> {
            i++
            val out = LinkedHashMap<String, Any?>()
            ws()
            if (i < s.length && s[i] == '}') { i++; return out }
            while (true) {
                ws()
                if (i >= s.length || s[i] != '"') throw JsonException("expected key")
                val k = str()
                ws()
                if (i >= s.length || s[i++] != ':') throw JsonException("expected :")
                ws()
                out[k] = value(depth + 1)
                ws()
                if (i >= s.length) throw JsonException("unterminated object")
                val c = s[i++]
                if (c == '}') return out
                if (c != ',') throw JsonException("expected , or }")
            }
        }
    }
}
