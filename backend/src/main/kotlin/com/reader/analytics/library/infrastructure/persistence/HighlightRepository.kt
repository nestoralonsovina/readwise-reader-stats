package com.reader.analytics.library.infrastructure.persistence

import com.reader.analytics.library.domain.Document
import com.reader.analytics.library.domain.Highlight
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface HighlightRepository : JpaRepository<Highlight, UUID> {
    fun findByReadwiseId(readwiseId: String): Highlight?
    fun findByDocument(document: Document): List<Highlight>
}
