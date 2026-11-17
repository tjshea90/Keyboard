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
    // learning
    const val LEARN = "learn_typing"
    // look
    const val NUMBERROW = "number_row"
    const val KEYHEIGHT = "key_height"
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
    val numberRow get() = b(Prefs.NUMBERROW, true)
    val keyHeight get() = i(Prefs.KEYHEIGHT, 1).coerceIn(0, KEY_HEIGHTS.size - 1)
    val theme get() = i(Prefs.THEME, 0).coerceIn(0, THEMES.size - 1)
    val accent get() = i(Prefs.ACCENT, 0).coerceIn(0, ACCENTS.size - 1)
    val keyPreview get() = b(Prefs.PREVIEW, true)
    val haptics get() = b(Prefs.HAPTIC, true)
    val sound get() = b(Prefs.SOUND, false)

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
            Accent("Blue", 0xFF1F6FEB.toInt()), Accent("Cyan", 0xFF00D8F0.toInt()), Accent("Green", 0xFF2BE37A.toInt()),
            Accent("Pink", 0xFFFF4FB8.toInt()), Accent("Orange", 0xFFFF9020.toInt()), Accent("Violet", 0xFFA574FF.toInt()),
        )
    }
}
