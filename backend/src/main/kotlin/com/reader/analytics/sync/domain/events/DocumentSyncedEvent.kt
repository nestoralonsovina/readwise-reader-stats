package com.reader.analytics.sync.domain.events

import java.time.Instant

data class DocumentSyncedEvent(
    val id: String,
    val url: String,
    val title: String?,
    val author: String?,
    val category: String?,
    val location: String?,
    val readingProgress: Double?,
    val wordCount: Int?,
    val savedAt: Instant?,
    val updatedAt: Instant?,
    val tags: List<String>,
    val parentId: String?,
    val highlights: List<HighlightSyncedEvent>
)
