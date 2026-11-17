package app.swoop.keyboard

import android.content.Context
import app.kestrel.core.input.glide.GlideKeys
import app.kestrel.core.input.glide.GlideTyping
import app.kestrel.core.input.glide.UserWords
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Owns the glide engine for the keyboard's process: where the word list and the learned memory live on disk, and the two worker threads (the engine's
 * own, which owns all learned state; and one for the live previews, so a preview never holds up the decode when the finger lifts). The threads exist only
 * while there is work: nothing runs while the keyboard is idle (plan 6.5). The learned memory file is private to the app and never leaves it except by
 * the owner's Export. Nothing here ever logs a word.
 */
class GlideHost(
    private val context: Context,
    private val engineExecutor: Executor = idleThread("swoop-glide"),
    previewExecutor: Executor = idleThread("swoop-preview"),
    loadMain: (() -> ByteArray?)? = null,
) {
    private val personalFile = File(context.filesDir, "glide_personal.txt")
    private val habitFile = File(context.filesDir, "glide_habit.txt")
    private val ownFile = File(context.filesDir, "own_words.txt")
    private val dictFile = File(context.filesDir, "user_dictionary.txt")

    val engine: GlideTyping = GlideTyping(
        loadMain = loadMain ?: { try { context.assets.open("glide/en.klx").use { it.readBytes() } } catch (e: Exception) { null } },
        executor = engineExecutor,
        loadPersonal = { if (personalFile.isFile) personalFile.readText() else null },
        savePersonal = { text -> writeAtomically(personalFile, text) },
        previewExecutor = previewExecutor,
        saveOwn = { text -> saveOwn(text) },
        loadDictionary = { if (dictFile.isFile) dictFile.readText() else null },
        saveDictionary = { text -> writeAtomically(dictFile, text) },
    )

    private var preparedFor = ""

    /** What the keyboard knows about this person, in words, read from the saved memory (numbers only; any thread). */
    fun learnedSummary(): String {
        val m = try { if (personalFile.isFile) app.kestrel.core.input.glide.PersonalModel.parse(personalFile.readText()) else null } catch (e: Exception) { null }
        if (m == null || m.isEmpty) return "Nothing yet. It starts learning with the first words you type."
        val n = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US)
        val c = m.confusionCount
        return "${n.format(m.wordCount)} of your words and ${n.format(m.pairCount)} word pairs" + (if (c > 0) ", and ${n.format(c)} corrections you made" else "") + "."
    }

    /** Settings > Forget: the learned words, pairs, corrections, own words and thumb habit are gone (any thread). */
    fun forgetEverything(done: () -> Unit = {}) {
        engine.forgetAll { own.clear(); try { ownFile.delete(); habitFile.delete() } catch (_: Exception) { }; done() }
    }

    /** Words the person uses that the shipped list lacks (KB-204 fills it from typing; an import brings the ones in the file). Loaded once, on first use. */
    val own: UserWords by lazy { try { if (ownFile.isFile) UserWords.parse(ownFile.readText(), 1400) else UserWords(1400) } catch (e: Exception) { UserWords(1400) } }
    fun saveOwn(text: String) { writeAtomically(ownFile, text) }

    /** The decoder needs the real key positions: known once the keys view has a size (and again when it changes). Cheap when nothing changed. */
    fun prepare(keys: GlideKeys?, tag: String) {
        if (keys == null || tag == preparedFor) return
        preparedFor = tag
        engine.prepare(keys, loadHabit(), own)
    }

    /** The thumb habit (where this person's finger lands, in key pitches), kept between sessions. */
    private fun loadHabit(): FloatArray? = try {
        if (habitFile.isFile) habitFile.readText().trim().split(' ').map { it.toFloat() }.takeIf { it.size == 2 }?.toFloatArray() else null
    } catch (e: Exception) { null }

    /** The saved thumb habit, for an export (null when none was learned yet). */
    fun habitOrNull(): FloatArray? = loadHabit()

    fun saveHabit(h: FloatArray) { if (h.size == 2) writeAtomically(habitFile, "${h[0]} ${h[1]}") }

    private fun writeAtomically(f: File, text: String) {
        try {
            if (text.isEmpty()) { f.delete(); return }
            val tmp = File(f.parentFile, f.name + ".tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
        } catch (e: Exception) { /* the memory is only a convenience: a full disk must never break typing */ }
    }

    companion object {
        /** One worker thread that exits after 20 s without work (core size 0: the queue starts it on demand). */
        fun idleThread(name: String): Executor = ThreadPoolExecutor(0, 1, 20, TimeUnit.SECONDS, LinkedBlockingQueue()) { r -> Thread(r, name).also { it.isDaemon = true } }
    }
}
