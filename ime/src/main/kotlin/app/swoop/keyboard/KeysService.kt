package app.swoop.keyboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import app.kestrel.core.input.CrashGuard
import java.io.File

/**
 * The system keyboard. A thin shell (plan 13.3): the lifecycle and the selection reports are forwarded to [KeyboardPresenter], which holds every decision
 * and is tested with a real EditText. SAFE MODE (KB-106): if the service started three times within a minute without ever drawing its keys, it runs with the plain
 * keys only (no glide engine, no word list, no learning), so a crash loop can never lock the owner out of typing; the globe key is always on the bottom row.
 */
class KeysService : InputMethodService() {
    private val main = Handler(Looper.getMainLooper())
    private val glide by lazy { Engine.host(applicationContext) }
    private val presenter by lazy { KeyboardPresenter(this, { currentInputConnection }, { glide }, { main.post(it) }) }
    /** True when the crash-loop guard switched the plain keys on. */
    var safeMode = false; private set
    private val guard by lazy { CrashGuard(FileStore(File(filesDir, "crash_guard.txt")), { SystemClock.elapsedRealtime() }) }

    override fun onCreate() {
        super.onCreate()
        safeMode = try { guard.onStart() } catch (e: Exception) { false }
        Engine.safeMode = safeMode
        presenter.safeMode = safeMode
        presenter.onNextKeyboard = { nextKeyboard() }
        presenter.onOpenSettings = { openSettings() }
        presenter.onTranslateMessage = { m -> root?.keys?.showNotice(m) }
        presenter.onToggleClipboard = { toggleClipboard() }
        presenter.clipChipSource = { clips.latestFresh(CLIP_CHIP_TTL_MS) }
        try { (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).addPrimaryClipChangedListener(clipListener) } catch (_: Exception) { }
    }

    private val clips by lazy { ClipboardHost(applicationContext) }
    private val clipListener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
        val st = Settings(this)
        if ((st.clipboardHistory || st.suggestCopied) && !safeMode) {
            if (clips.capture(history = st.clipboardHistory)) root?.clipboard?.let { if (it.isOpen) it.refresh() }
            presenter.onClipboardChanged()                         // the first box of the strip offers the new copy (owner R20a)
        }
    }

    /** The cog in the toolbar: the keyboard's settings screen (the Setup screen) opens in its own task. */
    private fun openSettings() {
        try { startActivity(android.content.Intent(this, SetupActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
    }

    /** The clipboard icon: show the clipboard in place of the keys, or go back to the keys. */
    private fun toggleClipboard() {
        val r = root ?: return
        if (r.clipboard.isOpen) { r.clipboard.close(); return }
        val on = Settings(this).clipboardHistory
        r.clipboard.historyOn = on
        clips.ensureLoaded()
        if (on) clips.capture()                                    // what was copied before the keyboard was open
        r.clipboard.store = clips.store
        r.clipboard.open()
    }

    private var root: KeyboardRoot? = null

    override fun onCreateInputView(): View = KeyboardRoot(this).let { root ->
        this.root = root
        root.host = object : KeyboardRoot.Host {
            override fun modeChanged(floating: Boolean) { applyWindow(floating) }
            override fun windowTall(tall: Boolean) { applyWindow(tall) }
            override fun geometryChanged() { presenter.prepareGlide() }
        }
        root.clipboard.host = object : ClipboardPanel.Host {
            override fun clipPaste(text: String) { presenter.pasteText(text); root.clipboard.close() }
            override fun clipClose() { root.clipboard.close() }
            override fun clipChanged() { clips.save() }
            override fun clipTurnOn() { Settings(this@KeysService).set(Prefs.CLIPBOARD, true); clips.capture(); root.clipboard.historyOn = true; root.clipboard.refresh() }
        }
        val v = root.keys
        presenter.view = v
        v.onFirstDraw = { try { guard.healthy() } catch (_: Exception) { } }
        v.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob -> if (r - l != or - ol || b - t != ob - ot) presenter.prepareGlide() }
        root
    }

    /**
     * Docked, the keyboard window is as tall as the keyboard and sits at the bottom. Floating (or while a docked height drag needs room to grow, R19c), it is the whole screen (transparent) so the panel can stand anywhere; [onComputeInsets] then tells Android that only the panel
     * takes touches and that the app behind is not covered at all (docs/FLOATING.md).
     */
    private fun applyWindow(floating: Boolean) {
        Diag.d("applyWindow floating=$floating window=${window?.window != null}")
        try { window?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, if (floating) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT) } catch (_: Exception) { }
        root?.requestLayout()
    }

    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        val r = root ?: return
        if (!r.floating && r.dockResizing && r.height > 0) {      // owner R19c: a docked height drag: the window is the whole screen, but the app stays laid out as it was and only the keyboard takes touches
            val h = maxOf(r.height, window?.window?.decorView?.height ?: 0)
            val top = (h - r.dockCoveredHeight()).coerceIn(0, h)
            outInsets.contentTopInsets = top
            outInsets.visibleTopInsets = top
            outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
            outInsets.touchableRegion.set(r.touchRegion())
            return
        }
        if (!r.floating && r.squeezed && r.height > 0) {            // a narrow docked keyboard: only it takes touches, the app shows (and takes touches) on both sides
            outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
            outInsets.touchableRegion.set(r.touchRegion())
            return
        }
        if (!r.floating || r.height <= 0) return
        val h = maxOf(r.height, window?.window?.decorView?.height ?: 0)
        Diag.d("onComputeInsets floating root=${r.height} decor=${window?.window?.decorView?.height} -> content/visible top=$h region=${r.touchRegion().bounds.flattenToString()}")
        outInsets.contentTopInsets = h
        outInsets.visibleTopInsets = h
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
        outInsets.touchableRegion.set(r.touchRegion())
    }

    /** The globe key: the next keyboard; if Android will not switch (a single keyboard, or no permission) the picker. */
    private fun nextKeyboard() {
        val ok = try { switchToNextInputMethod(false) } catch (e: Exception) { false }
        if (!ok) try { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() } catch (_: Exception) { }
    }

    /**
     * The Kestrel browser's navigation panel opens this keyboard floating over a page: it sends [ACTION_FLOAT] with InputMethodManager.sendAppPrivateCommand. Android delivers such a command ONLY to the keyboard
     * that is current and ONLY from the app whose text box is being edited, so no other app can reach this; and all it can do is float or dock the keyboard (it carries no data and reads nothing).
     */
    override fun onAppPrivateCommand(action: String?, data: android.os.Bundle?) {
        when (action) {
            ACTION_FLOAT -> root?.requestFloating(true)
            ACTION_DOCK -> root?.requestFloating(false)
            else -> super.onAppPrivateCommand(action, data)
        }
    }

    /** The keyboard never takes over the whole screen with the extract UI. */
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(editorInfo: EditorInfo, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        root?.clipboard?.close()                                   // a new text box starts on the keys
        root?.applySettings(Settings(this), safeMode)
        Settings(this).let { st -> if (!safeMode && (st.clipboardHistory || st.suggestCopied)) clips.capture(history = st.clipboardHistory) }       // a copy made while the keyboard was not on screen (it keeps its own time)
        Diag.d("onStartInputView safe=$safeMode floating=${root?.floating}")
        presenter.startInput(editorInfo, restarting)
    }

    override fun onFinishInput() { presenter.finishInput(); super.onFinishInput() }

    /** The keyboard goes away (Back, another app): what was learned is written now, so a killed process loses nothing. */
    override fun onFinishInputView(finishingInput: Boolean) { presenter.saveLearned(); presenter.inputViewFinished(); super.onFinishInputView(finishingInput) }
    override fun onWindowHidden() { presenter.saveLearned(); presenter.inputViewFinished(); super.onWindowHidden() }
    override fun onDestroy() { presenter.saveLearned(); try { (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).removePrimaryClipChangedListener(clipListener) } catch (_: Exception) { }; super.onDestroy() }

    /** Android tells us every selection change, ours and everybody else's; the controller tells them apart (plan 6.4). */
    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        presenter.onUpdateSelection(newSelStart, newSelEnd, candidatesStart, candidatesEnd)
    }
}

/** The private commands other apps (the Kestrel browser) may send to the keyboard: see [KeysService.onAppPrivateCommand]. */
const val ACTION_FLOAT = "app.swoop.keyboard.FLOAT"
const val ACTION_DOCK = "app.swoop.keyboard.DOCK"

/** [CrashGuard.Store] on a small private file. */
class FileStore(private val f: File) : CrashGuard.Store {
    override fun read(): String = if (f.isFile) f.readText() else ""
    override fun write(text: String) { if (text.isEmpty()) f.delete() else f.writeText(text) }
}
