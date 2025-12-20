package com.reader.analytics.sync.infrastructure.persistence

import com.reader.analytics.sync.domain.SyncRun
import com.reader.analytics.sync.domain.SyncRunStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SyncRunRepository : JpaRepository<SyncRun, UUID> {
    fun findFirstByStatus(status: SyncRunStatus): SyncRun?
    fun findAllByOrderByStartedAtDesc(pageable: Pageable): List<SyncRun>
    fun countByStatus(status: SyncRunStatus): Long
}
