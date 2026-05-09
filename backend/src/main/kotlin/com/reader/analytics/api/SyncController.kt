package com.reader.analytics.api

import com.reader.analytics.api.dto.ActiveSyncResponse
import com.reader.analytics.api.dto.SyncConflictResponse
import com.reader.analytics.api.dto.SyncStartResponse
import com.reader.analytics.api.dto.SyncStatusResponse
import com.reader.analytics.sync.application.CancelResult
import com.reader.analytics.sync.application.SyncOrchestrator
import com.reader.analytics.sync.application.SyncProgressEmitter
import com.reader.analytics.sync.application.SyncRunStore
import com.reader.analytics.sync.application.SyncStartResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

@RestController
@RequestMapping("/sync")
@Tag(name = "Sync")
class SyncController(
    private val syncOrchestrator: SyncOrchestrator,
    private val syncRunStore: SyncRunStore,
    private val syncProgressEmitter: SyncProgressEmitter
) {

    @PostMapping
    @Operation(
        summary = "Trigger async document sync",
        description = """
            Initiates an asynchronous incremental sync from the Readwise Reader API.

            Returns immediately with a sync ID. Use the stream endpoint for real-time progress
            or the status endpoint for polling.

            **Rate limiting**: The Readwise API has a rate limit of 20 requests/minute.
            Large syncs may take several minutes to complete.
        """
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "Sync started successfully",
            content = [Content(schema = Schema(implementation = SyncStartResponse::class))]
        ),
        ApiResponse(
            responseCode = "409",
            description = "A sync is already in progress",
            content = [Content(schema = Schema(implementation = SyncConflictResponse::class))]
        )
    )
    fun triggerSync(): ResponseEntity<Any> {
        return when (val result = syncOrchestrator.startSync()) {
            is SyncStartResult.Started -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                    SyncStartResponse(
                        syncId = result.syncId,
                        status = "PENDING",
                        streamUrl = "/sync/${result.syncId}/stream"
                    )
                )

            is SyncStartResult.AlreadyRunning -> ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                    SyncConflictResponse(
                        activeSyncId = result.activeSyncId
                    )
                )
        }
    }

    @GetMapping("/{syncId}")
    @Operation(
        summary = "Get sync status",
        description = "Returns the current status of a sync operation. Use for polling fallback."
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Sync status retrieved",
            content = [Content(schema = Schema(implementation = SyncStatusResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Sync not found"
        )
    )
    fun getSyncStatus(@PathVariable syncId: UUID): ResponseEntity<SyncStatusResponse> {
        val run = syncRunStore.findById(syncId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(SyncStatusResponse.from(run))
    }

    @DeleteMapping("/{syncId}")
    @Operation(
        summary = "Cancel a running sync",
        description = "Cancels a sync that is currently in PENDING or RUNNING state."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Sync cancelled successfully"),
        ApiResponse(responseCode = "404", description = "Sync not found"),
        ApiResponse(responseCode = "409", description = "Sync cannot be cancelled (already completed, failed, or cancelled)")
    )
    fun cancelSync(@PathVariable syncId: UUID): ResponseEntity<Any> {
        return when (val result = syncOrchestrator.cancel(syncId)) {
            is CancelResult.Success -> ResponseEntity.noContent().build()
            is CancelResult.NotFound -> ResponseEntity.notFound().build()
            is CancelResult.NotCancellable -> ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(mapOf("error" to "Sync is in status: ${result.status}"))
        }
    }

    @GetMapping("/history")
    @Operation(
        summary = "Get sync history",
        description = "Returns a list of past sync runs, ordered by start time descending."
    )
    fun getHistory(@RequestParam(defaultValue = "20") limit: Int): ResponseEntity<List<SyncStatusResponse>> {
        val runs = syncRunStore.findRecent(limit)
        return ResponseEntity.ok(runs.map { SyncStatusResponse.from(it) })
    }

    @GetMapping("/active")
    @Operation(
        summary = "Check for active sync",
        description = "Returns information about the currently running sync, if any."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Active sync status",
        content = [Content(schema = Schema(implementation = ActiveSyncResponse::class))]
    )
    fun getActiveSync(): ActiveSyncResponse {
        val activeSync = syncOrchestrator.getActiveSync()
            ?: return ActiveSyncResponse.inactive()

        return ActiveSyncResponse.from(activeSync)
    }

    @GetMapping("/{syncId}/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @Operation(
        summary = "Stream sync progress",
        description = """
            Server-Sent Events stream for real-time sync progress updates.

            Connect using the browser EventSource API or similar SSE client.
            Use the lastEventId query parameter on reconnection to replay missed events.
        """
    )
    @ApiResponse(
        responseCode = "200",
        description = "SSE stream established"
    )
    fun streamProgress(
        @PathVariable syncId: UUID,
        @RequestParam(required = false) lastEventId: Long?
    ): SseEmitter {
        val emitter = SseEmitter(0L)
        return syncProgressEmitter.register(syncId, emitter, lastEventId)
    }
}
