package com.reader.analytics.shared

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UrlUtilsTest {

    @Test
    fun `extracts domain from full URL with path`() {
        val result = UrlUtils.extractDomain("https://example.com/path/to/article")

        assertEquals("example.com", result)
    }

    @Test
    fun `strips www prefix from domain`() {
        val result = UrlUtils.extractDomain("https://www.example.com/article")

        assertEquals("example.com", result)
    }

    @Test
    fun `preserves subdomain when not www`() {
        val result = UrlUtils.extractDomain("https://blog.example.com/post")

        assertEquals("blog.example.com", result)
    }

    @Test
    fun `handles URL without path`() {
        val result = UrlUtils.extractDomain("https://example.com")

        assertEquals("example.com", result)
    }

    @Test
    fun `returns original string for malformed URL`() {
        val result = UrlUtils.extractDomain("not-a-valid-url")

        assertEquals("not-a-valid-url", result)
    }

    @Test
    fun `handles empty string`() {
        val result = UrlUtils.extractDomain("")

        assertEquals("", result)
    }
}
