package com.reader.analytics.sync.infrastructure.readwise

import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClientResponseException
import java.time.Duration
import kotlin.random.Random

data class RateLimitEvent(
    val retryAfterSeconds: Int,
    val attempt: Int,
    val maxAttempts: Int
)

data class PageFetchedEvent(
    val pageNumber: Int,
    val itemsInPage: Int,
    val totalItemsSoFar: Int,
    val hasMore: Boolean
)

class RateLimitRetryHandler(
    private val config: RetryConfig = RetryConfig()
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val retryAfterPattern = Regex("""Expected available in (\d+) seconds""")

    fun <T> executeWithRetry(
        operation: String,
        onRateLimited: ((RateLimitEvent) -> Unit)? = null,
        onRateLimitCleared: (() -> Unit)? = null,
        block: () -> T
    ): T {
        var lastException: Exception? = null
        var wasRateLimited = false

        repeat(config.maxAttempts) { attempt ->
            try {
                val result = block()
                if (wasRateLimited) {
                    onRateLimitCleared?.invoke()
                }
                return result
            } catch (e: RestClientResponseException) {
                if (!isRateLimitError(e)) {
                    throw e
                }

                lastException = e
                wasRateLimited = true
                val attemptNumber = attempt + 1

                if (attemptNumber >= config.maxAttempts) {
                    logger.error(
                        "Max retry attempts ({}) exceeded for {} - giving up",
                        config.maxAttempts, operation
                    )
                    throw e
                }

                val delay = calculateDelay(e.responseBodyAsString, attemptNumber)
                val retryAfterSeconds = (delay.toMillis() / 1000).toInt()

                onRateLimited?.invoke(
                    RateLimitEvent(
                        retryAfterSeconds = retryAfterSeconds,
                        attempt = attemptNumber,
                        maxAttempts = config.maxAttempts
                    )
                )

                logger.warn(
                    "Rate limited on {} (attempt {}/{}), waiting {}ms before retry",
                    operation, attemptNumber, config.maxAttempts, delay.toMillis()
                )

                Thread.sleep(delay.toMillis())
            }
        }

        throw lastException ?: IllegalStateException("Retry loop exited unexpectedly")
    }

    private fun isRateLimitError(e: RestClientResponseException): Boolean {
        return e.statusCode.value() == 429
    }

    internal fun calculateDelay(responseBody: String?, attempt: Int): Duration {
        val parsedSeconds = responseBody?.let { body ->
            retryAfterPattern.find(body)?.groupValues?.get(1)?.toLongOrNull()
        }

        val baseDelay = if (parsedSeconds != null) {
            Duration.ofSeconds(parsedSeconds + 1)
        } else {
            val exponentialMs = config.baseDelay.toMillis() * (1L shl (attempt - 1))
            Duration.ofMillis(exponentialMs.coerceAtMost(config.maxDelay.toMillis()))
        }

        val jitterMs = (baseDelay.toMillis() * config.jitterFactor * Random.nextDouble()).toLong()
        val finalDelay = baseDelay.plusMillis(jitterMs)

        return finalDelay.coerceAtMost(config.maxDelay)
    }
}
