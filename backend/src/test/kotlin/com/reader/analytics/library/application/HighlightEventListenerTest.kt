package com.reader.analytics.library.application

import com.reader.analytics.library.domain.Document
import com.reader.analytics.library.domain.Highlight
import com.reader.analytics.library.domain.Note
import com.reader.analytics.library.domain.Tag
import com.reader.analytics.sync.domain.events.HighlightSyncedEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HighlightEventListenerTest {

    private lateinit var documentStore: FakeDocumentStore
    private lateinit var listener: HighlightEventListener

    @BeforeEach
    fun setUp() {
        documentStore = FakeDocumentStore()
        listener = HighlightEventListener(documentStore)
    }

    @Test
    fun `creates new highlight from sync event`() {
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
        assertEquals("rw-doc-456", saved.documentReadwiseId)
        assertEquals(Instant.parse("2024-01-15T10:00:00Z"), saved.highlightedAt)
    }

    @Test
    fun `updates existing highlight on re-sync`() {
        val existingHighlight = Highlight(
            id = UUID.randomUUID(),
            readwiseId = "rw-hl-existing",
            documentReadwiseId = "rw-doc-123",
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
    fun `persists highlight without existing parent document`() {
        val event = HighlightSyncedEvent(
            id = "rw-hl-orphan",
            documentId = "rw-doc-not-yet-synced",
            text = "Orphan highlight text",
            highlightedAt = Instant.parse("2024-01-10T08:00:00Z")
        )

        listener.onHighlightSynced(event)

        val saved = documentStore.findHighlightByReadwiseId("rw-hl-orphan")
        assertNotNull(saved)
        assertEquals("rw-doc-not-yet-synced", saved.documentReadwiseId)
        assertEquals("Orphan highlight text", saved.text)
    }

    @Test
    fun `handles highlight without timestamp`() {
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
