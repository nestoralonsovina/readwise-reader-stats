package com.reader.analytics.sync.domain.events

import java.time.Instant

data class NoteSyncedEvent(
    val id: String,
    val parentId: String,
    val content: String,
    val createdAt: Instant?
)
