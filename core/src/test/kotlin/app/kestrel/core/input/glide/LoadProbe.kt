package app.kestrel.core.input.glide

import java.io.File

/** scratch: how long reading the word list takes and how much heap it holds */
object LoadProbe {
    @JvmStatic fun main(args: Array<String>) {
        val f = File(args[0])
        val rt = Runtime.getRuntime()
        repeat(3) { System.gc() }
        val before = rt.totalMemory() - rt.freeMemory()
        val t0 = System.nanoTime()
        val bytes = f.readBytes()
        val t1 = System.nanoTime()
        val lex = LexiconFile.read(bytes)
        val t2 = System.nanoTime()
        val keep = lex.size
        repeat(3) { System.gc() }
        val after = rt.totalMemory() - rt.freeMemory()
        println("%s: %d words %d pairs, file %d bytes, read %.0f ms, parse %.0f ms (first, cold JVM), heap held ~%.1f MB (file bytes dropped)".format(f.name, keep, lex.bigramCount, bytes.size, (t1 - t0) / 1e6, (t2 - t1) / 1e6, (after - before) / 1048576.0))
        var best = Long.MAX_VALUE
        repeat(5) { val a = System.nanoTime(); LexiconFile.read(bytes); best = minOf(best, System.nanoTime() - a) }
        println("   best of 5 warm parses %.0f ms".format(best / 1e6))
    }
}
