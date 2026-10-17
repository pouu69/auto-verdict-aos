package com.car.autoverdict.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebViewEngineHostTest {

    @Test
    fun `js string literal is double quoted and escapes quotes, newlines and line separators`() {
        val literal = WebViewEngineHost.jsStringLiteral("a\"b\\c\nd e f")
        assertEquals("\"a\\\"b\\\\c\\nd\\u2028e\\u2029f\"", literal)
        assertFalse(literal.contains(' '))
        assertFalse(literal.contains(' '))
    }
}
