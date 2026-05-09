package com.reader.analytics.library.application

import com.reader.analytics.library.domain.Document
import com.reader.analytics.library.domain.Highlight
import com.reader.analytics.library.domain.Note
import com.reader.analytics.library.domain.Tag
import com.reader.analytics.tracking.application.TrackingStore
import com.reader.analytics.tracking.domain.LocationChange
import com.reader.analytics.tracking.domain.ReadingProgressSnapshot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DocumentServiceTest {

    private lateinit var documentStore: FakeDocumentStore
    private lateinit var highlightStore: FakeHighlightStore
    private lateinit var noteStore: FakeNoteStore
    private lateinit var trackingStore: FakeTrackingStore
    private lateinit var service: DocumentService

    @BeforeEach
    fun setUp() {
        documentStore = FakeDocumentStore()
        highlightStore = FakeHighlightStore()
        noteStore = FakeNoteStore()
        trackingStore = FakeTrackingStore()
        service = DocumentService(documentStore, highlightStore, noteStore, trackingStore)
    }

    @Test
    fun `returns document detail with all fields`() {
        val docId = UUID.randomUUID()
        val document = Document(
            id = docId,
            readwiseId = "rw-123",
            url = "https://example.com/article",
            title = "Test Article",
            author = "John Doe",
            category = "article",
            location = "later",
            wordCount = 1500,
            savedAt = Instant.parse("2024-01-15T10:00:00Z"),
            firstOpenedAt = Instant.parse("2024-01-16T09:00:00Z"),
            lastOpenedAt = Instant.parse("2024-01-20T14:30:00Z"),
            imageUrl = "https://example.com/cover.jpg"
        )
        documentStore.documents[docId] = document
        trackingStore.snapshot = ReadingProgressSnapshot(
            documentId = "rw-123",
            readingProgress = 0.75,
            wordCount = 1500,
            firstOpenedAt = null,
            lastOpenedAt = null,
            recordedAt = Instant.now()
        )

        val result = service.getDocumentDetail(docId)

        assertNotNull(result)
        assertEquals(docId, result.id)
        assertEquals("rw-123", result.readwiseId)
        assertEquals("Test Article", result.title)
        assertEquals("John Doe", result.author)
        assertEquals("https://example.com/article", result.url)
        assertEquals("article", result.category)
        assertEquals("later", result.location)
        assertEquals(0.75, result.readingProgress)
        assertEquals(1500, result.wordCount)
    }

    @Test
    fun `returns null for non-existent document`() {
        val result = service.getDocumentDetail(UUID.randomUUID())
        assertNull(result)
    }

    @Test
    fun `includes highlights with notes`() {
        val docId = UUID.randomUUID()
        val document = Document(
            id = docId,
            readwiseId = "rw-456",
            url = "https://example.com/article"
        )
        documentStore.documents[docId] = document

        val highlightId = UUID.randomUUID()
        val highlight = Highlight(
            id = highlightId,
            readwiseId = "hl-1",
            document = document,
            text = "Important passage",
            highlightedAt = Instant.parse("2024-01-17T11:00:00Z")
        )
        highlightStore.highlights[docId] = listOf(highlight)

        val note = Note(
            id = UUID.randomUUID(),
            readwiseId = "note-1",
            highlight = highlight,
            content = "This is my note",
            createdAt = Instant.parse("2024-01-17T11:05:00Z")
        )
        noteStore.notesByHighlight[highlightId] = note

        val result = service.getDocumentDetail(docId)

        assertNotNull(result)
        assertEquals(1, result.highlights.size)
        assertEquals("Important passage", result.highlights[0].text)
        assertEquals("This is my note", result.highlights[0].note)
    }

    @Test
    fun `includes tags`() {
        val docId = UUID.randomUUID()
        val tag1 = Tag(id = UUID.randomUUID(), name = "tech")
        val tag2 = Tag(id = UUID.randomUUID(), name = "programming")
        val document = Document(
            id = docId,
            readwiseId = "rw-789",
            url = "https://example.com/article",
            tags = mutableSetOf(tag1, tag2)
        )
        documentStore.documents[docId] = document

        val result = service.getDocumentDetail(docId)

        assertNotNull(result)
        assertEquals(2, result.tags.size)
        assert(result.tags.contains("tech"))
        assert(result.tags.contains("programming"))
    }

    @Test
    fun `calculates stats correctly`() {
        val docId = UUID.randomUUID()
        val document = Document(
            id = docId,
            readwiseId = "rw-stats",
            url = "https://example.com/article",
            wordCount = 1500
        )
        documentStore.documents[docId] = document

        val highlight1 = Highlight(
            id = UUID.randomUUID(),
            readwiseId = "hl-1",
            document = document,
            text = "First highlight"
        )
        val highlight2 = Highlight(
            id = UUID.randomUUID(),
            readwiseId = "hl-2",
            document = document,
            text = "Second highlight"
        )
        highlightStore.highlights[docId] = listOf(highlight1, highlight2)

        noteStore.notesByHighlight[highlight1.id!!] = Note(
            id = UUID.randomUUID(),
            readwiseId = "note-1",
            highlight = highlight1,
            content = "Note for highlight 1"
        )

        val result = service.getDocumentDetail(docId)

        assertNotNull(result)
        assertEquals(2, result.highlightCount)
        assertEquals(1, result.notesCount)
        assertEquals(6, result.estimatedReadingTimeMinutes)
    }

    class FakeDocumentStore : DocumentStore {
        val documents = mutableMapOf<UUID, Document>()

        override fun save(document: Document): Document {
            val id = document.id ?: UUID.randomUUID()
            val saved = document.copy(id = id)
            documents[id] = saved
            return saved
        }

        override fun findByReadwiseId(readwiseId: String): Document? =
            documents.values.find { it.readwiseId == readwiseId }

        override fun findById(id: UUID): Document? = documents[id]

        override fun findOrCreateTags(tagNames: List<String>): MutableSet<Tag> = mutableSetOf()

        override fun findHighlightByReadwiseId(readwiseId: String): Highlight? = null

        override fun saveHighlight(highlight: Highlight): Highlight = highlight

        override fun findNoteByReadwiseId(readwiseId: String): Note? = null

        override fun saveNote(note: Note): Note = note
    }

    class FakeHighlightStore : HighlightStore {
        val highlights = mutableMapOf<UUID, List<Highlight>>()

        override fun findByDocumentId(documentId: UUID): List<Highlight> =
            highlights[documentId] ?: emptyList()
    }

    class FakeNoteStore : NoteStore {
        val notesByHighlight = mutableMapOf<UUID, Note>()

        override fun findByHighlightId(highlightId: UUID): Note? =
            notesByHighlight[highlightId]
    }

    class FakeTrackingStore : TrackingStore {
        var snapshot: ReadingProgressSnapshot? = null

        override fun findLatestSnapshot(documentId: String): ReadingProgressSnapshot? = snapshot
        override fun saveSnapshot(snapshot: ReadingProgressSnapshot): ReadingProgressSnapshot = snapshot
        override fun findLatestLocation(documentId: String): LocationChange? = null
        override fun saveLocationChange(change: LocationChange): LocationChange = change
    }
}