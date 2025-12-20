package com.reader.analytics.library.application

import com.reader.analytics.library.domain.Document
import com.reader.analytics.library.domain.Highlight
import com.reader.analytics.library.domain.Note
import com.reader.analytics.sync.domain.events.NoteSyncedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class NoteEventListener(
    private val documentStore: DocumentStore
) {

    @EventListener
    @Transactional
    fun onNoteSynced(event: NoteSyncedEvent) {
        val (document, highlight) = resolveParent(event.parentId)

        val existingNote = documentStore.findNoteByReadwiseId(event.id)

        if (existingNote != null) {
            existingNote.document = document
            existingNote.highlight = highlight
            existingNote.content = event.content
            existingNote.createdAt = event.createdAt
            documentStore.saveNote(existingNote)
        } else {
            val note = Note(
                readwiseId = event.id,
                document = document,
                highlight = highlight,
                content = event.content,
                createdAt = event.createdAt
            )
            documentStore.saveNote(note)
        }
    }

    private fun resolveParent(parentId: String): Pair<Document?, Highlight?> {
        val highlight = documentStore.findHighlightByReadwiseId(parentId)
        if (highlight != null) {
            return null to highlight
        }

        val document = documentStore.findByReadwiseId(parentId)
        if (document != null) {
            return document to null
        }

        throw IllegalStateException(
            "Parent not found for note. parentId=$parentId. " +
            "Expected either a Document or Highlight with this readwiseId. " +
            "Ensure full sync completes before using app."
        )
    }
}
