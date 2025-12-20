package com.reader.analytics.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Response when a sync is already in progress")
data class SyncConflictResponse(
    @Schema(description = "Error code", example = "SYNC_IN_PROGRESS")
    val error: String = "SYNC_IN_PROGRESS",

    @Schema(description = "ID of the currently active sync", example = "550e8400-e29b-41d4-a716-446655440000")
    val activeSyncId: UUID,

    @Schema(description = "Human-readable error message", example = "A sync is already in progress")
    val message: String = "A sync is already in progress"
)
