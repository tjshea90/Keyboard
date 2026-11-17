package app.swoop.keyboard

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Disk and file-picker work runs here, never on the UI thread (plan 2.3). One thread that exits when idle. */
object Io {
    private val ui = Handler(Looper.getMainLooper())
    private val pool: Executor = ThreadPoolExecutor(0, 1, 20, TimeUnit.SECONDS, LinkedBlockingQueue()) { r -> Thread(r, "swoop-io").also { it.isDaemon = true } }
    fun exec(r: () -> Unit) { pool.execute { try { r() } catch (_: Exception) { } } }
    fun ui(r: () -> Unit) { ui.post(r) }
}

/** The one glide engine of this process, shared by the keyboard service and the Setup screen (export / import work on the same learned memory). */
object Engine {
    /** The keyboard's own counters, shared with the Diagnostics screen (numbers only). */
    val inputStats = app.kestrel.core.input.editor.InputStats()
    /** True while the keyboard runs in safe mode (the crash-loop guard); shown in Diagnostics. */
    @Volatile var safeMode = false
    @Volatile private var host: GlideHost? = null
    fun host(context: android.content.Context): GlideHost = host ?: synchronized(this) { host ?: GlideHost(context.applicationContext).also { host = it } }
}
