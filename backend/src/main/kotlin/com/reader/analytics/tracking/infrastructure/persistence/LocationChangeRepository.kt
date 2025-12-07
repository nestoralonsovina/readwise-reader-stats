package com.reader.analytics.tracking.infrastructure.persistence

import com.reader.analytics.tracking.domain.LocationChange
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LocationChangeRepository : JpaRepository<LocationChange, UUID> {
    fun findTopByDocumentIdOrderByChangedAtDesc(documentId: String): LocationChange?
}
