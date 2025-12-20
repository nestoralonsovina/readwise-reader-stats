package com.reader.analytics.library.application

import com.reader.analytics.library.domain.Document
import com.reader.analytics.library.domain.Highlight
import com.reader.analytics.library.domain.Note
import com.reader.analytics.library.domain.Tag
import com.reader.analytics.sync.domain.events.NoteSyncedEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NoteEventListenerTest {

    private lateinit var documentStore: FakeDocumentStore
    private lateinit var listener: NoteEventListener

    @BeforeEach
    fun setUp() {
        documentStore = FakeDocumentStore()
        listener = NoteEventListener(documentStore)
    }

    @Test
    fun `creates new note from sync event`() {
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
        assertEquals("rw-hl-456", saved.parentId)
        assertEquals(Instant.parse("2024-01-15T10:00:00Z"), saved.createdAt)
    }

    @Test
    fun `updates existing note on re-sync`() {
        val existingNote = Note(
            id = UUID.randomUUID(),
            readwiseId = "rw-note-existing",
            parentId = "rw-hl-123",
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
    fun `handles note with null createdAt`() {
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
