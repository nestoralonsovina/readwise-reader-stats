package com.reader.analytics.sync.infrastructure.persistence

import com.reader.analytics.sync.domain.SyncCursor
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DataJpaTest
class SyncCursorRepositoryTest {

    @Autowired
    private lateinit var repository: SyncCursorRepository

    @Test
    fun `returns null when cursor does not exist for type`() {
        val cursor = repository.findByCursorType("documents")

        assertNull(cursor)
    }

    @Test
    fun `saves and retrieves cursor by type`() {
        val cursor = SyncCursor(
            cursorType = "documents",
            lastSyncedAt = Instant.parse("2024-01-15T10:00:00Z"),
            nextPageCursor = null
        )
        repository.save(cursor)

        val retrieved = repository.findByCursorType("documents")

        assertEquals("documents", retrieved?.cursorType)
        assertEquals(Instant.parse("2024-01-15T10:00:00Z"), retrieved?.lastSyncedAt)
        assertNull(retrieved?.nextPageCursor)
    }

    @Test
    fun `updates existing cursor`() {
        val cursor = SyncCursor(
            cursorType = "documents",
            lastSyncedAt = Instant.parse("2024-01-15T10:00:00Z"),
            nextPageCursor = null
        )
        repository.save(cursor)

        val updated = cursor.copy(
            lastSyncedAt = Instant.parse("2024-01-16T10:00:00Z"),
            nextPageCursor = "abc123"
        )
        repository.save(updated)

        val retrieved = repository.findByCursorType("documents")

        assertEquals(Instant.parse("2024-01-16T10:00:00Z"), retrieved?.lastSyncedAt)
        assertEquals("abc123", retrieved?.nextPageCursor)
    }
}
