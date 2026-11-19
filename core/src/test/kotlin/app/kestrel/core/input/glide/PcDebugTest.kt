package app.kestrel.core.input.glide

import org.junit.Test
import java.util.concurrent.Executor

class PcDebugTest {
    @Test fun dbg() {
        val bytes = LexiconFile.build(
            listOf("form" to 600L, "from" to 700L, "the" to 300L, "world" to 800L, "hello" to 500L).map { LexiconFile.Entry(it.first, it.second) },
            listOf(LexiconFile.Pair("from", "the", 3000L), LexiconFile.Pair("from", "world", 300L), LexiconFile.Pair("hello", "world", 400L))
        )
        val keys = GlideEval.keysFor(564f, 160f)
        val e = GlideTyping({ bytes }, Executor { it.run() }, savePersonal = { })
        e.prepare(keys, null, null)
        println("PCDBG ready=${e.ready} probs=" + e.nextWordLnProbs(listOf("form", "from"), "the", null)?.joinToString())
        val lex = LexiconFile.read(bytes)
        println("PCDBG ids form=${lex.idOf("form")} from=${lex.idOf("from")} the=${lex.idOf("the")}; bi(from,the)=${lex.bigramLogProb(lex.idOf("from"), lex.idOf("the"))} lp(the)=${lex.logProb(lex.idOf("the"))}")
    }
}
