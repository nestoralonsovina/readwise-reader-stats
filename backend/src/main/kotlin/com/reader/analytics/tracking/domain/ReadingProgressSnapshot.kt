package com.reader.analytics.tracking.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "reading_progress_snapshots")
data class ReadingProgressSnapshot(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    val documentId: String,
    val readingProgress: Double,
    val wordCount: Int?,
    val firstOpenedAt: Instant?,
    val lastOpenedAt: Instant?,
    val recordedAt: Instant
)
