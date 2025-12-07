package com.reader.analytics.library.infrastructure.persistence

import com.reader.analytics.library.domain.Document
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DocumentRepository : JpaRepository<Document, UUID> {
    fun findByReadwiseId(readwiseId: String): Document?
}
