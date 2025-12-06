package com.reader.analytics.sync.infrastructure.readwise

import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class RateLimiter(
    private val requestsPerMinute: Int
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val lock = ReentrantLock()
    private val requestTimestamps = ArrayDeque<Long>(requestsPerMinute)
    private val windowMillis = TimeUnit.MINUTES.toMillis(1)

    fun acquire() {
        lock.withLock {
            val now = System.currentTimeMillis()

            // Remove timestamps outside the window
            while (requestTimestamps.isNotEmpty() &&
                now - requestTimestamps.first() > windowMillis) {
                requestTimestamps.removeFirst()
            }

            // If at limit, wait until oldest request exits window
            if (requestTimestamps.size >= requestsPerMinute) {
                val waitTime = windowMillis - (now - requestTimestamps.first()) + 100
                if (waitTime > 0) {
                    logger.debug("Rate limit reached, waiting ${waitTime}ms")
                    Thread.sleep(waitTime)
                }
                requestTimestamps.removeFirst()
            }

            requestTimestamps.addLast(System.currentTimeMillis())
        }
    }
}