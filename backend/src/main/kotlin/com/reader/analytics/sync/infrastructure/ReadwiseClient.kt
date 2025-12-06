package com.reader.analytics.sync.infrastructure

import com.reader.analytics.sync.infrastructure.readwise.dto.DocumentDto
import java.time.Instant


interface ReadwiseClient {
    fun fetchDocuments(updatedAfter: Instant? = null): Sequence<DocumentDto>
}