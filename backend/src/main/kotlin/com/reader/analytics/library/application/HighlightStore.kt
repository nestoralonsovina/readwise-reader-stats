package com.reader.analytics.library.application

import com.reader.analytics.library.domain.Highlight
import java.util.UUID

interface HighlightStore {
    fun findByDocumentId(documentId: UUID): List<Highlight>
}
