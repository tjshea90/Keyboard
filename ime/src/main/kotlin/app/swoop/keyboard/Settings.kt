package app.swoop.keyboard

import android.content.Context
import android.content.SharedPreferences

/** The keys of the keyboard's settings (SharedPreferences, private to the app) and what each one is when nobody chose. */
object Prefs {
    const val FILE = "swoop"
    // typing
    const val GLIDE = "glide_typing"
    const val TRAIL = "trail"
    const val SUGGEST = "suggestions"
    const val AUTOFIX = "auto_fix"
    const val CAPI = "capitalize_i"
    const val AUTOCAPS = "auto_caps"
    const val AUTOSPACE = "auto_space_punct"
    const val DOUBLESPACE = "double_space_period"
    const val SPACESLIDE = "space_slide"
    // floating keyboard
    const val FLOAT_ON = "float_enabled"
    const val FLOATING = "floating"
    const val FLOAT_X = "float_x"
    const val FLOAT_Y = "float_y"
    const val FLOAT_W = "float_w_dp"
    const val FLOAT_H = "float_h_x100"
    // learning
    const val LEARN = "learn_typing"
    // look
    const val NUMBERROW = "number_row"
    const val HIDE_LETTERS = "hide_letters"
    const val CLIPBOARD = "clipboard_history"
    const val TRANSLATE = "translate_enabled"
    const val EMOJI_SUGGEST = "emoji_suggestions"
    const val DOCK_FRAC = "dock_frac_x1000"
    const val DOCK_X = "dock_x"
    const val KEYHEIGHT = "key_height"
    const val GLOBE = "show_globe"
    const val THEME = "theme"
    const val ACCENT = "accent"
    // feel
    const val PREVIEW = "key_preview"
    const val HAPTIC = "haptics"
    const val SOUND = "sound"
}

/** The neon colour of the glide trail, the lit keys and the settings screen. */
class Accent(val name: String, val color: Int)

/** Typed access to the settings, with their defaults in one place. Read each time the keyboard opens, so a change made in the app applies the next time a text box is tapped. */
class Settings(private val p: SharedPreferences) {
    constructor(c: Context) : this(c.getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE))

    private fun b(key: String, def: Boolean) = p.getBoolean(key, def)
    private fun i(key: String, def: Int) = p.getInt(key, def)
    fun set(key: String, v: Boolean) { p.edit().putBoolean(key, v).apply() }
    fun set(key: String, v: Int) { p.edit().putInt(key, v).apply() }

    val glide get() = b(Prefs.GLIDE, true)
    val trail get() = b(Prefs.TRAIL, true)
    val suggest get() = b(Prefs.SUGGEST, true)
    val autoFix get() = b(Prefs.AUTOFIX, true)
    val capitalizeI get() = b(Prefs.CAPI, true)
    val autoCaps get() = b(Prefs.AUTOCAPS, true)
    val autoSpace get() = b(Prefs.AUTOSPACE, true)
    val doubleSpace get() = b(Prefs.DOUBLESPACE, true)
    val spaceSlide get() = b(Prefs.SPACESLIDE, true)
    val learn get() = b(Prefs.LEARN, true)
    /** Off by default (the photo): the digits are the small hints on the top row, and a held top-row key types its digit. */
    val numberRow get() = b(Prefs.NUMBERROW, false)
    /** Letters are not drawn (a blank panel that still types the letters under it). */
    val hideLetters get() = b(Prefs.HIDE_LETTERS, false)
    /** The clipboard history (the clipboard icon in the toolbar keeps what was copied for an hour; pinned items stay). */
    val clipboardHistory get() = b(Prefs.CLIPBOARD, true)
    /** The translate icon in the toolbar (English to Mexican Spanish, only when switched on there). */
    val translateEnabled get() = b(Prefs.TRANSLATE, true)
    /** A word with an emoji (clown) offers the emoji among the guesses. */
    val emojiSuggestions get() = b(Prefs.EMOJI_SUGGEST, true)
    /** How wide the docked keyboard is, in thousandths of the screen (1000 = full width), and where it stands (0..1000 of the free room). */
    val dockFrac get() = i(Prefs.DOCK_FRAC, 1000).coerceIn(500, 1000)
    val dockX get() = i(Prefs.DOCK_X, 500).coerceIn(0, 1000)
    fun saveDock(frac: Int, x: Int) { p.edit().putInt(Prefs.DOCK_FRAC, frac).putInt(Prefs.DOCK_X, x).apply() }
    /** A key that switches keyboards, at the left of the space bar. Off by default: hold the space bar instead. */
    val showGlobe get() = b(Prefs.GLOBE, false)
    val keyHeight get() = i(Prefs.KEYHEIGHT, 1).coerceIn(0, KEY_HEIGHTS.size - 1)
    val theme get() = i(Prefs.THEME, 0).coerceIn(0, THEMES.size - 1)
    /** Cyan (the photo's Enter key) unless the person chose another. */
    val accent get() = i(Prefs.ACCENT, 1).coerceIn(0, ACCENTS.size - 1)
    val keyPreview get() = b(Prefs.PREVIEW, true)
    val haptics get() = b(Prefs.HAPTIC, true)
    val sound get() = b(Prefs.SOUND, false)
    /** The neon bar and floating are available (Settings > Layout and look). */
    val floatEnabled get() = b(Prefs.FLOAT_ON, true)
    /** The keyboard was floating when it last closed. */
    val floating get() = b(Prefs.FLOATING, false)
    /** Where the floating keyboard stands: 0..1000 of the free room across and down; its width in dp (0 = the default); the height multiplier x100 (100 = normal). */
    val floatX get() = i(Prefs.FLOAT_X, 500)
    val floatY get() = i(Prefs.FLOAT_Y, 800)
    val floatWDp get() = i(Prefs.FLOAT_W, 0)
    val floatHMul get() = i(Prefs.FLOAT_H, 100) / 100f
    fun saveFloat(floating: Boolean, x: Int, y: Int, wDp: Int, hMul: Float) {
        p.edit().putBoolean(Prefs.FLOATING, floating).putInt(Prefs.FLOAT_X, x).putInt(Prefs.FLOAT_Y, y).putInt(Prefs.FLOAT_W, wDp).putInt(Prefs.FLOAT_H, Math.round(hMul * 100f)).apply()
    }

    val keyHeightScale get() = KEY_HEIGHT_SCALE[keyHeight]
    val accentColor get() = ACCENTS[accent].color

    /** Everything back to how it was at install. */
    fun reset() { p.edit().clear().apply() }

    companion object {
        val KEY_HEIGHTS = listOf("Small", "Normal", "Large", "Extra large")
        val KEY_HEIGHT_SCALE = floatArrayOf(0.85f, 1f, 1.15f, 1.3f)
        /** 0 = follows the phone's light / dark setting. */
        val THEMES = listOf("Automatic", "Dark", "Light")
        val ACCENTS = listOf(
            Accent("Blue", 0xFF1F6FEB.toInt()), Accent("Cyan", 0xFF00DDE6.toInt()), Accent("Green", 0xFF2BE37A.toInt()),
            Accent("Pink", 0xFFFF4FB8.toInt()), Accent("Orange", 0xFFFF9020.toInt()), Accent("Violet", 0xFFA574FF.toInt()),
        )
    }
}
