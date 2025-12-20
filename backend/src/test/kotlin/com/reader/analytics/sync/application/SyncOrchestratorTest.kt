package com.reader.analytics.sync.application

import com.reader.analytics.sync.domain.SyncRun
import com.reader.analytics.sync.domain.SyncRunStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SyncOrchestratorTest {

    private lateinit var syncRunStore: FakeSyncRunStore
    private lateinit var syncExecutor: FakeSyncExecutor
    private lateinit var orchestrator: SyncOrchestrator

    @BeforeEach
    fun setUp() {
        syncRunStore = FakeSyncRunStore()
        syncExecutor = FakeSyncExecutor()
        orchestrator = SyncOrchestrator(syncRunStore, syncExecutor)
    }

    @Test
    fun `startSync creates pending run and returns Started when no active sync`() {
        val result = orchestrator.startSync()

        assertIs<SyncStartResult.Started>(result)
        assertNotNull(result.syncId)

        val savedRun = syncRunStore.findById(result.syncId)
        assertNotNull(savedRun)
        assertEquals(SyncRunStatus.PENDING, savedRun.status)
    }

    @Test
    fun `startSync triggers async execution`() {
        val result = orchestrator.startSync()

        assertIs<SyncStartResult.Started>(result)
        assertEquals(result.syncId, syncExecutor.executedSyncId)
    }

    @Test
    fun `startSync returns AlreadyRunning when sync is in progress`() {
        val existingRun = syncRunStore.save(
            SyncRun(
                status = SyncRunStatus.RUNNING,
                startedAt = java.time.Instant.now()
            )
        )

        val result = orchestrator.startSync()

        assertIs<SyncStartResult.AlreadyRunning>(result)
        assertEquals(existingRun.id, result.activeSyncId)
    }

    @Test
    fun `startSync returns AlreadyRunning when sync is pending`() {
        val existingRun = syncRunStore.save(
            SyncRun(
                status = SyncRunStatus.PENDING,
                startedAt = java.time.Instant.now()
            )
        )

        val result = orchestrator.startSync()

        assertIs<SyncStartResult.AlreadyRunning>(result)
        assertEquals(existingRun.id, result.activeSyncId)
    }

    @Test
    fun `startSync allows new sync when previous completed`() {
        syncRunStore.save(
            SyncRun(
                status = SyncRunStatus.COMPLETED,
                startedAt = java.time.Instant.now()
            )
        )

        val result = orchestrator.startSync()

        assertIs<SyncStartResult.Started>(result)
    }

    @Test
    fun `startSync allows new sync when previous failed`() {
        syncRunStore.save(
            SyncRun(
                status = SyncRunStatus.FAILED,
                startedAt = java.time.Instant.now()
            )
        )

        val result = orchestrator.startSync()

        assertIs<SyncStartResult.Started>(result)
    }

    @Test
    fun `getActiveSync returns active sync when running`() {
        val activeRun = syncRunStore.save(
            SyncRun(
                status = SyncRunStatus.RUNNING,
                startedAt = java.time.Instant.now()
            )
        )

        val result = orchestrator.getActiveSync()

        assertNotNull(result)
        assertEquals(activeRun.id, result.id)
    }

    @Test
    fun `getActiveSync returns null when no active sync`() {
        syncRunStore.save(
            SyncRun(
                status = SyncRunStatus.COMPLETED,
                startedAt = java.time.Instant.now()
            )
        )

        val result = orchestrator.getActiveSync()

        assertEquals(null, result)
    }

    // Test doubles

    class FakeSyncRunStore : SyncRunStore {
        private val runs = mutableMapOf<UUID, SyncRun>()

        override fun save(run: SyncRun): SyncRun {
            val id = run.id ?: UUID.randomUUID()
            val saved = run.copy(id = id)
            runs[id] = saved
            return saved
        }

        override fun findById(id: UUID): SyncRun? = runs[id]

        override fun findByStatus(status: SyncRunStatus): SyncRun? =
            runs.values.find { it.status == status }

        override fun findAllOrderByStartedAtDesc(limit: Int, offset: Int): List<SyncRun> =
            runs.values.sortedByDescending { it.startedAt }.drop(offset).take(limit)

        override fun countAll(): Long = runs.size.toLong()

        override fun countByStatus(status: SyncRunStatus): Long =
            runs.values.count { it.status == status }.toLong()
    }

    class FakeSyncExecutor : SyncExecutor {
        var executedSyncId: UUID? = null

        override fun executeAsync(syncId: UUID) {
            executedSyncId = syncId
        }
    }
}
