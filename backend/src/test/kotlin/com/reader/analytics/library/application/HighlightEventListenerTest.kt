package com.reader.analytics.library.application

import com.reader.analytics.library.domain.Document
import com.reader.analytics.library.domain.Highlight
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
            note = "My note",
            highlightedAt = Instant.parse("2024-01-15T10:00:00Z")
        )

        listener.onHighlightSynced(event)

        val saved = documentStore.findHighlightByReadwiseId("rw-hl-123")
        assertNotNull(saved)
        assertEquals("This is highlighted text", saved.text)
        assertEquals("My note", saved.note)
        assertEquals("rw-doc-456", saved.documentReadwiseId)
        assertEquals(Instant.parse("2024-01-15T10:00:00Z"), saved.highlightedAt)
    }

    @Test
    fun `updates existing highlight on re-sync`() {
        val existingHighlight = Highlight(
            id = UUID.randomUUID(),
            readwiseId = "rw-hl-existing",
            documentReadwiseId = "rw-doc-123",
            text = "Original text",
            note = "Original note"
        )
        documentStore.saveHighlight(existingHighlight)

        val event = HighlightSyncedEvent(
            id = "rw-hl-existing",
            documentId = "rw-doc-123",
            text = "Updated text",
            note = "Updated note",
            highlightedAt = Instant.parse("2024-01-20T15:00:00Z")
        )

        listener.onHighlightSynced(event)

        val updated = documentStore.findHighlightByReadwiseId("rw-hl-existing")
        assertNotNull(updated)
        assertEquals("Updated text", updated.text)
        assertEquals("Updated note", updated.note)
        assertEquals(existingHighlight.id, updated.id)
    }

    @Test
    fun `persists highlight without existing parent document`() {
        val event = HighlightSyncedEvent(
            id = "rw-hl-orphan",
            documentId = "rw-doc-not-yet-synced",
            text = "Orphan highlight text",
            note = null,
            highlightedAt = Instant.parse("2024-01-10T08:00:00Z")
        )

        listener.onHighlightSynced(event)

        val saved = documentStore.findHighlightByReadwiseId("rw-hl-orphan")
        assertNotNull(saved)
        assertEquals("rw-doc-not-yet-synced", saved.documentReadwiseId)
        assertEquals("Orphan highlight text", saved.text)
    }

    @Test
    fun `handles highlight without note`() {
        val event = HighlightSyncedEvent(
            id = "rw-hl-no-note",
            documentId = "rw-doc-789",
            text = "Just highlighted, no note",
            note = null,
            highlightedAt = null
        )

        listener.onHighlightSynced(event)

        val saved = documentStore.findHighlightByReadwiseId("rw-hl-no-note")
        assertNotNull(saved)
        assertEquals("Just highlighted, no note", saved.text)
        assertEquals(null, saved.note)
        assertEquals(null, saved.highlightedAt)
    }

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

        override fun saveHighlight(highlight: Highlight): Highlight {
            val id = highlight.id ?: UUID.randomUUID()
            val saved = highlight.copy(id = id)
            highlights[highlight.readwiseId] = saved
            return saved
        }
    }
}
