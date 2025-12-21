package com.reader.analytics.library.application

import com.reader.analytics.library.domain.DocumentDetail
import com.reader.analytics.library.domain.HighlightDetail
import com.reader.analytics.tracking.application.TrackingStore
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DocumentService(
    private val documentStore: DocumentStore,
    private val highlightStore: HighlightStore,
    private val noteStore: NoteStore,
    private val trackingStore: TrackingStore
) {
    companion object {
        private const val WORDS_PER_MINUTE = 250
    }

    fun getDocumentDetail(id: UUID): DocumentDetail? {
        val document = documentStore.findById(id) ?: return null
        val highlights = highlightStore.findByDocumentId(id)
        val latestSnapshot = trackingStore.findLatestSnapshot(document.readwiseId)

        val highlightDetails = highlights.map { highlight ->
            val note = highlight.id?.let { noteStore.findByHighlightId(it) }
            HighlightDetail(
                id = highlight.id!!,
                text = highlight.text,
                note = note?.content,
                highlightedAt = highlight.highlightedAt
            )
        }

        val notesCount = highlightDetails.count { it.note != null }
        val estimatedReadingTime = (document.wordCount ?: 0) / WORDS_PER_MINUTE

        return DocumentDetail(
            id = document.id!!,
            readwiseId = document.readwiseId,
            url = document.url,
            title = document.title,
            author = document.author,
            category = document.category,
            location = document.location,
            readingProgress = latestSnapshot?.readingProgress,
            wordCount = document.wordCount,
            savedAt = document.savedAt,
            firstOpenedAt = document.firstOpenedAt,
            lastOpenedAt = document.lastOpenedAt,
            imageUrl = document.imageUrl,
            tags = document.tags.map { it.name },
            highlights = highlightDetails,
            highlightCount = highlights.size,
            notesCount = notesCount,
            estimatedReadingTimeMinutes = estimatedReadingTime
        )
    }
}
