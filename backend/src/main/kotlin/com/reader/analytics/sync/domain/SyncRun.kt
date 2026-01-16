package com.reader.analytics.sync.domain

import jakarta.persistence.Column
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
@Table(name = "sync_runs")
data class SyncRun(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Enumerated(EnumType.STRING)
    val status: SyncRunStatus,

    @Enumerated(EnumType.STRING)
    val currentPhase: SyncPhase? = null,

    val startedAt: Instant,
    val completedAt: Instant? = null,

    val totalPhases: Int = 3,
    val completedPhases: Int = 0,
    val currentPhaseProgress: Int = 0,

    val documentsProcessed: Int = 0,
    val highlightsProcessed: Int = 0,
    val notesProcessed: Int = 0,

    val rateLimitHits: Int = 0,
    val lastRateLimitRetrySeconds: Int? = null,
    val lastRateLimitAttempt: Int? = null,

    @Column(columnDefinition = "TEXT")
    val errorMessage: String? = null,

    @Enumerated(EnumType.STRING)
    val errorPhase: SyncPhase? = null
)
