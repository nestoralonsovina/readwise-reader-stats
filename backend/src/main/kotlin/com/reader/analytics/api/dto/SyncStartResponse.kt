package com.reader.analytics.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Response when starting a sync operation")
data class SyncStartResponse(
    @Schema(description = "Unique identifier for this sync run", example = "550e8400-e29b-41d4-a716-446655440000")
    val syncId: UUID,

    @Schema(description = "Initial status of the sync", example = "PENDING")
    val status: String,

    @Schema(description = "URL to connect to for real-time progress updates", example = "/sync/550e8400-e29b-41d4-a716-446655440000/stream")
    val streamUrl: String
)
