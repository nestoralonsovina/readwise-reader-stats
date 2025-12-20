package com.reader.analytics.sync.application

import com.reader.analytics.sync.domain.SyncRun
import com.reader.analytics.sync.domain.SyncRunStatus
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock

@Service
class SyncOrchestrator(
    private val syncRunStore: SyncRunStore,
    private val syncExecutor: SyncExecutor
) {
    private val syncLock = ReentrantLock()

    fun startSync(): SyncStartResult {
        syncLock.lock()
        try {
            val activeSync = findActiveSync()
            if (activeSync != null) {
                return SyncStartResult.AlreadyRunning(activeSync.id!!)
            }

            val run = syncRunStore.save(
                SyncRun(
                    status = SyncRunStatus.PENDING,
                    startedAt = Instant.now()
                )
            )

            val syncId = run.id!!
            syncExecutor.executeAsync(syncId)
            return SyncStartResult.Started(syncId)
        } finally {
            syncLock.unlock()
        }
    }

    fun getActiveSync(): SyncRun? = findActiveSync()

    private fun findActiveSync(): SyncRun? {
        return syncRunStore.findByStatus(SyncRunStatus.RUNNING)
            ?: syncRunStore.findByStatus(SyncRunStatus.PENDING)
    }
}
