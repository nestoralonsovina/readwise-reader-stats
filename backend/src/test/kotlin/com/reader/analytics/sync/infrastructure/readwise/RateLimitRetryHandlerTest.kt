package com.reader.analytics.sync.infrastructure.readwise

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatusCode
import org.springframework.web.client.RestClientResponseException
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RateLimitRetryHandlerTest {

    private val handler = RateLimitRetryHandler(
        RetryConfig(
            maxAttempts = 3,
            baseDelay = Duration.ofMillis(10),
            maxDelay = Duration.ofMillis(100)
        )
    )

    @Test
    fun `succeeds on first attempt without retry`() {
        var callCount = 0

        val result = handler.executeWithRetry("test") {
            callCount++
            "success"
        }

        assertEquals("success", result)
        assertEquals(1, callCount)
    }

    @Test
    fun `retries on 429 and succeeds on second attempt`() {
        var callCount = 0

        val result = handler.executeWithRetry("test") {
            callCount++
            if (callCount == 1) {
                throw create429Exception()
            }
            "success"
        }

        assertEquals("success", result)
        assertEquals(2, callCount)
    }

    @Test
    fun `throws immediately on non-429 error`() {
        var callCount = 0

        assertFailsWith<RestClientResponseException> {
            handler.executeWithRetry("test") {
                callCount++
                throw create500Exception()
            }
        }

        assertEquals(1, callCount)
    }

    @Test
    fun `throws after max attempts exceeded`() {
        var callCount = 0

        assertFailsWith<RestClientResponseException> {
            handler.executeWithRetry("test") {
                callCount++
                throw create429Exception()
            }
        }

        assertEquals(3, callCount)
    }

    @Test
    fun `parses retry-after from response body`() {
        val responseBody = """{"detail":"Request was throttled. Expected available in 37 seconds."}"""

        val delay = handler.calculateDelay(responseBody, 1)

        assertTrue(delay >= Duration.ofSeconds(38))
        assertTrue(delay <= Duration.ofSeconds(46))
    }

    @Test
    fun `uses exponential backoff when retry-after not parseable`() {
        val handlerWithLongerDelay = RateLimitRetryHandler(
            RetryConfig(
                baseDelay = Duration.ofSeconds(5),
                maxDelay = Duration.ofSeconds(120)
            )
        )

        val delay1 = handlerWithLongerDelay.calculateDelay(null, 1)
        val delay2 = handlerWithLongerDelay.calculateDelay(null, 2)
        val delay3 = handlerWithLongerDelay.calculateDelay(null, 3)

        assertTrue(delay1 >= Duration.ofSeconds(5))
        assertTrue(delay1 <= Duration.ofSeconds(6))
        assertTrue(delay2 >= Duration.ofSeconds(10))
        assertTrue(delay2 <= Duration.ofSeconds(12))
        assertTrue(delay3 >= Duration.ofSeconds(20))
        assertTrue(delay3 <= Duration.ofSeconds(24))
    }

    @Test
    fun `caps delay at max delay`() {
        val handlerWithLowMax = RateLimitRetryHandler(
            RetryConfig(
                baseDelay = Duration.ofSeconds(60),
                maxDelay = Duration.ofSeconds(30)
            )
        )

        val delay = handlerWithLowMax.calculateDelay(null, 5)

        assertTrue(delay <= Duration.ofSeconds(30))
    }

    private fun create429Exception(): RestClientResponseException {
        return RestClientResponseException(
            "Too Many Requests",
            HttpStatusCode.valueOf(429),
            "Too Many Requests",
            null,
            """{"detail":"Request was throttled."}""".toByteArray(),
            null
        )
    }

    private fun create500Exception(): RestClientResponseException {
        return RestClientResponseException(
            "Internal Server Error",
            HttpStatusCode.valueOf(500),
            "Internal Server Error",
            null,
            "Server error".toByteArray(),
            null
        )
    }
}
