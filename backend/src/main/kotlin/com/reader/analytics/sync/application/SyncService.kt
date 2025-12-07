package com.reader.analytics.sync.application

import com.reader.analytics.sync.domain.SyncCursor
import com.reader.analytics.sync.domain.SyncLog
import com.reader.analytics.sync.domain.SyncStatus
import com.reader.analytics.sync.domain.events.DocumentSyncedEvent
import com.reader.analytics.sync.domain.events.HighlightSyncedEvent
import com.reader.analytics.sync.infrastructure.ReadwiseClient
import com.reader.analytics.sync.infrastructure.readwise.dto.DocumentDto
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

    fun sync(): SyncLog {
        val startedAt = Instant.now()
        var log = logStore.save(SyncLog(startedAt = startedAt, status = SyncStatus.RUNNING))

        return try {
            val cursor = cursorStore.findByCursorType("documents")
            val updatedAfter = cursor?.lastSyncedAt

            val documents = readwiseClient.fetchDocuments(updatedAfter).toList()
            var latestUpdatedAt: Instant? = null

            documents.forEach { doc ->
                val event = doc.toEvent()
                eventPublisher.publishEvent(event)

                doc.updatedAt?.let { docUpdatedAt ->
                    if (latestUpdatedAt == null || docUpdatedAt.isAfter(latestUpdatedAt)) {
                        latestUpdatedAt = docUpdatedAt
                    }
                }
            }

            latestUpdatedAt?.let { timestamp ->
                cursorStore.save(SyncCursor("documents", timestamp, null))
            }

            log = log.copy(
                status = SyncStatus.COMPLETED,
                completedAt = Instant.now(),
                documentsProcessed = documents.size
            )
            logStore.save(log)
        } catch (e: Exception) {
            log = log.copy(
                status = SyncStatus.FAILED,
                completedAt = Instant.now(),
                errorMessage = e.message
            )
            logStore.save(log)
        }
    }

    private fun DocumentDto.toEvent() = DocumentSyncedEvent(
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
        highlights = emptyList() // TODO: Add highlight extraction when available
    )
}
