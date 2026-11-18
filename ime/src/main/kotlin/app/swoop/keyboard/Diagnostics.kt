package app.swoop.keyboard

import android.content.Context
import android.os.Build
import android.os.Debug
import app.kestrel.core.input.editor.InputStats

/**
 * The text of the Diagnostics screen (plan 8.5 layer 1): versions, memory, the glide engine's counters and timings and the keyboard's own counters. It holds NUMBERS ONLY:
 * no typed text, no word, no field content ever reaches it, so the owner can paste it into a chat (plan 11.6).
 */
object Diagnostics {
    fun text(context: Context, input: InputStats, glideLine: String, glideReady: Boolean, safeMode: Boolean = false): String {
        val mi = Debug.MemoryInfo(); try { Debug.getMemoryInfo(mi) } catch (_: Exception) { }
        val rt = Runtime.getRuntime()
        val pkg = try { context.packageManager.getPackageInfo(context.packageName, 0) } catch (e: Exception) { null }
        val name = pkg?.versionName ?: "?"
        @Suppress("DEPRECATION") val code = if (pkg != null) (if (Build.VERSION.SDK_INT >= 28) pkg.longVersionCode.toString() else pkg.versionCode.toString()) else "?"
        return buildString {
            appendLine("Swoop $name (code $code)")
            appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ${Build.MODEL}")
            appendLine("keyboard switched on: ${SetupActivity.isEnabled(context)}, chosen now: ${SetupActivity.isCurrent(context)}")
            appendLine("memory: process ${mi.totalPss / 1024} MB, java heap ${(rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)} of ${rt.maxMemory() / (1024 * 1024)} MB")
            appendLine("safe layout (crash-loop guard): ${if (safeMode) "ON - plain keys only" else "off"}")
            appendLine("glide engine ready: $glideReady")
            appendLine(if (glideLine.isBlank()) "glide: no glide yet" else "glide: $glideLine")
            appendLine("input: ${input.line()}")
            val net = app.swoop.keyboard.translate.ConnectionLog.text()
            if (net.isNotEmpty()) { appendLine("translate connections (service, time, result; never words):"); appendLine(net) }
            append("(numbers only: this text never contains anything you typed)")
        }
    }
}
