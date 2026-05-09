package com.reader.analytics.sync.application

import com.reader.analytics.sync.domain.SyncRun
import com.reader.analytics.sync.domain.SyncRunStatus
import java.util.UUID

interface SyncRunStore {
    fun save(run: SyncRun): SyncRun
    fun findById(id: UUID): SyncRun?
    fun findByStatus(status: SyncRunStatus): SyncRun?
    fun findRecent(limit: Int): List<SyncRun>
    fun findAllOrderByStartedAtDesc(limit: Int, offset: Int): List<SyncRun>
    fun countAll(): Long
    fun countByStatus(status: SyncRunStatus): Long
}
