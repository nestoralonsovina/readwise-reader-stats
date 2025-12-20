package com.reader.analytics.library.application

import com.reader.analytics.library.domain.Highlight
import com.reader.analytics.sync.domain.events.HighlightSyncedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class HighlightEventListener(
    private val documentStore: DocumentStore
) {

    @EventListener
    @Transactional
    fun onHighlightSynced(event: HighlightSyncedEvent) {
        val document = documentStore.findByReadwiseId(event.documentId)
            ?: throw IllegalStateException(
                "Document not found for highlight. " +
                "documentId=${event.documentId}, highlightId=${event.id}. " +
                "Ensure full sync completes before using app."
            )

        val existingHighlight = documentStore.findHighlightByReadwiseId(event.id)

        if (existingHighlight != null) {
            existingHighlight.document = document
            existingHighlight.text = event.text
            existingHighlight.highlightedAt = event.highlightedAt
            documentStore.saveHighlight(existingHighlight)
        } else {
            val highlight = Highlight(
                readwiseId = event.id,
                document = document,
                text = event.text,
                highlightedAt = event.highlightedAt
            )
            documentStore.saveHighlight(highlight)
        }
    }
}
