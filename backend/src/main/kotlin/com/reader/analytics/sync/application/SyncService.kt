package com.reader.analytics.sync.application

import com.reader.analytics.sync.domain.SyncCursor
import com.reader.analytics.sync.domain.SyncLog
import com.reader.analytics.sync.domain.SyncStatus
import com.reader.analytics.sync.domain.events.DocumentSyncedEvent
import com.reader.analytics.sync.infrastructure.ReadwiseClient
import com.reader.analytics.sync.infrastructure.readwise.dto.DocumentDto
import com.reader.analytics.sync.infrastructure.readwise.dto.isDocument
import com.reader.analytics.sync.infrastructure.readwise.dto.isHighlight
import com.reader.analytics.sync.infrastructure.readwise.dto.isNote
import com.reader.analytics.sync.infrastructure.readwise.dto.toHighlightEvent
import com.reader.analytics.sync.infrastructure.readwise.dto.toNoteEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SyncService(
    private val readwiseClient: ReadwiseClient,
    private val cursorStore: CursorStore,
    private val logStore: LogStore,
    private val eventPublisher: ApplicationEventPublisher
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun sync(): SyncLog {
        val startedAt = Instant.now()
        var log = logStore.save(SyncLog(startedAt = startedAt, status = SyncStatus.RUNNING))

        return try {
            val cursor = cursorStore.findByCursorType("documents")
            val updatedAfter = cursor?.lastSyncedAt

            logger.info("Starting sync [lastSyncedAt={}]", updatedAfter)

            val allItems = readwiseClient.fetchDocuments(updatedAfter).toList()

            // Three-way classification
            val documentDtos = allItems.filter { it.isDocument() }
            val highlightDtos = allItems.filter { it.isHighlight() }
            val noteDtos = allItems.filter { it.isNote() }

            logger.info(
                "Fetched {} documents, {} highlights, {} notes to process",
                documentDtos.size, highlightDtos.size, noteDtos.size
            )

            var latestUpdatedAt: Instant? = null

            // Phase 1: Publish document events
            documentDtos.forEach { doc ->
                val event = doc.toDocumentEvent()
                eventPublisher.publishEvent(event)

                doc.updatedAt?.let { docUpdatedAt ->
                    if (latestUpdatedAt == null || docUpdatedAt.isAfter(latestUpdatedAt)) {
                        latestUpdatedAt = docUpdatedAt
                    }
                }
            }

            // Phase 2: Publish highlight events
            highlightDtos.forEach { highlight ->
                val event = highlight.toHighlightEvent()
                eventPublisher.publishEvent(event)

                highlight.updatedAt?.let { highlightUpdatedAt ->
                    if (latestUpdatedAt == null || highlightUpdatedAt.isAfter(latestUpdatedAt)) {
                        latestUpdatedAt = highlightUpdatedAt
                    }
                }
            }

            // Phase 3: Publish note events
            noteDtos.forEach { note ->
                val event = note.toNoteEvent()
                eventPublisher.publishEvent(event)

                note.updatedAt?.let { noteUpdatedAt ->
                    if (latestUpdatedAt == null || noteUpdatedAt.isAfter(latestUpdatedAt)) {
                        latestUpdatedAt = noteUpdatedAt
                    }
                }
            }

            latestUpdatedAt?.let { timestamp ->
                cursorStore.save(SyncCursor("documents", timestamp, null))
            }

            val duration = System.currentTimeMillis() - startedAt.toEpochMilli()
            logger.info(
                "Sync completed [documents={}, highlights={}, notes={}, latestUpdatedAt={}, duration={}ms]",
                documentDtos.size, highlightDtos.size, noteDtos.size, latestUpdatedAt, duration
            )

            log = log.copy(
                status = SyncStatus.COMPLETED,
                completedAt = Instant.now(),
                documentsProcessed = documentDtos.size,
                highlightsProcessed = highlightDtos.size,
                notesProcessed = noteDtos.size
            )
            logStore.save(log)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startedAt.toEpochMilli()
            val cursor = cursorStore.findByCursorType("documents")
            logger.error(
                "Sync failed [lastSyncedAt={}, duration={}ms]: {}",
                cursor?.lastSyncedAt, duration, e.message, e
            )

            log = log.copy(
                status = SyncStatus.FAILED,
                completedAt = Instant.now(),
                errorMessage = e.message
            )
            logStore.save(log)
        }
    }

    private fun DocumentDto.toDocumentEvent() = DocumentSyncedEvent(
        id = id,
        url = url,
        title = title,
        author = author,
        category = category,
        location = location,
        readingProgress = readingProgress,
        wordCount = wordCount,
        savedAt = savedAt,
        updatedAt = updatedAt,
        firstOpenedAt = firstOpenedAt,
        lastOpenedAt = lastOpenedAt,
        tags = tagKeys(),
        parentId = parentId,
        imageUrl = imageUrl
    )
}
