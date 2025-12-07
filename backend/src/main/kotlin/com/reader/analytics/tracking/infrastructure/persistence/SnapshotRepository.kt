package com.reader.analytics.tracking.infrastructure.persistence

import com.reader.analytics.tracking.domain.ReadingProgressSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SnapshotRepository : JpaRepository<ReadingProgressSnapshot, UUID> {
    fun findTopByDocumentIdOrderByRecordedAtDesc(documentId: String): ReadingProgressSnapshot?
}
