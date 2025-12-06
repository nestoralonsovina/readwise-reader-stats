package com.reader.analytics.sync.infrastructure.persistence

import com.reader.analytics.sync.domain.SyncLog
import com.reader.analytics.sync.domain.SyncStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DataJpaTest
class SyncLogRepositoryTest {

    @Autowired
    private lateinit var repository: SyncLogRepository

    @Test
    fun `saves sync log with running status`() {
        val log = SyncLog(
            startedAt = Instant.parse("2024-01-15T10:00:00Z"),
            status = SyncStatus.RUNNING
        )

        val saved = repository.save(log)

        assertNotNull(saved.id)
        assertEquals(SyncStatus.RUNNING, saved.status)
        assertEquals(Instant.parse("2024-01-15T10:00:00Z"), saved.startedAt)
        assertNull(saved.completedAt)
        assertEquals(0, saved.documentsProcessed)
        assertEquals(0, saved.highlightsProcessed)
        assertNull(saved.errorMessage)
    }

    @Test
    fun `updates sync log to completed status with counts`() {
        val log = SyncLog(
            startedAt = Instant.parse("2024-01-15T10:00:00Z"),
            status = SyncStatus.RUNNING
        )
        val saved = repository.save(log)

        val updated = saved.copy(
            status = SyncStatus.COMPLETED,
            completedAt = Instant.parse("2024-01-15T10:05:00Z"),
            documentsProcessed = 42,
            highlightsProcessed = 128
        )
        repository.save(updated)

        val retrieved = repository.findById(saved.id!!).orElse(null)

        assertEquals(SyncStatus.COMPLETED, retrieved?.status)
        assertEquals(Instant.parse("2024-01-15T10:05:00Z"), retrieved?.completedAt)
        assertEquals(42, retrieved?.documentsProcessed)
        assertEquals(128, retrieved?.highlightsProcessed)
    }

    @Test
    fun `updates sync log to failed status with error message`() {
        val log = SyncLog(
            startedAt = Instant.parse("2024-01-15T10:00:00Z"),
            status = SyncStatus.RUNNING
        )
        val saved = repository.save(log)

        val updated = saved.copy(
            status = SyncStatus.FAILED,
            completedAt = Instant.parse("2024-01-15T10:01:00Z"),
            errorMessage = "API rate limit exceeded"
        )
        repository.save(updated)

        val retrieved = repository.findById(saved.id!!).orElse(null)

        assertEquals(SyncStatus.FAILED, retrieved?.status)
        assertEquals("API rate limit exceeded", retrieved?.errorMessage)
    }
}
