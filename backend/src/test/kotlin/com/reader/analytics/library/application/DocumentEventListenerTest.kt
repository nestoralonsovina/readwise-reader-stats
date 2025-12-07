package com.reader.analytics.library.application

import com.reader.analytics.library.domain.Document
import com.reader.analytics.library.domain.Highlight
import com.reader.analytics.library.domain.Tag
import com.reader.analytics.sync.domain.events.DocumentSyncedEvent
import com.reader.analytics.sync.domain.events.HighlightSyncedEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DocumentEventListenerTest {

    private lateinit var documentStore: FakeDocumentStore
    private lateinit var listener: DocumentEventListener

    @BeforeEach
    fun setUp() {
        documentStore = FakeDocumentStore()
        listener = DocumentEventListener(documentStore)
    }

    @Test
    fun `creates new document from sync event`() {
        val event = createDocumentSyncedEvent(
            id = "rw-123",
            title = "Test Article",
            tags = listOf("kotlin", "spring")
        )

        listener.onDocumentSynced(event)

        val saved = documentStore.findByReadwiseId("rw-123")
        assertNotNull(saved)
        assertEquals("Test Article", saved.title)
        assertEquals(2, saved.tags.size)
    }

    @Test
    fun `updates existing document from sync event`() {
        val existingDoc = Document(
            id = UUID.randomUUID(),
            readwiseId = "rw-456",
            url = "https://example.com/old",
            title = "Old Title",
            readingProgress = 0.0
        )
        documentStore.save(existingDoc)

        val event = createDocumentSyncedEvent(
            id = "rw-456",
            title = "Updated Title",
            readingProgress = 0.75
        )

        listener.onDocumentSynced(event)

        val updated = documentStore.findByReadwiseId("rw-456")
        assertNotNull(updated)
        assertEquals("Updated Title", updated.title)
        assertEquals(0.75, updated.readingProgress)
        assertEquals(existingDoc.id, updated.id)
    }

    @Test
    fun `handles event with no tags`() {
        val event = createDocumentSyncedEvent(
            id = "rw-no-tags",
            title = "No Tags Article",
            tags = emptyList()
        )

        listener.onDocumentSynced(event)

        val saved = documentStore.findByReadwiseId("rw-no-tags")
        assertNotNull(saved)
        assertEquals(0, saved.tags.size)
    }

    @Test
    fun `preserves all document fields from event`() {
        val event = DocumentSyncedEvent(
            id = "rw-full",
            url = "https://example.com/full",
            title = "Full Article",
            author = "Test Author",
            category = "article",
            location = "archive",
            readingProgress = 0.5,
            wordCount = 1500,
            savedAt = Instant.parse("2024-01-10T10:00:00Z"),
            updatedAt = Instant.parse("2024-01-15T10:00:00Z"),
            firstOpenedAt = null,
            lastOpenedAt = null,
            tags = emptyList(),
            parentId = "parent-123",
            highlights = emptyList()
        )

        listener.onDocumentSynced(event)

        val saved = documentStore.findByReadwiseId("rw-full")
        assertNotNull(saved)
        assertEquals("https://example.com/full", saved.url)
        assertEquals("Full Article", saved.title)
        assertEquals("Test Author", saved.author)
        assertEquals("article", saved.category)
        assertEquals("archive", saved.location)
        assertEquals(0.5, saved.readingProgress)
        assertEquals(1500, saved.wordCount)
        assertEquals(Instant.parse("2024-01-10T10:00:00Z"), saved.savedAt)
        assertEquals(Instant.parse("2024-01-15T10:00:00Z"), saved.updatedAt)
        assertEquals("parent-123", saved.parentId)
    }

    @Test
    fun `persists highlights from sync event`() {
        val event = DocumentSyncedEvent(
            id = "rw-with-highlights",
            url = "https://example.com/highlighted",
            title = "Highlighted Article",
            author = null,
            category = null,
            location = null,
            readingProgress = null,
            wordCount = null,
            savedAt = null,
            updatedAt = null,
            firstOpenedAt = null,
            lastOpenedAt = null,
            tags = emptyList(),
            parentId = null,
            highlights = listOf(
                HighlightSyncedEvent(
                    id = "hl-1",
                    documentId = "rw-with-highlights",
                    text = "First highlight",
                    note = "My note",
                    color = "yellow",
                    location = 100,
                    highlightedAt = Instant.parse("2024-01-15T10:00:00Z")
                ),
                HighlightSyncedEvent(
                    id = "hl-2",
                    documentId = "rw-with-highlights",
                    text = "Second highlight",
                    note = null,
                    color = "blue",
                    location = 200,
                    highlightedAt = null
                )
            )
        )

        listener.onDocumentSynced(event)

        val savedDocument = documentStore.findByReadwiseId("rw-with-highlights")
        assertNotNull(savedDocument)

        val highlights = documentStore.findHighlightsByDocument(savedDocument)
        assertEquals(2, highlights.size)

        val hl1 = highlights.find { it.readwiseId == "hl-1" }
        assertNotNull(hl1)
        assertEquals("First highlight", hl1.text)
        assertEquals("My note", hl1.note)
        assertEquals("yellow", hl1.color)
        assertEquals(100, hl1.locationIndex)

        val hl2 = highlights.find { it.readwiseId == "hl-2" }
        assertNotNull(hl2)
        assertEquals("Second highlight", hl2.text)
        assertEquals("blue", hl2.color)
    }

    @Test
    fun `updates existing highlights on re-sync`() {
        val document = Document(
            id = UUID.randomUUID(),
            readwiseId = "rw-update-hl",
            url = "https://example.com/update-hl"
        )
        documentStore.save(document)

        val existingHighlight = Highlight(
            id = UUID.randomUUID(),
            readwiseId = "hl-existing",
            document = document,
            text = "Old text",
            note = "Old note"
        )
        documentStore.saveHighlight(existingHighlight)

        val event = DocumentSyncedEvent(
            id = "rw-update-hl",
            url = "https://example.com/update-hl",
            title = null,
            author = null,
            category = null,
            location = null,
            readingProgress = null,
            wordCount = null,
            savedAt = null,
            updatedAt = null,
            firstOpenedAt = null,
            lastOpenedAt = null,
            tags = emptyList(),
            parentId = null,
            highlights = listOf(
                HighlightSyncedEvent(
                    id = "hl-existing",
                    documentId = "rw-update-hl",
                    text = "Updated text",
                    note = "Updated note",
                    color = "green",
                    location = 50,
                    highlightedAt = null
                )
            )
        )

        listener.onDocumentSynced(event)

        val savedDocument = documentStore.findByReadwiseId("rw-update-hl")
        val highlights = documentStore.findHighlightsByDocument(savedDocument!!)
        assertEquals(1, highlights.size)

        val updated = highlights.first()
        assertEquals(existingHighlight.id, updated.id)
        assertEquals("Updated text", updated.text)
        assertEquals("Updated note", updated.note)
        assertEquals("green", updated.color)
    }

    private fun createDocumentSyncedEvent(
        id: String,
        title: String,
        tags: List<String> = emptyList(),
        readingProgress: Double? = null
    ) = DocumentSyncedEvent(
        id = id,
        url = "https://example.com/$id",
        title = title,
        author = "Test Author",
        category = "article",
        location = "new",
        readingProgress = readingProgress,
        wordCount = 1000,
        savedAt = Instant.now(),
        updatedAt = Instant.now(),
        firstOpenedAt = null,
        lastOpenedAt = null,
        tags = tags,
        parentId = null,
        highlights = emptyList()
    )

    class FakeDocumentStore : DocumentStore {
        private val documents = mutableMapOf<String, Document>()
        private val tags = mutableMapOf<String, Tag>()
        private val highlights = mutableMapOf<String, Highlight>()

        override fun findByReadwiseId(readwiseId: String): Document? =
            documents[readwiseId]

        override fun save(document: Document): Document {
            val id = document.id ?: UUID.randomUUID()
            val saved = document.copy(id = id)
            documents[document.readwiseId] = saved
            return saved
        }

        override fun findOrCreateTags(tagNames: List<String>): MutableSet<Tag> {
            return tagNames.map { name ->
                tags.getOrPut(name) { Tag(id = UUID.randomUUID(), name = name) }
            }.toMutableSet()
        }

        override fun findHighlightByReadwiseId(readwiseId: String): Highlight? =
            highlights[readwiseId]

        override fun findHighlightsByDocument(document: Document): List<Highlight> =
            highlights.values.filter { it.document.readwiseId == document.readwiseId }

        override fun saveHighlight(highlight: Highlight): Highlight {
            val id = highlight.id ?: UUID.randomUUID()
            val saved = highlight.copy(id = id)
            highlights[highlight.readwiseId] = saved
            return saved
        }
    }
}
