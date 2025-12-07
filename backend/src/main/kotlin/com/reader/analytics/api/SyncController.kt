package com.reader.analytics.api

import com.reader.analytics.sync.application.SyncService
import com.reader.analytics.sync.domain.SyncStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/sync")
@Tag(name = "Sync")
class SyncController(private val syncService: SyncService) {

    @PostMapping
    @Operation(
        summary = "Trigger document sync",
        description = """
            Initiates an incremental sync from the Readwise Reader API.

            The sync fetches all documents updated since the last sync cursor,
            processes them through the event pipeline, and updates the local database.

            **Rate limiting**: The Readwise API has a rate limit of 20 requests/minute.
            Large syncs may take several minutes to complete.
        """
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Sync completed (may have succeeded or failed)",
            content = [Content(schema = Schema(implementation = SyncResponse::class))]
        )
    )
    fun triggerSync(): SyncResponse {
        val log = syncService.sync()
        return SyncResponse(
            syncId = log.id!!,
            status = log.status,
            startedAt = log.startedAt,
            completedAt = log.completedAt,
            documentsProcessed = log.documentsProcessed,
            highlightsProcessed = log.highlightsProcessed,
            errorMessage = log.errorMessage
        )
    }
}

@Schema(description = "Result of a sync operation")
data class SyncResponse(
    @Schema(description = "Unique identifier for this sync run", example = "550e8400-e29b-41d4-a716-446655440000")
    val syncId: UUID,

    @Schema(description = "Final status of the sync", example = "COMPLETED")
    val status: SyncStatus,

    @Schema(description = "When the sync started (ISO 8601)", example = "2024-01-15T10:30:00Z")
    val startedAt: Instant,

    @Schema(description = "When the sync completed (null if still running)", example = "2024-01-15T10:32:15Z")
    val completedAt: Instant?,

    @Schema(description = "Number of documents processed", example = "42")
    val documentsProcessed: Int,

    @Schema(description = "Number of highlights processed", example = "156")
    val highlightsProcessed: Int,

    @Schema(description = "Error message if sync failed (null on success)", example = "Rate limit exceeded")
    val errorMessage: String?
)
