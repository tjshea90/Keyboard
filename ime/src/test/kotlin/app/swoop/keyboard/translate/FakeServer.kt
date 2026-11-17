package app.swoop.keyboard.translate

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/** A tiny HTTP/1.1 server on 127.0.0.1 for tests (plain ServerSocket: no JDK-internal packages). One request per connection. */
class FakeServer {
    class Req(val method: String, val target: String, val headers: Map<String, String>, val body: String) {
        val path: String get() = target.substringBefore('?')
        val rawQuery: String? get() = if (target.contains('?')) target.substringAfter('?') else null
    }

    class Res(val code: Int, val body: ByteArray = ByteArray(0), val headers: Map<String, String> = emptyMap(), val delayMs: Long = 0)

    private val ss = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
    val port: Int get() = ss.localPort
    private val routes = LinkedHashMap<String, (Req) -> Res>()
    @Volatile private var running = true

    fun route(path: String, handler: (Req) -> Res) { routes[path] = handler }

    fun start() {
        val t = Thread {
            while (running) {
                val s = try { ss.accept() } catch (e: Exception) { break }
                Thread { serve(s) }.also { it.isDaemon = true }.start()
            }
        }
        t.isDaemon = true
        t.start()
    }

    fun stop() { running = false; try { ss.close() } catch (e: Exception) { } }

    private fun readLine(i: InputStream): String? {
        val b = ByteArrayOutputStream()
        while (true) {
            val c = i.read()
            if (c < 0) return if (b.size() == 0) null else b.toString("UTF-8")
            if (c == '\n'.code) return b.toString("UTF-8").trimEnd('\r')
            b.write(c)
        }
    }

    private fun serve(s: Socket) {
        try {
            s.soTimeout = 5000
            val i = s.getInputStream()
            val first = readLine(i) ?: return
            val parts = first.split(' ')
            val headers = HashMap<String, String>()
            while (true) {
                val l = readLine(i) ?: break
                if (l.isEmpty()) break
                headers[l.substringBefore(':').trim().lowercase()] = l.substringAfter(':').trim()
            }
            val len = headers["content-length"]?.toIntOrNull() ?: 0
            val body = ByteArray(len)
            var got = 0
            while (got < len) { val n = i.read(body, got, len - got); if (n < 0) break; got += n }
            val req = Req(parts[0], parts[1], headers, String(body, Charsets.UTF_8))
            val h = routes[req.path]
            val res = if (h == null) Res(404) else h(req)
            if (res.delayMs > 0) try { Thread.sleep(res.delayMs) } catch (e: InterruptedException) { }
            val o = s.getOutputStream()
            val sb = StringBuilder("HTTP/1.1 ${res.code} X\r\nContent-Length: ${res.body.size}\r\nConnection: close\r\n")
            for ((k, v) in res.headers) sb.append(k).append(": ").append(v).append("\r\n")
            sb.append("\r\n")
            o.write(sb.toString().toByteArray(Charsets.ISO_8859_1))
            o.write(res.body)
            o.flush()
        } catch (e: Exception) {
            // the client may hang up (abort, timeout): fine
        } finally {
            try { s.close() } catch (e: Exception) { }
        }
    }
}
