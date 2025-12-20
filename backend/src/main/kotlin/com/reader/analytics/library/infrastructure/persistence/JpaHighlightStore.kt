package com.reader.analytics.library.infrastructure.persistence

import com.reader.analytics.library.application.HighlightStore
import com.reader.analytics.library.domain.Highlight
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JpaHighlightStore(
    private val highlightRepository: HighlightRepository
) : HighlightStore {
    override fun findByDocumentId(documentId: UUID): List<Highlight> =
        highlightRepository.findByDocumentId(documentId)
}
