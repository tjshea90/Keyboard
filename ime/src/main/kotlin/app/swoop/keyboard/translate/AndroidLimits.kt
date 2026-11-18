package app.swoop.keyboard.translate

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.PowerManager

/**
 * R25 (2026-11-18): the owner's connection test said "Android refuses the keyboard a network socket" (EPERM / EACCES). Android can refuse a background app the network on purpose, and a
 * keyboard process is a background app: Data Saver (ON by default on many Android Go tablets) blocks every app that is not on the "Unrestricted data" list on a METERED network, and a phone's
 * hotspot counts as metered; battery saver, doze and a restricted standby bucket do the same. This reads those facts about THIS app (no permission needed: each call answers for the calling app
 * only; Data Saver is the exception on Android 10: that one needs the network-state permission, which Swoop does not hold, so it often reads "unknown") so the connection test can name the one that applies. Pure facts, never anything typed.
 */
object AndroidLimits {
    /** Data Saver as the system reports it for this app. */
    enum class DataSaver { OFF, ON_NOT_ALLOWED, ON_ALLOWED, UNKNOWN }

    class Facts(val dataSaver: DataSaver, val batterySaver: Boolean, val doze: Boolean, val backgroundRestricted: Boolean, val standbyBucket: String) {
        /** True when something that can block this app's network in the background is on. */
        val limits: Boolean get() = dataSaver == DataSaver.ON_NOT_ALLOWED || backgroundRestricted || standbyBucket == "restricted" || batterySaver || doze

        /** One line for the screen: what is on, in words. */
        fun note(): String {
            val parts = ArrayList<String>(5)
            parts.add(when (dataSaver) {
                DataSaver.OFF -> "Data Saver off"
                DataSaver.ON_NOT_ALLOWED -> "Data Saver ON and Swoop is NOT on the Unrestricted data list"
                DataSaver.ON_ALLOWED -> "Data Saver on, Swoop is on the Unrestricted data list"
                DataSaver.UNKNOWN -> "Data Saver cannot be read (Android only tells apps with the network-state permission, which Swoop does not have)"
            })
            if (batterySaver) parts.add("battery saver ON")
            if (doze) parts.add("device is dozing")
            if (backgroundRestricted) parts.add("background use of Swoop is restricted")
            parts.add("standby bucket " + standbyBucket)
            return parts.joinToString(", ")
        }
    }

    /**
     * R25: does this process belong to the "inet" group (gid 3003)? On the kernels of Android 10 tablets (4.9) a process outside that group gets EACCES from every socket() call: Android did not hand
     * the app its internet access, which can happen right after an update that ADDED the INTERNET permission (Swoop added it in 0.4.0) until the device restarts. True / false, or null when it cannot be told
     * (newer Android checks internet access another way and an app need not show the group there: only asked below API 33).
     */
    fun inInetGroup(): Boolean? {
        if (android.os.Build.VERSION.SDK_INT >= 33) return null
        return try {
            val line = java.io.File("/proc/self/status").readLines().firstOrNull { it.startsWith("Groups:") } ?: return null
            line.removePrefix("Groups:").trim().split(Regex("\\s+")).contains("3003")
        } catch (e: Exception) { null }
    }

    fun read(context: Context): Facts {
        val saver = try {
            when ((context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.restrictBackgroundStatus) {
                ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED -> DataSaver.OFF
                ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED -> DataSaver.ON_NOT_ALLOWED
                ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED -> DataSaver.ON_ALLOWED
                else -> DataSaver.UNKNOWN
            }
        } catch (e: RuntimeException) { DataSaver.UNKNOWN }
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val battery = try { pm?.isPowerSaveMode == true } catch (e: RuntimeException) { false }
        val doze = try { pm?.isDeviceIdleMode == true } catch (e: RuntimeException) { false }
        val restricted = try { (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isBackgroundRestricted == true } catch (e: RuntimeException) { false }
        val bucket = try {
            when ((context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager)?.appStandbyBucket) {
                UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "active"
                UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "working set"
                UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "frequent"
                UsageStatsManager.STANDBY_BUCKET_RARE -> "rare"
                UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "restricted"
                null -> "unknown"
                else -> "other"
            }
        } catch (e: RuntimeException) { "unknown" }
        return Facts(saver, battery, doze, restricted, bucket)
    }
}
