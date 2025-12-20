package com.reader.analytics.api.dto

import com.reader.analytics.sync.domain.SyncPhase
import com.reader.analytics.sync.domain.SyncRun
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "Response indicating whether a sync is currently active")
data class ActiveSyncResponse(
    @Schema(description = "Whether a sync is currently running")
    val active: Boolean,

    @Schema(description = "ID of the active sync (null if no active sync)")
    val syncId: UUID? = null,

    @Schema(description = "When the active sync started (null if no active sync)")
    val startedAt: Instant? = null,

    @Schema(description = "Current phase of the active sync (null if no active sync)")
    val currentPhase: SyncPhase? = null,

    @Schema(description = "URL to connect to for real-time progress updates (null if no active sync)")
    val streamUrl: String? = null
) {
    companion object {
        fun inactive() = ActiveSyncResponse(active = false)

        fun from(run: SyncRun) = ActiveSyncResponse(
            active = true,
            syncId = run.id,
            startedAt = run.startedAt,
            currentPhase = run.currentPhase,
            streamUrl = "/sync/${run.id}/stream"
        )
    }
}
