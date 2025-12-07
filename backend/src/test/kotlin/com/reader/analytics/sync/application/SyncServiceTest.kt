package com.reader.analytics.sync.application

import com.reader.analytics.sync.domain.SyncCursor
import com.reader.analytics.sync.domain.SyncLog
import com.reader.analytics.sync.domain.SyncStatus
import com.reader.analytics.sync.domain.events.DocumentSyncedEvent
import com.reader.analytics.sync.infrastructure.ReadwiseClient
import com.reader.analytics.sync.infrastructure.readwise.dto.DocumentDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SyncServiceTest {

    private lateinit var readwiseClient: FakeReadwiseClient
    private lateinit var cursorStore: FakeCursorStore
    private lateinit var logStore: FakeLogStore
    private lateinit var eventPublisher: FakeEventPublisher
    private lateinit var syncService: SyncService

    @BeforeEach
    fun setUp() {
        readwiseClient = FakeReadwiseClient()
        cursorStore = FakeCursorStore()
        logStore = FakeLogStore()
        eventPublisher = FakeEventPublisher()
        syncService = SyncService(readwiseClient, cursorStore, logStore, eventPublisher)
    }

    @Test
    fun `first sync fetches all documents without updatedAfter filter`() {
        readwiseClient.documents = listOf(
            createDocumentDto("doc-1", "Article 1")
        )

        syncService.sync()

        assertEquals(null, readwiseClient.lastUpdatedAfter)
    }

    @Test
    fun `incremental sync uses updatedAfter from cursor`() {
        val lastSyncTime = Instant.parse("2024-01-15T10:00:00Z")
        cursorStore.save(SyncCursor("documents", lastSyncTime, null))
        readwiseClient.documents = listOf(
            createDocumentDto("doc-1", "Article 1")
        )

        syncService.sync()

        assertEquals(lastSyncTime, readwiseClient.lastUpdatedAfter)
    }

    @Test
    fun `publishes DocumentSyncedEvent for each document`() {
        readwiseClient.documents = listOf(
            createDocumentDto("doc-1", "Article 1"),
            createDocumentDto("doc-2", "Article 2")
        )

        syncService.sync()

        assertEquals(2, eventPublisher.publishedEvents.size)
        val event1 = eventPublisher.publishedEvents[0] as DocumentSyncedEvent
        val event2 = eventPublisher.publishedEvents[1] as DocumentSyncedEvent
        assertEquals("doc-1", event1.id)
        assertEquals("Article 1", event1.title)
        assertEquals("doc-2", event2.id)
        assertEquals("Article 2", event2.title)
    }

    @Test
    fun `updates cursor with latest updatedAt after sync`() {
        readwiseClient.documents = listOf(
            createDocumentDto("doc-1", "Article 1", updatedAt = Instant.parse("2024-01-15T10:00:00Z")),
            createDocumentDto("doc-2", "Article 2", updatedAt = Instant.parse("2024-01-16T10:00:00Z"))
        )

        syncService.sync()

        val cursor = cursorStore.findByCursorType("documents")
        assertNotNull(cursor)
        assertEquals(Instant.parse("2024-01-16T10:00:00Z"), cursor.lastSyncedAt)
    }

    @Test
    fun `creates sync log with completed status and counts`() {
        readwiseClient.documents = listOf(
            createDocumentDto("doc-1", "Article 1"),
            createDocumentDto("doc-2", "Article 2")
        )

        val result = syncService.sync()

        assertEquals(SyncStatus.COMPLETED, result.status)
        assertEquals(2, result.documentsProcessed)
        assertNotNull(result.completedAt)
    }

    @Test
    fun `creates sync log with failed status on error`() {
        readwiseClient.shouldThrowError = true

        val result = syncService.sync()

        assertEquals(SyncStatus.FAILED, result.status)
        assertNotNull(result.errorMessage)
        assertNotNull(result.completedAt)
    }

    @Test
    fun `returns empty sync log when no documents to process`() {
        readwiseClient.documents = emptyList()

        val result = syncService.sync()

        assertEquals(SyncStatus.COMPLETED, result.status)
        assertEquals(0, result.documentsProcessed)
    }

    @Test
    fun `includes firstOpenedAt and lastOpenedAt in event`() {
        val firstOpened = Instant.parse("2024-01-10T08:00:00Z")
        val lastOpened = Instant.parse("2024-01-15T20:30:00Z")
        readwiseClient.documents = listOf(
            createDocumentDto(
                id = "doc-1",
                title = "Article 1",
                firstOpenedAt = firstOpened,
                lastOpenedAt = lastOpened
            )
        )

        syncService.sync()

        val event = eventPublisher.publishedEvents[0] as DocumentSyncedEvent
        assertEquals(firstOpened, event.firstOpenedAt)
        assertEquals(lastOpened, event.lastOpenedAt)
    }

    private fun createDocumentDto(
        id: String,
        title: String,
        updatedAt: Instant = Instant.now(),
        firstOpenedAt: Instant? = null,
        lastOpenedAt: Instant? = null
    ) = DocumentDto(
        id = id,
        url = "https://example.com/$id",
        sourceUrl = null,
        title = title,
        author = "Test Author",
        source = "web",
        category = "article",
        location = "new",
        tags = emptyMap(),
        siteName = "Example",
        wordCount = 1000,
        readingProgress = 0.0,
        publishedDate = null,
        savedAt = Instant.now(),
        createdAt = Instant.now(),
        updatedAt = updatedAt,
        firstOpenedAt = firstOpenedAt,
        lastOpenedAt = lastOpenedAt,
        parentId = null,
        summary = null,
        notes = null,
        imageUrl = null
    )

    // Test doubles

    class FakeReadwiseClient : ReadwiseClient {
        var documents: List<DocumentDto> = emptyList()
        var lastUpdatedAfter: Instant? = null
        var shouldThrowError = false

        override fun fetchDocuments(updatedAfter: Instant?): Sequence<DocumentDto> {
            lastUpdatedAfter = updatedAfter
            if (shouldThrowError) {
                throw RuntimeException("API error")
            }
            return documents.asSequence()
        }
    }

    class FakeCursorStore : CursorStore {
        private val cursors = mutableMapOf<String, SyncCursor>()

        override fun findByCursorType(cursorType: String): SyncCursor? = cursors[cursorType]

        override fun save(cursor: SyncCursor): SyncCursor {
            cursors[cursor.cursorType] = cursor
            return cursor
        }
    }

    class FakeLogStore : LogStore {
        private val logs = mutableMapOf<UUID, SyncLog>()

        override fun save(log: SyncLog): SyncLog {
            val id = log.id ?: UUID.randomUUID()
            val saved = log.copy(id = id)
            logs[id] = saved
            return saved
        }
    }

    class FakeEventPublisher : ApplicationEventPublisher {
        val publishedEvents = mutableListOf<Any>()

        override fun publishEvent(event: Any) {
            publishedEvents.add(event)
        }
    }
}
