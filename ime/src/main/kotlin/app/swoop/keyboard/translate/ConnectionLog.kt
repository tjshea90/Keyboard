package app.swoop.keyboard.translate

import app.kestrel.core.translate.TranslateLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The last 20 translation connections: time, service, milliseconds, how it ended (a code and a [app.kestrel.core.translate.SafeReason]).
 * Never a word of what was translated. In memory only (gone when the keyboard process is), shown on the Diagnostics screen so a failure the
 * owner met a minute ago can be read out ("translate (MyMemory) 8003 ms failed (DROPPED: SocketException (connection reset))").
 */
object ConnectionLog : TranslateLog {
    private const val MAX = 20
    private val lines = ArrayDeque<String>()

    override fun add(service: String, millis: Long, result: String) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        synchronized(lines) {
            lines.addLast("$stamp $service $millis ms $result")
            while (lines.size > MAX) lines.removeFirst()
        }
    }

    /** Newest last; empty text when nothing was translated since the keyboard started. */
    fun text(): String = synchronized(lines) { lines.joinToString("\n") }

    fun clear() = synchronized(lines) { lines.clear() }
}
