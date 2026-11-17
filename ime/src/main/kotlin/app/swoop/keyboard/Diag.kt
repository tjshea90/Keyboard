package app.swoop.keyboard

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log

/**
 * Geometry-only trace lines (tag SwoopDiag) for the emulator smoke test, written ONLY by a debuggable build (the release APK writes nothing). Sizes, positions and state names: never a typed character, never a word.
 */
object Diag {
    @Volatile private var on = false
    /** True in a debuggable build only: callers that build a trace line from state check this first, so the release build does no work for it. */
    val enabled: Boolean get() = on
    fun init(c: Context) { on = (c.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }
    fun d(msg: String) { if (on) Log.d("SwoopDiag", msg) }
}
