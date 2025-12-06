package com.reader.analytics.sync.domain.events

import java.time.Instant

data class HighlightSyncedEvent(
    val id: String,
    val documentId: String,
    val text: String,
    val note: String?,
    val color: String?,
    val location: Int?,
    val highlightedAt: Instant?
)
