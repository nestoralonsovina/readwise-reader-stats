package com.reader.analytics.library.application

import com.reader.analytics.library.domain.Document
import com.reader.analytics.sync.domain.events.DocumentSyncedEvent
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
                firstOpenedAt = event.firstOpenedAt,
                lastOpenedAt = event.lastOpenedAt,
                parentId = event.parentId,
                imageUrl = event.imageUrl,
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
                firstOpenedAt = event.firstOpenedAt,
                lastOpenedAt = event.lastOpenedAt,
                parentId = event.parentId,
                imageUrl = event.imageUrl,
                tags = tags
            )
        }

        documentStore.save(document)
    }
}
