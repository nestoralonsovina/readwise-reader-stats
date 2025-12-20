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
        val existingHighlight = documentStore.findHighlightByReadwiseId(event.id)

        val highlight = if (existingHighlight != null) {
            existingHighlight.copy(
                text = event.text,
                note = event.note,
                highlightedAt = event.highlightedAt
            )
        } else {
            Highlight(
                readwiseId = event.id,
                documentReadwiseId = event.documentId,
                text = event.text,
                note = event.note,
                highlightedAt = event.highlightedAt
            )
        }

        documentStore.saveHighlight(highlight)
    }
}
