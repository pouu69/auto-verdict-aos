package com.car.autoverdict.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class JsEscapeTest {

    @Test
    fun `plain text passes through unchanged`() {
        assertEquals("hello world", JsEscape.escapeForSingleQuotedString("hello world"))
    }

    @Test
    fun `escapes backslash`() {
        assertEquals("a\\\\b", JsEscape.escapeForSingleQuotedString("a\\b"))
    }

    @Test
    fun `escapes single quote`() {
        assertEquals("it\\'s", JsEscape.escapeForSingleQuotedString("it's"))
    }

    @Test
    fun `escapes newline`() {
        assertEquals("a\\nb", JsEscape.escapeForSingleQuotedString("a\nb"))
    }

    @Test
    fun `escapes carriage return`() {
        assertEquals("a\\rb", JsEscape.escapeForSingleQuotedString("a\rb"))
    }

    @Test
    fun `escapes tab`() {
        assertEquals("a\\tb", JsEscape.escapeForSingleQuotedString("a\tb"))
    }

    @Test
    fun `escapes unicode line separator U+2028`() {
        val input = "a b"
        val result = JsEscape.escapeForSingleQuotedString(input)
        assertEquals("a\\u2028b", result)
    }

    @Test
    fun `escapes unicode paragraph separator U+2029`() {
        val input = "a b"
        val result = JsEscape.escapeForSingleQuotedString(input)
        assertEquals("a\\u2029b", result)
    }

    @Test
    fun `handles combined special characters`() {
        val input = "it's a\nnew\\line\twith sep"
        val result = JsEscape.escapeForSingleQuotedString(input)
        assertEquals("it\\'s a\\nnew\\\\line\\twith\\u2028sep", result)
    }

    @Test
    fun `handles empty string`() {
        assertEquals("", JsEscape.escapeForSingleQuotedString(""))
    }

    @Test
    fun `handles JSON with special chars`() {
        val json = """{"key":"val'ue","num":1}"""
        val result = JsEscape.escapeForSingleQuotedString(json)
        assertFalse(result.contains("'v"))
    }

    @Test
    fun `backslash is escaped before other replacements`() {
        val input = "a\\nb"
        val result = JsEscape.escapeForSingleQuotedString(input)
        assertEquals("a\\\\nb", result)
    }
}
