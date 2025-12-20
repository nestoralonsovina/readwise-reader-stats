package com.reader.analytics.library.infrastructure.persistence

import com.reader.analytics.library.domain.Note
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NoteRepository : JpaRepository<Note, UUID> {
    fun findByReadwiseId(readwiseId: String): Note?
    fun findByHighlightId(highlightId: UUID): Note?
}
