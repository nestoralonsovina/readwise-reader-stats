package com.reader.analytics.sync.application

import com.reader.analytics.sync.domain.SyncRun
import com.reader.analytics.sync.domain.SyncRunStatus
import com.reader.analytics.sync.domain.events.SyncProgressEvent
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock

@Service
class SyncOrchestrator(
    private val syncRunStore: SyncRunStore,
    private val syncExecutor: SyncExecutor,
    private val progressEmitter: SyncProgressEmitter
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

    fun cancel(syncId: java.util.UUID): CancelResult {
        val syncRun = syncRunStore.findById(syncId)
            ?: return CancelResult.NotFound

        if (syncRun.status != SyncRunStatus.RUNNING && syncRun.status != SyncRunStatus.PENDING) {
            return CancelResult.NotCancellable(syncRun.status)
        }

        val cancelled = syncRun.copy(
            status = SyncRunStatus.CANCELLED,
            completedAt = Instant.now()
        )
        syncRunStore.save(cancelled)

        progressEmitter.emit(syncId, SyncProgressEvent.Cancelled(
            syncId = syncId,
            reason = "Cancelled by user"
        ))

        return CancelResult.Success
    }

    fun getActiveSync(): SyncRun? = findActiveSync()

    private fun findActiveSync(): SyncRun? {
        return syncRunStore.findByStatus(SyncRunStatus.RUNNING)
            ?: syncRunStore.findByStatus(SyncRunStatus.PENDING)
    }
}
