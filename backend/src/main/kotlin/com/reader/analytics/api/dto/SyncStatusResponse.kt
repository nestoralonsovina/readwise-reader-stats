package com.reader.analytics.api.dto

import com.reader.analytics.sync.domain.SyncPhase
import com.reader.analytics.sync.domain.SyncRun
import com.reader.analytics.sync.domain.SyncRunStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Schema(description = "Current status of a sync operation")
data class SyncStatusResponse(
    @Schema(description = "Unique identifier for this sync run")
    val syncId: UUID,

    @Schema(description = "Current status of the sync")
    val status: SyncRunStatus,

    @Schema(description = "Current phase being processed")
    val currentPhase: SyncPhase?,

    @Schema(description = "When the sync started")
    val startedAt: Instant,

    @Schema(description = "When the sync completed (null if still running)")
    val completedAt: Instant?,

    @Schema(description = "Progress information")
    val progress: ProgressInfo,

    @Schema(description = "Entity counts processed")
    val counts: CountsInfo,

    @Schema(description = "Rate limit information (null if not rate limited)")
    val rateLimit: RateLimitInfo?,

    @Schema(description = "Duration as ISO 8601 duration string")
    val duration: String?,

    @Schema(description = "Error message if sync failed")
    val errorMessage: String?
) {
    companion object {
        fun from(run: SyncRun): SyncStatusResponse {
            val duration = if (run.completedAt != null) {
                Duration.between(run.startedAt, run.completedAt)
            } else {
                Duration.between(run.startedAt, Instant.now())
            }

            val overallPercent = when {
                run.status == SyncRunStatus.COMPLETED -> 100
                run.completedPhases == 0 -> 0
                else -> (run.completedPhases * 33) + (run.currentPhaseProgress / 3)
            }

            return SyncStatusResponse(
                syncId = run.id!!,
                status = run.status,
                currentPhase = run.currentPhase,
                startedAt = run.startedAt,
                completedAt = run.completedAt,
                progress = ProgressInfo(
                    completedPhases = run.completedPhases,
                    totalPhases = run.totalPhases,
                    currentPhaseProgress = run.currentPhaseProgress,
                    overallPercent = overallPercent
                ),
                counts = CountsInfo(
                    documents = run.documentsProcessed,
                    highlights = run.highlightsProcessed,
                    notes = run.notesProcessed
                ),
                rateLimit = run.lastRateLimitRetrySeconds?.let { retrySeconds ->
                    RateLimitInfo(
                        isLimited = true,
                        retryAfter = retrySeconds,
                        attempt = run.lastRateLimitAttempt ?: 1,
                        maxAttempts = 3
                    )
                },
                duration = duration.toString(),
                errorMessage = run.errorMessage
            )
        }
    }
}

@Schema(description = "Progress information for a sync operation")
data class ProgressInfo(
    @Schema(description = "Number of phases completed")
    val completedPhases: Int,

    @Schema(description = "Total number of phases")
    val totalPhases: Int,

    @Schema(description = "Progress within current phase (0-100)")
    val currentPhaseProgress: Int,

    @Schema(description = "Overall progress percentage (0-100)")
    val overallPercent: Int
)

@Schema(description = "Entity counts for a sync operation")
data class CountsInfo(
    @Schema(description = "Number of documents processed")
    val documents: Int,

    @Schema(description = "Number of highlights processed")
    val highlights: Int,

    @Schema(description = "Number of notes processed")
    val notes: Int
)

@Schema(description = "Rate limit information")
data class RateLimitInfo(
    @Schema(description = "Whether currently rate limited")
    val isLimited: Boolean,

    @Schema(description = "Seconds until retry")
    val retryAfter: Int,

    @Schema(description = "Current retry attempt number")
    val attempt: Int,

    @Schema(description = "Maximum retry attempts")
    val maxAttempts: Int
)
