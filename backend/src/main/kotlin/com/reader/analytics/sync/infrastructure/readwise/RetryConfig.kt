package com.reader.analytics.sync.infrastructure.readwise

import java.time.Duration

data class RetryConfig(
    val maxAttempts: Int = 3,
    val baseDelay: Duration = Duration.ofSeconds(5),
    val maxDelay: Duration = Duration.ofSeconds(120),
    val jitterFactor: Double = 0.2
)
