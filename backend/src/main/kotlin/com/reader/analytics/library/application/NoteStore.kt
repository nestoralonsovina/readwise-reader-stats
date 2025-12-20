package com.reader.analytics.library.application

import com.reader.analytics.library.domain.Note
import java.util.UUID

interface NoteStore {
    fun findByHighlightId(highlightId: UUID): Note?
}
