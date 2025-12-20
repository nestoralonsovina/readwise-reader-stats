package com.reader.analytics.sync.application

import com.reader.analytics.sync.domain.SyncCursor
import com.reader.analytics.sync.domain.SyncLog
import com.reader.analytics.sync.domain.SyncStatus
import com.reader.analytics.sync.domain.events.DocumentSyncedEvent
import com.reader.analytics.sync.domain.events.HighlightSyncedEvent
import com.reader.analytics.sync.domain.events.NoteSyncedEvent
import com.reader.analytics.sync.infrastructure.ReadwiseClient
import com.reader.analytics.sync.infrastructure.readwise.dto.DocumentDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

    @Test
    fun `partitions documents and highlights into separate events`() {
        readwiseClient.documents = listOf(
            createDocumentDto("doc-1", title = "Article 1", category = "article"),
            createHighlightDto("hl-1", parentId = "doc-1", content = "Highlighted text")
        )

        syncService.sync()

        assertEquals(2, eventPublisher.publishedEvents.size)
        assertTrue(eventPublisher.publishedEvents[0] is DocumentSyncedEvent)
        assertTrue(eventPublisher.publishedEvents[1] is HighlightSyncedEvent)

        val docEvent = eventPublisher.publishedEvents[0] as DocumentSyncedEvent
        val hlEvent = eventPublisher.publishedEvents[1] as HighlightSyncedEvent
        assertEquals("doc-1", docEvent.id)
        assertEquals("hl-1", hlEvent.id)
        assertEquals("doc-1", hlEvent.documentId)
        assertEquals("Highlighted text", hlEvent.text)
    }

    @Test
    fun `publishes documents before highlights`() {
        readwiseClient.documents = listOf(
            createHighlightDto("hl-first", parentId = "doc-1", content = "First in list"),
            createDocumentDto("doc-1", title = "Article 1", category = "article"),
            createHighlightDto("hl-second", parentId = "doc-1", content = "Second in list")
        )

        syncService.sync()

        assertEquals(3, eventPublisher.publishedEvents.size)
        assertTrue(eventPublisher.publishedEvents[0] is DocumentSyncedEvent)
        assertTrue(eventPublisher.publishedEvents[1] is HighlightSyncedEvent)
        assertTrue(eventPublisher.publishedEvents[2] is HighlightSyncedEvent)
    }

    @Test
    fun `tracks highlightsProcessed in sync log`() {
        readwiseClient.documents = listOf(
            createDocumentDto("doc-1", title = "Article 1"),
            createHighlightDto("hl-1", parentId = "doc-1"),
            createHighlightDto("hl-2", parentId = "doc-1")
        )

        val result = syncService.sync()

        assertEquals(1, result.documentsProcessed)
        assertEquals(2, result.highlightsProcessed)
    }

    @Test
    fun `publishes NoteSyncedEvent for notes`() {
        readwiseClient.documents = listOf(
            createNoteDto(
                id = "note-1",
                parentId = "hl-123",
                content = "This is my annotation"
            )
        )

        syncService.sync()

        assertEquals(1, eventPublisher.publishedEvents.size)
        assertTrue(eventPublisher.publishedEvents[0] is NoteSyncedEvent)

        val noteEvent = eventPublisher.publishedEvents[0] as NoteSyncedEvent
        assertEquals("note-1", noteEvent.id)
        assertEquals("hl-123", noteEvent.parentId)
        assertEquals("This is my annotation", noteEvent.content)
    }

    @Test
    fun `classifies documents, highlights, and notes correctly`() {
        readwiseClient.documents = listOf(
            createDocumentDto("doc-1", title = "Article"),
            createHighlightDto("hl-1", parentId = "doc-1"),
            createNoteDto("note-1", parentId = "hl-1", content = "My note")
        )

        syncService.sync()

        assertEquals(3, eventPublisher.publishedEvents.size)
        assertTrue(eventPublisher.publishedEvents[0] is DocumentSyncedEvent)
        assertTrue(eventPublisher.publishedEvents[1] is HighlightSyncedEvent)
        assertTrue(eventPublisher.publishedEvents[2] is NoteSyncedEvent)
    }

    @Test
    fun `tracks notesProcessed in sync log`() {
        readwiseClient.documents = listOf(
            createDocumentDto("doc-1", title = "Article"),
            createHighlightDto("hl-1", parentId = "doc-1"),
            createNoteDto("note-1", parentId = "hl-1"),
            createNoteDto("note-2", parentId = "doc-1")
        )

        val result = syncService.sync()

        assertEquals(1, result.documentsProcessed)
        assertEquals(1, result.highlightsProcessed)
        assertEquals(2, result.notesProcessed)
    }

    private fun createDocumentDto(
        id: String,
        title: String,
        updatedAt: Instant = Instant.now(),
        firstOpenedAt: Instant? = null,
        lastOpenedAt: Instant? = null,
        category: String = "article",
        parentId: String? = null,
        content: String? = null
    ) = DocumentDto(
        id = id,
        url = "https://example.com/$id",
        sourceUrl = null,
        title = title,
        author = "Test Author",
        source = "web",
        category = category,
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
        parentId = parentId,
        summary = null,
        notes = null,
        content = content,
        imageUrl = null
    )

    private fun createHighlightDto(
        id: String,
        parentId: String,
        content: String = "Highlighted text",
        updatedAt: Instant = Instant.now()
    ) = DocumentDto(
        id = id,
        url = "",  // Highlights don't have URLs
        sourceUrl = null,
        title = null,
        author = null,
        source = null,
        category = "highlight",
        location = null,
        tags = emptyMap(),
        siteName = null,
        wordCount = null,
        readingProgress = null,
        publishedDate = null,
        savedAt = Instant.now(),
        createdAt = Instant.now(),
        updatedAt = updatedAt,
        firstOpenedAt = null,
        lastOpenedAt = null,
        parentId = parentId,
        summary = null,
        notes = null,
        content = content,
        imageUrl = null
    )

    private fun createNoteDto(
        id: String,
        parentId: String,
        content: String = "Note content",
        updatedAt: Instant = Instant.now()
    ) = DocumentDto(
        id = id,
        url = "",
        sourceUrl = null,
        title = null,
        author = null,
        source = null,
        category = "note",
        location = null,
        tags = emptyMap(),
        siteName = null,
        wordCount = null,
        readingProgress = null,
        publishedDate = null,
        savedAt = Instant.now(),
        createdAt = Instant.now(),
        updatedAt = updatedAt,
        firstOpenedAt = null,
        lastOpenedAt = null,
        parentId = parentId,
        summary = null,
        notes = null,
        content = content,
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
