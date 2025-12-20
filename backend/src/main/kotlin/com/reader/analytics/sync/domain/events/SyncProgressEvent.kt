package com.reader.analytics.sync.domain.events

import com.reader.analytics.sync.domain.SyncPhase
import java.time.Instant
import java.util.UUID

sealed class SyncProgressEvent {
    abstract val timestamp: Instant
    abstract val syncId: UUID

    data class Started(
        override val syncId: UUID,
        override val timestamp: Instant = Instant.now()
    ) : SyncProgressEvent()

    data class PhaseStarted(
        override val syncId: UUID,
        val phase: SyncPhase,
        val phaseNumber: Int,
        val totalPhases: Int = 4,
        override val timestamp: Instant = Instant.now()
    ) : SyncProgressEvent()

    data class Progress(
        override val syncId: UUID,
        val phase: SyncPhase,
        val processed: Int,
        override val timestamp: Instant = Instant.now()
    ) : SyncProgressEvent()

    data class PageFetched(
        override val syncId: UUID,
        val pageNumber: Int,
        val itemsInPage: Int,
        val totalItemsSoFar: Int,
        val hasMore: Boolean,
        override val timestamp: Instant = Instant.now()
    ) : SyncProgressEvent()

    data class RateLimited(
        override val syncId: UUID,
        val retryAfter: Int,
        val attempt: Int,
        val maxAttempts: Int = 3,
        override val timestamp: Instant = Instant.now()
    ) : SyncProgressEvent()

    data class RateLimitCleared(
        override val syncId: UUID,
        override val timestamp: Instant = Instant.now()
    ) : SyncProgressEvent()

    data class PhaseCompleted(
        override val syncId: UUID,
        val phase: SyncPhase,
        val count: Int,
        override val timestamp: Instant = Instant.now()
    ) : SyncProgressEvent()

    data class Completed(
        override val syncId: UUID,
        val documentsCount: Int,
        val highlightsCount: Int,
        val notesCount: Int,
        val duration: String,
        override val timestamp: Instant = Instant.now()
    ) : SyncProgressEvent()

    data class Error(
        override val syncId: UUID,
        val phase: SyncPhase?,
        val message: String,
        override val timestamp: Instant = Instant.now()
    ) : SyncProgressEvent()

    data class Cancelled(
        override val syncId: UUID,
        val phase: SyncPhase?,
        override val timestamp: Instant = Instant.now()
    ) : SyncProgressEvent()

    fun toJson(): String {
        val type = when (this) {
            is Started -> "started"
            is PhaseStarted -> "phase_started"
            is Progress -> "progress"
            is PageFetched -> "page_fetched"
            is RateLimited -> "rate_limited"
            is RateLimitCleared -> "rate_limit_cleared"
            is PhaseCompleted -> "phase_completed"
            is Completed -> "completed"
            is Error -> "error"
            is Cancelled -> "cancelled"
        }

        val data = when (this) {
            is Started -> """{"type":"$type","syncId":"$syncId","timestamp":"$timestamp"}"""
            is PhaseStarted -> """{"type":"$type","phase":"$phase","phaseNumber":$phaseNumber,"totalPhases":$totalPhases,"timestamp":"$timestamp"}"""
            is Progress -> """{"type":"$type","phase":"$phase","processed":$processed,"timestamp":"$timestamp"}"""
            is PageFetched -> """{"type":"$type","pageNumber":$pageNumber,"itemsInPage":$itemsInPage,"totalItemsSoFar":$totalItemsSoFar,"hasMore":$hasMore,"timestamp":"$timestamp"}"""
            is RateLimited -> """{"type":"$type","retryAfter":$retryAfter,"attempt":$attempt,"maxAttempts":$maxAttempts,"timestamp":"$timestamp"}"""
            is RateLimitCleared -> """{"type":"$type","timestamp":"$timestamp"}"""
            is PhaseCompleted -> """{"type":"$type","phase":"$phase","count":$count,"timestamp":"$timestamp"}"""
            is Completed -> """{"type":"$type","summary":{"documents":$documentsCount,"highlights":$highlightsCount,"notes":$notesCount},"duration":"$duration","timestamp":"$timestamp"}"""
            is Error -> """{"type":"$type","phase":${phase?.let { "\"$it\"" } ?: "null"},"message":"${message.replace("\"", "\\\"")}","timestamp":"$timestamp"}"""
            is Cancelled -> """{"type":"$type","phase":${phase?.let { "\"$it\"" } ?: "null"},"timestamp":"$timestamp"}"""
        }

        return data
    }
}
