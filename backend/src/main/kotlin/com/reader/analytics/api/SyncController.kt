package com.reader.analytics.api

import com.reader.analytics.sync.application.SyncService
import com.reader.analytics.sync.domain.SyncStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/sync")
class SyncController(private val syncService: SyncService) {

    @PostMapping
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

data class SyncResponse(
    val syncId: UUID,
    val status: SyncStatus,
    val startedAt: Instant,
    val completedAt: Instant?,
    val documentsProcessed: Int,
    val highlightsProcessed: Int,
    val errorMessage: String?
)
