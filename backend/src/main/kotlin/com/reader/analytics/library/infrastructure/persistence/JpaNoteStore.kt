package com.reader.analytics.library.infrastructure.persistence

import com.reader.analytics.library.application.NoteStore
import com.reader.analytics.library.domain.Note
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JpaNoteStore(
    private val noteRepository: NoteRepository
) : NoteStore {
    override fun findByHighlightId(highlightId: UUID): Note? =
        noteRepository.findByHighlightId(highlightId)
}
