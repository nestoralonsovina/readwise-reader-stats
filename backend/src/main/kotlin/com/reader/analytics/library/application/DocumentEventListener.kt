package com.reader.analytics.library.application

import com.reader.analytics.library.domain.Document
import com.reader.analytics.library.domain.Highlight
import com.reader.analytics.sync.domain.events.DocumentSyncedEvent
import com.reader.analytics.sync.domain.events.HighlightSyncedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DocumentEventListener(
    private val documentStore: DocumentStore
) {

    @EventListener
    @Transactional
    fun onDocumentSynced(event: DocumentSyncedEvent) {
        val existingDocument = documentStore.findByReadwiseId(event.id)
        val tags = documentStore.findOrCreateTags(event.tags)

        val document = if (existingDocument != null) {
            existingDocument.copy(
                url = event.url,
                title = event.title,
                author = event.author,
                category = event.category,
                location = event.location,
                readingProgress = event.readingProgress,
                wordCount = event.wordCount,
                savedAt = event.savedAt,
                updatedAt = event.updatedAt,
                parentId = event.parentId,
                tags = tags
            )
        } else {
            Document(
                readwiseId = event.id,
                url = event.url,
                title = event.title,
                author = event.author,
                category = event.category,
                location = event.location,
                readingProgress = event.readingProgress,
                wordCount = event.wordCount,
                savedAt = event.savedAt,
                updatedAt = event.updatedAt,
                parentId = event.parentId,
                tags = tags
            )
        }

        val savedDocument = documentStore.save(document)

        event.highlights.forEach { highlightEvent ->
            saveHighlight(highlightEvent, savedDocument)
        }
    }

    private fun saveHighlight(event: HighlightSyncedEvent, document: Document) {
        val existingHighlight = documentStore.findHighlightByReadwiseId(event.id)

        val highlight = if (existingHighlight != null) {
            existingHighlight.copy(
                text = event.text,
                note = event.note,
                color = event.color,
                locationIndex = event.location,
                highlightedAt = event.highlightedAt
            )
        } else {
            Highlight(
                readwiseId = event.id,
                document = document,
                text = event.text,
                note = event.note,
                color = event.color,
                locationIndex = event.location,
                highlightedAt = event.highlightedAt
            )
        }

        documentStore.saveHighlight(highlight)
    }
}
