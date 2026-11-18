package app.kestrel.core.translate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** R24a: a failure reason that can say what broke (EPERM, ECONNRESET, ...) and can NEVER carry an address or the translated words. */
class SafeReasonTest {
    @Test fun blockedSocketNamesTheErrno() {
        assertEquals("SocketException (EPERM, socket creation refused)", SafeReason.of("SocketException", "socket failed: EPERM (Operation not permitted)"))
        assertEquals("a refused connect is a different thing", "SocketException (EPERM, not permitted)", SafeReason.of("SocketException", "connect failed: EPERM (Operation not permitted)"))
    }

    @Test fun aResetNamesErrnoAndPhrase() {
        assertEquals("SocketException (ECONNRESET, connection reset)", SafeReason.of("SocketException", "recvfrom failed: ECONNRESET (Connection reset by peer)"))
        assertEquals("SocketException (connection reset)", SafeReason.of("SocketException", "Connection reset"))
        assertEquals("SocketException (socket closed)", SafeReason.of("SocketException", "Socket closed"))
        assertEquals("SocketException (connection aborted)", SafeReason.of("SocketException", "Software caused connection abort"))
        assertEquals("SocketException (cut off mid-answer)", SafeReason.of("SocketException", "Unexpected end of file from server"))
    }

    @Test fun certificateAndClockProblemsAreNamed() {
        assertEquals("SSLHandshakeException (certificate not trusted)", SafeReason.of("SSLHandshakeException", "java.security.cert.CertPathValidatorException: Trust anchor for certification path not found."))
        assertEquals("SSLHandshakeException (certificate date wrong (check the clock))", SafeReason.of("SSLHandshakeException", "Chain validation failed: NotBefore"))
    }

    @Test fun aBareClassNameStaysBare() {
        assertEquals("SocketException", SafeReason.of("SocketException", null))
        assertEquals("SocketException", SafeReason.of("SocketException", ""))
        assertEquals("IOException", SafeReason.of("IOException", "something we do not know"))
    }

    @Test fun nothingElseOfAMessageIsEverCopied() {
        val msg = "failed to connect to api.mymemory.translated.net/203.0.113.7 (port 443) after 8000ms: https://api.mymemory.translated.net/get?q=my%20secret%20words ECONNREFUSED (Connection refused)"
        val r = SafeReason.of("ConnectException", msg)
        assertEquals("ConnectException (ECONNREFUSED, connection refused)", r)
        for (bad in listOf("secret", "mymemory", "203", "http", "443", "%")) assertFalse(bad, r.contains(bad))
        // an unknown ALL-CAPS word is not an errno: it is dropped
        assertNull(SafeReason.errnoOf("the word SECRET and PASSWORD1"))
        assertEquals("IOException", SafeReason.of("IOException", "my PASSWORD1 is here"))
    }

    @Test fun theClassNameIsCleaned() {
        assertEquals("abcde", SafeReason.of("a b/c?d=e", null))
        assertEquals("Exception", SafeReason.of("", null))
    }
}
