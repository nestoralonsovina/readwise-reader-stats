package com.reader.analytics.sync.application

import com.reader.analytics.sync.domain.SyncRun
import com.reader.analytics.sync.domain.SyncRunStatus
import com.reader.analytics.sync.domain.events.SyncProgressEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncOrchestratorTest {

    private lateinit var syncRunStore: FakeSyncRunStore
    private lateinit var syncExecutor: FakeSyncExecutor
    private lateinit var progressEmitter: RecordingProgressEmitter
    private lateinit var orchestrator: SyncOrchestrator

    @BeforeEach
    fun setUp() {
        syncRunStore = FakeSyncRunStore()
        syncExecutor = FakeSyncExecutor()
        progressEmitter = RecordingProgressEmitter()
        orchestrator = SyncOrchestrator(syncRunStore, syncExecutor, progressEmitter)
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
                startedAt = Instant.now()
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
                startedAt = Instant.now()
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
                startedAt = Instant.now()
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
                startedAt = Instant.now()
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
                startedAt = Instant.now()
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
                startedAt = Instant.now()
            )
        )

        val result = orchestrator.getActiveSync()

        assertNull(result)
    }

    @Test
    fun `cancel returns Success for running sync`() {
        val run = syncRunStore.save(
            SyncRun(
                status = SyncRunStatus.RUNNING,
                startedAt = Instant.now()
            )
        )

        val result = orchestrator.cancel(run.id!!)

        assertIs<CancelResult.Success>(result)
        val updated = syncRunStore.findById(run.id!!)
        assertEquals(SyncRunStatus.CANCELLED, updated?.status)
        assertNotNull(updated?.completedAt)
    }

    @Test
    fun `cancel returns Success for pending sync`() {
        val run = syncRunStore.save(
            SyncRun(
                status = SyncRunStatus.PENDING,
                startedAt = Instant.now()
            )
        )

        val result = orchestrator.cancel(run.id!!)

        assertIs<CancelResult.Success>(result)
        val updated = syncRunStore.findById(run.id!!)
        assertEquals(SyncRunStatus.CANCELLED, updated?.status)
    }

    @Test
    fun `cancel emits Cancelled event with reason`() {
        val run = syncRunStore.save(
            SyncRun(
                status = SyncRunStatus.RUNNING,
                startedAt = Instant.now()
            )
        )

        orchestrator.cancel(run.id!!)

        val emittedEvents = progressEmitter.emittedEvents.filter { it.syncId == run.id }
        assertEquals(1, emittedEvents.size)
        val event = emittedEvents.first()
        assertIs<SyncProgressEvent.Cancelled>(event)
        assertEquals("Cancelled by user", event.reason)
    }

    @Test
    fun `cancel returns NotFound for unknown syncId`() {
        val unknownId = UUID.randomUUID()

        val result = orchestrator.cancel(unknownId)

        assertIs<CancelResult.NotFound>(result)
    }

    @Test
    fun `cancel returns NotCancellable for completed sync`() {
        val run = syncRunStore.save(
            SyncRun(
                status = SyncRunStatus.COMPLETED,
                startedAt = Instant.now(),
                completedAt = Instant.now()
            )
        )

        val result = orchestrator.cancel(run.id!!)

        assertIs<CancelResult.NotCancellable>(result)
        assertEquals(SyncRunStatus.COMPLETED, result.status)
    }

    @Test
    fun `cancel returns NotCancellable for failed sync`() {
        val run = syncRunStore.save(
            SyncRun(
                status = SyncRunStatus.FAILED,
                startedAt = Instant.now(),
                completedAt = Instant.now()
            )
        )

        val result = orchestrator.cancel(run.id!!)

        assertIs<CancelResult.NotCancellable>(result)
        assertEquals(SyncRunStatus.FAILED, result.status)
    }

    @Test
    fun `cancel returns NotCancellable for already cancelled sync`() {
        val run = syncRunStore.save(
            SyncRun(
                status = SyncRunStatus.CANCELLED,
                startedAt = Instant.now(),
                completedAt = Instant.now()
            )
        )

        val result = orchestrator.cancel(run.id!!)

        assertIs<CancelResult.NotCancellable>(result)
        assertEquals(SyncRunStatus.CANCELLED, result.status)
    }

    @Test
    fun `cancel does not emit event for NotFound`() {
        orchestrator.cancel(UUID.randomUUID())

        assertTrue(progressEmitter.emittedEvents.isEmpty())
    }

    @Test
    fun `cancel does not emit event for NotCancellable`() {
        val run = syncRunStore.save(
            SyncRun(
                status = SyncRunStatus.COMPLETED,
                startedAt = Instant.now(),
                completedAt = Instant.now()
            )
        )

        orchestrator.cancel(run.id!!)

        assertTrue(progressEmitter.emittedEvents.isEmpty())
    }

    @Test
    fun `findRecent returns syncs ordered by startedAt DESC`() {
        val older = syncRunStore.save(
            SyncRun(
                status = SyncRunStatus.COMPLETED,
                startedAt = Instant.parse("2024-01-01T00:00:00Z")
            )
        )
        val newer = syncRunStore.save(
            SyncRun(
                status = SyncRunStatus.COMPLETED,
                startedAt = Instant.parse("2024-01-15T00:00:00Z")
            )
        )

        val results = syncRunStore.findRecent(10)

        assertEquals(2, results.size)
        assertEquals(newer.id, results[0].id)
        assertEquals(older.id, results[1].id)
    }

    @Test
    fun `findRecent respects limit`() {
        repeat(5) {
            syncRunStore.save(
                SyncRun(
                    status = SyncRunStatus.COMPLETED,
                    startedAt = Instant.now().minusSeconds(it.toLong())
                )
            )
        }

        val results = syncRunStore.findRecent(3)

        assertEquals(3, results.size)
    }

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

        override fun findRecent(limit: Int): List<SyncRun> =
            runs.values.sortedByDescending { it.startedAt }.take(limit)

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

    class RecordingProgressEmitter : SyncProgressEmitter() {
        val emittedEvents = mutableListOf<SyncProgressEvent>()

        override fun emit(syncId: UUID, event: SyncProgressEvent) {
            emittedEvents.add(event)
        }
    }
}