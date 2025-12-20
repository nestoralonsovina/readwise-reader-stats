package com.reader.analytics.library.application

import com.reader.analytics.library.domain.Document
import com.reader.analytics.library.domain.Highlight
import com.reader.analytics.library.domain.Note
import com.reader.analytics.library.domain.Tag
import com.reader.analytics.sync.domain.events.NoteSyncedEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class NoteEventListenerTest {

    private lateinit var documentStore: FakeDocumentStore
    private lateinit var listener: NoteEventListener

    @BeforeEach
    fun setUp() {
        documentStore = FakeDocumentStore()
        listener = NoteEventListener(documentStore)
    }

    @Test
    fun `creates note linked to highlight`() {
        val document = documentStore.save(
            Document(readwiseId = "rw-doc-123", url = "https://example.com/article")
        )
        val highlight = documentStore.saveHighlight(
            Highlight(readwiseId = "rw-hl-456", document = document, text = "Some text")
        )

        val event = NoteSyncedEvent(
            id = "rw-note-123",
            parentId = "rw-hl-456",
            content = "This is my annotation",
            createdAt = Instant.parse("2024-01-15T10:00:00Z")
        )

        listener.onNoteSynced(event)

        val saved = documentStore.findNoteByReadwiseId("rw-note-123")
        assertNotNull(saved)
        assertEquals("This is my annotation", saved.content)
        assertSame(highlight, saved.highlight)
        assertNull(saved.document)
        assertEquals(Instant.parse("2024-01-15T10:00:00Z"), saved.createdAt)
    }

    @Test
    fun `creates note linked to document`() {
        val document = documentStore.save(
            Document(readwiseId = "rw-doc-789", url = "https://example.com/article")
        )

        val event = NoteSyncedEvent(
            id = "rw-note-doc",
            parentId = "rw-doc-789",
            content = "Note directly on document",
            createdAt = Instant.parse("2024-01-15T10:00:00Z")
        )

        listener.onNoteSynced(event)

        val saved = documentStore.findNoteByReadwiseId("rw-note-doc")
        assertNotNull(saved)
        assertEquals("Note directly on document", saved.content)
        assertSame(document, saved.document)
        assertNull(saved.highlight)
    }

    @Test
    fun `updates existing note on re-sync`() {
        val document = documentStore.save(
            Document(readwiseId = "rw-doc-123", url = "https://example.com/article")
        )
        val highlight = documentStore.saveHighlight(
            Highlight(readwiseId = "rw-hl-123", document = document, text = "Some text")
        )
        val existingNote = Note(
            id = UUID.randomUUID(),
            readwiseId = "rw-note-existing",
            highlight = highlight,
            content = "Original content"
        )
        documentStore.saveNote(existingNote)

        val event = NoteSyncedEvent(
            id = "rw-note-existing",
            parentId = "rw-hl-123",
            content = "Updated content",
            createdAt = Instant.parse("2024-01-20T15:00:00Z")
        )

        listener.onNoteSynced(event)

        val updated = documentStore.findNoteByReadwiseId("rw-note-existing")
        assertNotNull(updated)
        assertEquals("Updated content", updated.content)
        assertEquals(existingNote.id, updated.id)
    }

    @Test
    fun `fails fast when parent not found`() {
        val event = NoteSyncedEvent(
            id = "rw-note-orphan",
            parentId = "rw-unknown-parent",
            content = "Orphan note",
            createdAt = null
        )

        val exception = assertThrows<IllegalStateException> {
            listener.onNoteSynced(event)
        }

        assertEquals(
            "Parent not found for note. parentId=rw-unknown-parent. Expected either a Document or Highlight with this readwiseId. Ensure full sync completes before using app.",
            exception.message
        )
    }

    @Test
    fun `handles note with null createdAt`() {
        val document = documentStore.save(
            Document(readwiseId = "rw-doc-789", url = "https://example.com/article")
        )

        val event = NoteSyncedEvent(
            id = "rw-note-no-date",
            parentId = "rw-doc-789",
            content = "Note without timestamp",
            createdAt = null
        )

        listener.onNoteSynced(event)

        val saved = documentStore.findNoteByReadwiseId("rw-note-no-date")
        assertNotNull(saved)
        assertEquals("Note without timestamp", saved.content)
        assertEquals(null, saved.createdAt)
    }

    class FakeDocumentStore : DocumentStore {
        private val documents = mutableMapOf<String, Document>()
        private val documentsById = mutableMapOf<UUID, Document>()
        private val tags = mutableMapOf<String, Tag>()
        private val highlights = mutableMapOf<String, Highlight>()
        private val notes = mutableMapOf<String, Note>()

        override fun findByReadwiseId(readwiseId: String): Document? =
            documents[readwiseId]

        override fun findById(id: UUID): Document? =
            documentsById[id]

        override fun save(document: Document): Document {
            val id = document.id ?: UUID.randomUUID()
            if (document.id == null) {
                val field = Document::class.java.getDeclaredField("id")
                field.isAccessible = true
                field.set(document, id)
            }
            documents[document.readwiseId] = document
            documentsById[id] = document
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
