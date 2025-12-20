package com.reader.analytics.sync.application

import com.reader.analytics.sync.domain.SyncPhase
import com.reader.analytics.sync.domain.SyncRunStatus
import com.reader.analytics.sync.domain.events.DocumentSyncedEvent
import com.reader.analytics.sync.domain.events.SyncProgressEvent
import com.reader.analytics.sync.infrastructure.ReadwiseClient
import com.reader.analytics.sync.infrastructure.readwise.RateLimitEvent
import com.reader.analytics.sync.infrastructure.readwise.dto.DocumentDto
import com.reader.analytics.sync.infrastructure.readwise.dto.isDocument
import com.reader.analytics.sync.infrastructure.readwise.dto.isHighlight
import com.reader.analytics.sync.infrastructure.readwise.dto.isNote
import com.reader.analytics.sync.infrastructure.readwise.dto.toHighlightEvent
import com.reader.analytics.sync.infrastructure.readwise.dto.toNoteEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class SyncExecutorImpl(
    private val readwiseClient: ReadwiseClient,
    private val cursorStore: CursorStore,
    private val syncRunStore: SyncRunStore,
    private val eventPublisher: ApplicationEventPublisher,
    private val progressEmitter: SyncProgressEmitter
) : SyncExecutor {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Async("syncExecutor")
    override fun executeAsync(syncId: UUID) {
        val run = syncRunStore.findById(syncId)
            ?: throw IllegalStateException("SyncRun not found: $syncId")

        val startedAt = run.startedAt
        var currentRun = syncRunStore.save(run.copy(status = SyncRunStatus.RUNNING))

        // Emit started event
        progressEmitter.emit(syncId, SyncProgressEvent.Started(syncId))

        try {
            val cursor = cursorStore.findByCursorType("documents")
            val updatedAfter = cursor?.lastSyncedAt

            logger.info("Starting async sync [syncId={}, lastSyncedAt={}]", syncId, updatedAfter)

            val allItems = readwiseClient.fetchDocuments(
                updatedAfter = updatedAfter,
                onRateLimited = { event -> handleRateLimited(syncId, event) },
                onRateLimitCleared = { handleRateLimitCleared(syncId) }
            ).toList()

            val documentDtos = allItems.filter { it.isDocument() }
            val highlightDtos = allItems.filter { it.isHighlight() }
            val noteDtos = allItems.filter { it.isNote() }

            logger.info(
                "Fetched {} documents, {} highlights, {} notes to process",
                documentDtos.size, highlightDtos.size, noteDtos.size
            )

            var latestUpdatedAt: Instant? = null

            // Phase 1: Documents
            currentRun = processPhase(
                syncId = syncId,
                currentRun = currentRun,
                phase = SyncPhase.DOCUMENTS,
                phaseNumber = 1,
                items = documentDtos,
                processItem = { doc ->
                    val event = doc.toDocumentEvent()
                    eventPublisher.publishEvent(event)
                    doc.updatedAt?.let { if (latestUpdatedAt == null || it.isAfter(latestUpdatedAt)) latestUpdatedAt = it }
                },
                updateCounts = { run, count -> run.copy(documentsProcessed = count) }
            )

            // Phase 2: Highlights
            currentRun = processPhase(
                syncId = syncId,
                currentRun = currentRun,
                phase = SyncPhase.HIGHLIGHTS,
                phaseNumber = 2,
                items = highlightDtos,
                processItem = { highlight ->
                    val event = highlight.toHighlightEvent()
                    eventPublisher.publishEvent(event)
                    highlight.updatedAt?.let { if (latestUpdatedAt == null || it.isAfter(latestUpdatedAt)) latestUpdatedAt = it }
                },
                updateCounts = { run, count -> run.copy(highlightsProcessed = count) }
            )

            // Phase 3: Notes
            currentRun = processPhase(
                syncId = syncId,
                currentRun = currentRun,
                phase = SyncPhase.NOTES,
                phaseNumber = 3,
                items = noteDtos,
                processItem = { note ->
                    val event = note.toNoteEvent()
                    eventPublisher.publishEvent(event)
                    note.updatedAt?.let { if (latestUpdatedAt == null || it.isAfter(latestUpdatedAt)) latestUpdatedAt = it }
                },
                updateCounts = { run, count -> run.copy(notesProcessed = count) }
            )

            latestUpdatedAt?.let { timestamp ->
                cursorStore.save(com.reader.analytics.sync.domain.SyncCursor("documents", timestamp, null))
            }

            val duration = Duration.between(startedAt, Instant.now())
            logger.info(
                "Sync completed [syncId={}, documents={}, highlights={}, notes={}, duration={}]",
                syncId, documentDtos.size, highlightDtos.size, noteDtos.size, duration
            )

            syncRunStore.save(
                currentRun.copy(
                    status = SyncRunStatus.COMPLETED,
                    completedAt = Instant.now(),
                    currentPhase = null
                )
            )

            // Emit completed event
            progressEmitter.emit(
                syncId,
                SyncProgressEvent.Completed(
                    syncId = syncId,
                    documentsCount = documentDtos.size,
                    highlightsCount = highlightDtos.size,
                    notesCount = noteDtos.size,
                    duration = duration.toString()
                )
            )
        } catch (e: Exception) {
            val duration = Duration.between(startedAt, Instant.now())
            logger.error("Sync failed [syncId={}, duration={}]: {}", syncId, duration, e.message, e)

            syncRunStore.save(
                currentRun.copy(
                    status = SyncRunStatus.FAILED,
                    completedAt = Instant.now(),
                    errorMessage = e.message,
                    errorPhase = currentRun.currentPhase
                )
            )

            // Emit error event
            progressEmitter.emit(
                syncId,
                SyncProgressEvent.Error(
                    syncId = syncId,
                    phase = currentRun.currentPhase,
                    message = e.message ?: "Unknown error"
                )
            )
        }
    }

    private fun <T> processPhase(
        syncId: UUID,
        currentRun: com.reader.analytics.sync.domain.SyncRun,
        phase: SyncPhase,
        phaseNumber: Int,
        items: List<T>,
        processItem: (T) -> Unit,
        updateCounts: (com.reader.analytics.sync.domain.SyncRun, Int) -> com.reader.analytics.sync.domain.SyncRun
    ): com.reader.analytics.sync.domain.SyncRun {
        // Emit phase started
        progressEmitter.emit(
            syncId,
            SyncProgressEvent.PhaseStarted(
                syncId = syncId,
                phase = phase,
                phaseNumber = phaseNumber
            )
        )

        var run = syncRunStore.save(currentRun.copy(currentPhase = phase))

        items.forEachIndexed { index, item ->
            processItem(item)

            // Emit progress every 10 items
            if ((index + 1) % 10 == 0 || index == items.lastIndex) {
                progressEmitter.emit(
                    syncId,
                    SyncProgressEvent.Progress(
                        syncId = syncId,
                        phase = phase,
                        processed = index + 1
                    )
                )
            }
        }

        // Emit phase completed
        progressEmitter.emit(
            syncId,
            SyncProgressEvent.PhaseCompleted(
                syncId = syncId,
                phase = phase,
                count = items.size
            )
        )

        return syncRunStore.save(
            updateCounts(run, items.size).copy(
                completedPhases = phaseNumber
            )
        )
    }

    private fun handleRateLimited(syncId: UUID, event: RateLimitEvent) {
        logger.warn(
            "Rate limited [syncId={}, retryAfter={}s, attempt={}/{}]",
            syncId, event.retryAfterSeconds, event.attempt, event.maxAttempts
        )
        progressEmitter.emit(
            syncId,
            SyncProgressEvent.RateLimited(
                syncId = syncId,
                retryAfter = event.retryAfterSeconds,
                attempt = event.attempt,
                maxAttempts = event.maxAttempts
            )
        )
    }

    private fun handleRateLimitCleared(syncId: UUID) {
        logger.info("Rate limit cleared [syncId={}]", syncId)
        progressEmitter.emit(syncId, SyncProgressEvent.RateLimitCleared(syncId))
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
        imageUrl = imageUrl,
        highlights = emptyList()
    )
}
