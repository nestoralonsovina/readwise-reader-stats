package com.reader.analytics.sync.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "sync_cursors")
data class SyncCursor(
    @Id
    val cursorType: String,
    val lastSyncedAt: Instant,
    val nextPageCursor: String?
)
