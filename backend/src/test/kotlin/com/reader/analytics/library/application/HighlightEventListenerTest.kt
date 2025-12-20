package com.reader.analytics.library.application

import com.reader.analytics.library.domain.Document
import com.reader.analytics.library.domain.Highlight
import com.reader.analytics.library.domain.Note
import com.reader.analytics.library.domain.Tag
import com.reader.analytics.sync.domain.events.HighlightSyncedEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class HighlightEventListenerTest {

    private lateinit var documentStore: FakeDocumentStore
    private lateinit var listener: HighlightEventListener

    @BeforeEach
    fun setUp() {
        documentStore = FakeDocumentStore()
        listener = HighlightEventListener(documentStore)
    }

    @Test
    fun `creates new highlight linked to document`() {
        val document = documentStore.save(
            Document(readwiseId = "rw-doc-456", url = "https://example.com/article")
        )

        val event = HighlightSyncedEvent(
            id = "rw-hl-123",
            documentId = "rw-doc-456",
            text = "This is highlighted text",
            highlightedAt = Instant.parse("2024-01-15T10:00:00Z")
        )

        listener.onHighlightSynced(event)

        val saved = documentStore.findHighlightByReadwiseId("rw-hl-123")
        assertNotNull(saved)
        assertEquals("This is highlighted text", saved.text)
        assertSame(document, saved.document)
        assertEquals(Instant.parse("2024-01-15T10:00:00Z"), saved.highlightedAt)
    }

    @Test
    fun `updates existing highlight on re-sync`() {
        val document = documentStore.save(
            Document(readwiseId = "rw-doc-123", url = "https://example.com/article")
        )
        val existingHighlight = Highlight(
            id = UUID.randomUUID(),
            readwiseId = "rw-hl-existing",
            document = document,
            text = "Original text"
        )
        documentStore.saveHighlight(existingHighlight)

        val event = HighlightSyncedEvent(
            id = "rw-hl-existing",
            documentId = "rw-doc-123",
            text = "Updated text",
            highlightedAt = Instant.parse("2024-01-20T15:00:00Z")
        )

        listener.onHighlightSynced(event)

        val updated = documentStore.findHighlightByReadwiseId("rw-hl-existing")
        assertNotNull(updated)
        assertEquals("Updated text", updated.text)
        assertEquals(existingHighlight.id, updated.id)
    }

    @Test
    fun `fails fast when document not found`() {
        val event = HighlightSyncedEvent(
            id = "rw-hl-orphan",
            documentId = "rw-doc-not-synced",
            text = "Orphan highlight text",
            highlightedAt = Instant.parse("2024-01-10T08:00:00Z")
        )

        val exception = assertThrows<IllegalStateException> {
            listener.onHighlightSynced(event)
        }

        assertEquals(
            "Document not found for highlight. documentId=rw-doc-not-synced, highlightId=rw-hl-orphan. Ensure full sync completes before using app.",
            exception.message
        )
    }

    @Test
    fun `handles highlight without timestamp`() {
        documentStore.save(
            Document(readwiseId = "rw-doc-789", url = "https://example.com/article")
        )

        val event = HighlightSyncedEvent(
            id = "rw-hl-no-timestamp",
            documentId = "rw-doc-789",
            text = "Just highlighted, no timestamp",
            highlightedAt = null
        )

        listener.onHighlightSynced(event)

        val saved = documentStore.findHighlightByReadwiseId("rw-hl-no-timestamp")
        assertNotNull(saved)
        assertEquals("Just highlighted, no timestamp", saved.text)
        assertEquals(null, saved.highlightedAt)
    }

    class FakeDocumentStore : DocumentStore {
        private val documents = mutableMapOf<String, Document>()
        private val tags = mutableMapOf<String, Tag>()
        private val highlights = mutableMapOf<String, Highlight>()
        private val notes = mutableMapOf<String, Note>()

        override fun findByReadwiseId(readwiseId: String): Document? =
            documents[readwiseId]

        override fun save(document: Document): Document {
            if (document.id == null) {
                val field = Document::class.java.getDeclaredField("id")
                field.isAccessible = true
                field.set(document, UUID.randomUUID())
            }
            documents[document.readwiseId] = document
            return document
        }

        override fun findOrCreateTags(tagNames: List<String>): MutableSet<Tag> {
            return tagNames.map { name ->
                tags.getOrPut(name) { Tag(id = UUID.randomUUID(), name = name) }
            }.toMutableSet()
        }

        override fun findHighlightByReadwiseId(readwiseId: String): Highlight? =
            highlights[readwiseId]

        override fun saveHighlight(highlight: Highlight): Highlight {
            if (highlight.id == null) {
                highlight.id = UUID.randomUUID()
            }
            highlights[highlight.readwiseId] = highlight
            return highlight
        }

        override fun findNoteByReadwiseId(readwiseId: String): Note? =
            notes[readwiseId]

        override fun saveNote(note: Note): Note {
            if (note.id == null) {
                note.id = UUID.randomUUID()
            }
            notes[note.readwiseId] = note
            return note
        }
    }
}
