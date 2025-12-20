package com.reader.analytics.sync.infrastructure

import com.reader.analytics.sync.infrastructure.readwise.RateLimitEvent
import com.reader.analytics.sync.infrastructure.readwise.dto.DocumentDto
import java.time.Instant


interface ReadwiseClient {
    fun fetchDocuments(
        updatedAfter: Instant? = null,
        onRateLimited: ((RateLimitEvent) -> Unit)? = null,
        onRateLimitCleared: (() -> Unit)? = null
    ): Sequence<DocumentDto>
}