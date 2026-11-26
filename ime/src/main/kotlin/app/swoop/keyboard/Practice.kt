package app.swoop.keyboard

import app.kestrel.core.input.glide.GlideKeys

/**
 * R34a2: the bridge between the keyboard and the glide practice screen ([PracticeActivity]); both live in the same process. The practice screen sets [sink] while it is open and gives its text box the private option
 * [OPTION]; the keyboard hands over a glide ONLY for a box that carries the option and only while a sink is set, so nothing typed anywhere else ever reaches it. A box with the option also learns nothing (the screen asks
 * for no personalised learning).
 */
object Practice {
    const val OPTION = "app.swoop.practice=1"

    fun isPracticeBox(privateImeOptions: String?): Boolean = privateImeOptions != null && privateImeOptions.split(',').any { it.trim() == OPTION }

    /** A glide of the practice box: its path (view pixels, milliseconds from the first touch), the words the keyboard offered (best first, the first is what it typed), and the key positions and view size it decoded on. UI thread. */
    fun interface Sink { fun glide(xs: FloatArray, ys: FloatArray, ts: FloatArray, n: Int, offered: List<String>, keys: GlideKeys?, viewW: Int, viewH: Int) }

    @Volatile var sink: Sink? = null
}
