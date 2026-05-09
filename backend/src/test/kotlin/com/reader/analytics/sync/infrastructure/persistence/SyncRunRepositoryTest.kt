package com.reader.analytics.sync.infrastructure.persistence

import com.reader.analytics.sync.domain.SyncRun
import com.reader.analytics.sync.domain.SyncRunStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.domain.PageRequest
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DataJpaTest
class SyncRunRepositoryTest {

    @Autowired
    private lateinit var repository: SyncRunRepository

    @Test
    fun `saves and retrieves sync run by id`() {
        val run = SyncRun(
            status = SyncRunStatus.PENDING,
            startedAt = Instant.parse("2024-01-15T10:00:00Z")
        )

        val saved = repository.save(run)

        assertNotNull(saved.id)
        assertEquals(SyncRunStatus.PENDING, saved.status)

        val retrieved = repository.findById(saved.id!!).orElse(null)
        assertNotNull(retrieved)
        assertEquals(SyncRunStatus.PENDING, retrieved.status)
    }

    @Test
    fun `finds first run by status`() {
        val run = SyncRun(
            status = SyncRunStatus.RUNNING,
            startedAt = Instant.parse("2024-01-15T10:00:00Z")
        )
        repository.save(run)

        val found = repository.findFirstByStatus(SyncRunStatus.RUNNING)

        assertNotNull(found)
        assertEquals(SyncRunStatus.RUNNING, found.status)
    }

    @Test
    fun `returns null when no run matches status`() {
        val found = repository.findFirstByStatus(SyncRunStatus.RUNNING)

        assertEquals(null, found)
    }

    @Test
    fun `finds recent runs ordered by startedAt descending`() {
        val older = repository.save(
            SyncRun(
                status = SyncRunStatus.COMPLETED,
                startedAt = Instant.parse("2024-01-01T00:00:00Z")
            )
        )
        val newer = repository.save(
            SyncRun(
                status = SyncRunStatus.COMPLETED,
                startedAt = Instant.parse("2024-01-15T00:00:00Z")
            )
        )

        val results = repository.findAllByOrderByStartedAtDesc(PageRequest.of(0, 10))

        assertEquals(2, results.size)
        assertEquals(newer.id, results[0].id)
        assertEquals(older.id, results[1].id)
    }

    @Test
    fun `counts runs by status`() {
        repository.save(SyncRun(status = SyncRunStatus.COMPLETED, startedAt = Instant.now()))
        repository.save(SyncRun(status = SyncRunStatus.COMPLETED, startedAt = Instant.now()))
        repository.save(SyncRun(status = SyncRunStatus.FAILED, startedAt = Instant.now()))

        assertEquals(2, repository.countByStatus(SyncRunStatus.COMPLETED))
        assertEquals(1, repository.countByStatus(SyncRunStatus.FAILED))
        assertEquals(0, repository.countByStatus(SyncRunStatus.RUNNING))
    }

    @Test
    fun `updates sync run status to cancelled`() {
        val run = repository.save(
            SyncRun(
                status = SyncRunStatus.RUNNING,
                startedAt = Instant.parse("2024-01-15T10:00:00Z")
            )
        )

        val updated = run.copy(
            status = SyncRunStatus.CANCELLED,
            completedAt = Instant.parse("2024-01-15T10:05:00Z")
        )
        repository.save(updated)

        val retrieved = repository.findById(run.id!!).orElse(null)
        assertEquals(SyncRunStatus.CANCELLED, retrieved?.status)
        assertEquals(Instant.parse("2024-01-15T10:05:00Z"), retrieved?.completedAt)
    }
}