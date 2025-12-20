package com.reader.analytics.library.application

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
        val existingNote = documentStore.findNoteByReadwiseId(event.id)

        val note = if (existingNote != null) {
            existingNote.copy(
                parentId = event.parentId,
                content = event.content,
                createdAt = event.createdAt
            )
        } else {
            Note(
                readwiseId = event.id,
                parentId = event.parentId,
                content = event.content,
                createdAt = event.createdAt
            )
        }

        documentStore.saveNote(note)
    }
}
