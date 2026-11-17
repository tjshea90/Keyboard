package app.kestrel.core.translate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class JsonTest {
    @Test fun parsesObjectsArraysAndScalars() {
        val m = Json.parseObject("""{"a":1,"b":[true,false,null,"x"],"c":{"d":-2.5e1},"e":"q\"\\\/\n\u00e9\u00F1"}""")
        assertEquals(1L, m["a"])
        assertEquals(listOf(true, false, null, "x"), m["b"])
        assertEquals(-25.0, (m["c"] as Map<*, *>)["d"])
        assertEquals("q\"\\/\n\u00e9\u00f1", m["e"])
    }

    @Test fun parsesGoogleShapedArrays() {
        val o = Json.parse("""[[["Hola","Hello",null,null,10]],null,"en"]""") as List<*>
        assertEquals("Hola", ((o[0] as List<*>)[0] as List<*>)[0])
    }

    @Test fun rejectsBrokenInput() {
        for (bad in listOf("", "{", "[1,", "{\"a\"}", "{\"a\":}", "tru", "\"abc", "[1] x", "{\"a\":\"\\q\"}", "{\"a\":\"\\u12\"}", "nul", "-", "{1:2}")) {
            try {
                Json.parse(bad)
                fail("accepted: $bad")
            } catch (e: Json.JsonException) {
                assertTrue(true)
            }
        }
    }

    @Test fun refusesAnObjectWhereNoneIsExpectedAndDeepNesting() {
        try { Json.parseObject("[1]"); fail() } catch (e: Json.JsonException) { }
        try { Json.parse("[".repeat(500) + "]".repeat(500)); fail() } catch (e: Json.JsonException) { }
    }
}
