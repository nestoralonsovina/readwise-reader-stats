package com.reader.analytics.library.application

import com.reader.analytics.library.domain.Document
import com.reader.analytics.library.domain.Highlight
import com.reader.analytics.library.domain.Note
import com.reader.analytics.library.domain.Tag
import com.reader.analytics.sync.domain.events.DocumentSyncedEvent
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
            imageUrl = "https://example.com/image.jpg",
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
        assertEquals("https://example.com/image.jpg", saved.imageUrl)
    }

    private fun createDocumentSyncedEvent(
        id: String,
        title: String,
        tags: List<String> = emptyList(),
        readingProgress: Double? = null,
        imageUrl: String? = null
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
        imageUrl = imageUrl,
        highlights = emptyList()
    )

    class FakeDocumentStore : DocumentStore {
        private val documents = mutableMapOf<String, Document>()
        private val tags = mutableMapOf<String, Tag>()
        private val highlights = mutableMapOf<String, Highlight>()
        private val notes = mutableMapOf<String, Note>()

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

        override fun saveHighlight(highlight: Highlight): Highlight {
            val id = highlight.id ?: UUID.randomUUID()
            val saved = highlight.copy(id = id)
            highlights[highlight.readwiseId] = saved
            return saved
        }

        override fun findNoteByReadwiseId(readwiseId: String): Note? =
            notes[readwiseId]

        override fun saveNote(note: Note): Note {
            val id = note.id ?: UUID.randomUUID()
            val saved = note.copy(id = id)
            notes[note.readwiseId] = saved
            return saved
        }
    }
}
