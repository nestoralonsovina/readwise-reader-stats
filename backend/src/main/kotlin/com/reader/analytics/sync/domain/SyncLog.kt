package com.reader.analytics.sync.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sync_logs")
data class SyncLog(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    val startedAt: Instant,
    val completedAt: Instant? = null,
    @Enumerated(EnumType.STRING)
    val status: SyncStatus,
    val documentsProcessed: Int = 0,
    val highlightsProcessed: Int = 0,
    val notesProcessed: Int = 0,
    val errorMessage: String? = null
)
