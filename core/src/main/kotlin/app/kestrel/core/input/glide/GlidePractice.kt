package app.kestrel.core.input.glide

/**
 * R34a2 (owner 2026-11-26: "It still gets a lot of glide predictions wrong"): the one thing public data cannot give is how glides look on THIS person's tablet. The practice drill shows a fixed word, the person glides it
 * on the real keyboard, and the keyboard keeps the finger's path, the words it offered and the word that was asked for. Only the PROMPTED words are ever kept (never what the person types anywhere else), and only while the practice
 * screen is open. The result is a plain-text file the person attaches to the chat; a lab ([PracticeEval] in the tests) re-decodes it on the recorded key positions to measure and tune the decoder for that tablet.
 * Pure Kotlin, no Android. The file is private to the owner: it is never committed (tools/secretscan.sh blocks its magic line).
 */
class GlidePractice(prompts: List<String> = WORDS) {
    /**
     * One answered prompt: the word asked for, the finger's path (view pixels, milliseconds from the first touch) and the (up to three) words the keyboard offered, best first. R36 (Glide training): [prev] and [prev2] are
     * the two words before it that the keyboard decoded it with (null: none, as at the start of a sentence or in a drill), [tag] says which page of the training it came from ("p3" the third page).
     */
    class Entry(val word: String, val xs: FloatArray, val ys: FloatArray, val ts: FloatArray, val got: List<String>, val prev: String? = null, val prev2: String? = null, val tag: String = "")

    private val list = ArrayList(prompts)
    /** The words asked for, in order (R36: more can be added while the training runs: the words that were missed). */
    val words: List<String> get() = list
    private val items = ArrayList<Entry>()
    var index = 0; private set
    val entries: List<Entry> get() = items
    val total: Int get() = list.size
    /** The word to glide now; null when every prompt was answered or skipped. */
    val current: String? get() = list.getOrNull(index)
    val done: Boolean get() = index >= list.size
    val answered: Int get() = items.size

    /** The glide of the current word: [n] points of [xs], [ys], [ts]; [got] what the keyboard offered (the first is what it typed). Moves on to the next word. */
    fun record(xs: FloatArray, ys: FloatArray, ts: FloatArray, n: Int, got: List<String>, prev: String? = null, prev2: String? = null, tag: String = "") {
        val w = current ?: return
        val m = n.coerceIn(0, minOf(xs.size, ys.size, ts.size))
        val t0 = if (m > 0) ts[0] else 0f
        items.add(Entry(w, xs.copyOf(m), ys.copyOf(m), FloatArray(m) { ts[it] - t0 }, got.take(3), prev, prev2, tag))
        index++
    }

    /** The person skipped the current word (nothing is kept for it). */
    fun skip() { if (!done) index++ }

    /** The person stops early: what was answered so far is the result. */
    fun finishNow() { index = list.size }

    /** R36: more words to ask for, after the ones already in the list. */
    fun append(more: List<String>) { list.addAll(more) }

    /** How many answered prompts the keyboard put first / among its first three. */
    fun firstRight(): Int = items.count { it.got.firstOrNull() == it.word }
    fun inThree(): Int = items.count { it.word in it.got }

    /** The prompts the keyboard did not put first: (the word asked for, the word it typed). */
    fun misses(): List<Pair<String, String>> = items.filter { it.got.firstOrNull() != it.word }.map { it.word to (it.got.firstOrNull() ?: "") }

    /**
     * The file: a magic line, which build and view the glides were made on (key centres in view pixels, so a lab can decode them exactly as the keyboard did), then one line per answered word.
     * [version] is the app version; nothing else about the person or the device is written. R36: a [training] file (the Glide training screen) is format 2: every line also says which page it came from and the two words
     * before it, and the file says whether the keyboard was being [teaching] from the glides while they were made.
     */
    fun serialize(keys: GlideKeys, viewW: Int, viewH: Int, density: Float, version: String, training: Boolean = false, teaching: Boolean = false): String {
        val sb = StringBuilder(4096)
        sb.append(if (training) MAGIC2 else MAGIC).append('\n')
        sb.append("app ").append(version).append('\n')
        if (training) sb.append("teaching ").append(if (teaching) 1 else 0).append('\n')
        sb.append("view ").append(viewW).append(' ').append(viewH).append(' ').append(density).append('\n')
        sb.append("keys ").append(keys.keyW).append(' ').append(keys.keyH).append(' ').append(keys.pitch).append('\n')
        sb.append("centres ")
        for (k in 0 until 26) { if (k > 0) sb.append(';'); sb.append(keys.cx[k]).append(',').append(keys.cy[k]) }
        sb.append('\n')
        for (e in items) {
            sb.append("w ").append(e.word).append(" | ").append(e.got.joinToString(",")).append(" | ")
            for (i in e.xs.indices) { if (i > 0) sb.append(';'); sb.append(fmt(e.xs[i])).append(',').append(fmt(e.ys[i])).append(',').append(Math.round(e.ts[i])) }
            if (training) sb.append(" | ").append(e.tag.ifEmpty { "-" }).append(' ').append(e.prev2 ?: "-").append(' ').append(e.prev ?: "-")
            sb.append('\n')
        }
        return sb.toString()
    }

    class Parsed(val keys: GlideKeys, val viewW: Int, val viewH: Int, val density: Float, val version: String, val entries: List<Entry>, val training: Boolean = false, val teaching: Boolean = false)

    companion object {
        const val MAGIC = "SWOOP-GLIDE-PRACTICE 1"
        /** R36: the Glide training file (longer, with the context words and the page of every glide). */
        const val MAGIC2 = "SWOOP-GLIDE-PRACTICE 2"

        private fun fmt(v: Float): String = if (v == Math.rint(v.toDouble()).toFloat()) v.toInt().toString() else "%.1f".format(java.util.Locale.US, v)

        /** The file written by [serialize]; null when the header is not one. A damaged or cut-off line is skipped, the complete ones are kept. */
        fun parse(text: String?): Parsed? {
            if (text.isNullOrBlank()) return null
            val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
            if (lines.isEmpty() || (lines[0] != MAGIC && lines[0] != MAGIC2)) return null
            val training = lines[0] == MAGIC2
            var teaching = false
            var version = ""; var viewW = 0; var viewH = 0; var density = 1f; var keyW = 0f; var keyH = 0f; var pitch = 0f
            var cx: FloatArray? = null; var cy: FloatArray? = null
            val out = ArrayList<Entry>()
            try {
                for (l in lines.drop(1)) {
                    when {
                        l.startsWith("app ") -> version = l.substring(4)
                        l.startsWith("teaching ") -> teaching = l.substring(9).trim() == "1"
                        l.startsWith("view ") -> { val p = l.substring(5).split(' '); viewW = p[0].toInt(); viewH = p[1].toInt(); density = p[2].toFloat() }
                        l.startsWith("keys ") -> { val p = l.substring(5).split(' '); keyW = p[0].toFloat(); keyH = p[1].toFloat(); pitch = p[2].toFloat() }
                        l.startsWith("centres ") -> {
                            val p = l.substring(8).split(';'); if (p.size != 26) return null
                            cx = FloatArray(26); cy = FloatArray(26)
                            for (k in 0 until 26) { val q = p[k].split(','); cx[k] = q[0].toFloat(); cy[k] = q[1].toFloat() }
                        }
                        l.startsWith("w ") -> try {                                       // (a damaged or cut-off line is skipped, the others stay)
                            val parts = l.substring(2).split(" | ")
                            if (parts.size == 3) {
                                val pts = parts[2].split(';').filter { it.isNotEmpty() }.map { it.split(',') }
                                if (pts.all { it.size == 3 }) out.add(Entry(parts[0], FloatArray(pts.size) { pts[it][0].toFloat() }, FloatArray(pts.size) { pts[it][1].toFloat() }, FloatArray(pts.size) { pts[it][2].toFloat() }, parts[1].split(',').filter { it.isNotEmpty() }))
                            }
                        } catch (e: NumberFormatException) { }
                    }
                }
            } catch (e: Exception) { return null }
            val x = cx ?: return null; val y = cy ?: return null
            if (keyW <= 0f || keyH <= 0f || pitch <= 0f || viewW <= 0 || viewH <= 0) return null
            val keys = GlideKeys(x, y, pitch, keyW, keyH).also { it.pitchDp = pitch / density.coerceAtLeast(0.1f) }
            return Parsed(keys, viewW, viewH, density, version, out)
        }

        /**
         * The words asked for: everyday words, the ones the owner reported (and, for, make, or, the, there, access) and the look-alikes they are confused with (our, out, it, is, in, of), short and long, so a few minutes
         * of gliding say where this tablet differs from the phone-sized public data. Letters a to z only.
         */
        val WORDS: List<String> = listOf(
            "the", "and", "for", "or", "there", "is", "in", "it", "of", "to",
            "you", "that", "with", "have", "this", "from", "they", "what", "make", "can",
            "like", "just", "know", "out", "our", "are", "not", "but", "all", "was",
            "when", "your", "would", "about", "time", "people", "because", "something", "access", "never",
        )
    }
}
