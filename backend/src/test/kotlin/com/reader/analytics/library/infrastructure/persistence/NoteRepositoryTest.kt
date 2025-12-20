package com.reader.analytics.library.infrastructure.persistence

import com.reader.analytics.library.domain.Document
import com.reader.analytics.library.domain.Highlight
import com.reader.analytics.library.domain.Note
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

@DataJpaTest
class NoteRepositoryTest {

    @Autowired
    private lateinit var noteRepository: NoteRepository

    @Autowired
    private lateinit var highlightRepository: HighlightRepository

    @Autowired
    private lateinit var documentRepository: DocumentRepository

    @Test
    fun `saves note with highlight parent`() {
        val document = documentRepository.save(Document(
            readwiseId = "rw-doc-1",
            url = "https://example.com/article"
        ))
        val highlight = highlightRepository.save(Highlight(
            readwiseId = "rw-hl-456",
            document = document,
            text = "Highlighted text"
        ))

        val note = Note(
            readwiseId = "rw-note-123",
            highlight = highlight,
            content = "This is my annotation",
            createdAt = Instant.parse("2024-01-15T10:00:00Z")
        )

        val saved = noteRepository.save(note)
        val retrieved = noteRepository.findById(saved.id!!).orElse(null)

        assertNotNull(retrieved)
        assertEquals("rw-note-123", retrieved.readwiseId)
        assertNotNull(retrieved.highlight)
        assertNull(retrieved.document)
        assertEquals("This is my annotation", retrieved.content)
    }

    @Test
    fun `saves note with document parent`() {
        val document = documentRepository.save(Document(
            readwiseId = "rw-doc-2",
            url = "https://example.com/article2"
        ))

        val note = Note(
            readwiseId = "rw-note-doc",
            document = document,
            content = "Note directly on document"
        )

        val saved = noteRepository.save(note)
        val retrieved = noteRepository.findById(saved.id!!).orElse(null)

        assertNotNull(retrieved)
        assertNotNull(retrieved.document)
        assertNull(retrieved.highlight)
        assertEquals("Note directly on document", retrieved.content)
    }

    @Test
    fun `finds note by readwise id`() {
        val document = documentRepository.save(Document(
            readwiseId = "rw-doc-3",
            url = "https://example.com/article3"
        ))

        noteRepository.save(Note(
            readwiseId = "rw-note-789",
            document = document,
            content = "Note on document"
        ))

        val found = noteRepository.findByReadwiseId("rw-note-789")

        assertNotNull(found)
        assertEquals("Note on document", found.content)
        assertEquals(document.id, found.document?.id)
    }

    @Test
    fun `returns null when note not found`() {
        val found = noteRepository.findByReadwiseId("nonexistent")

        assertNull(found)
    }

    @Test
    fun `enforces unique constraint on readwiseId`() {
        val document = documentRepository.save(Document(
            readwiseId = "rw-doc-dup",
            url = "https://example.com/dup"
        ))

        noteRepository.save(Note(
            readwiseId = "rw-note-duplicate",
            document = document,
            content = "First note"
        ))

        assertFailsWith<DataIntegrityViolationException> {
            noteRepository.saveAndFlush(Note(
                readwiseId = "rw-note-duplicate",
                document = document,
                content = "Second note with same readwiseId"
            ))
        }
    }

    @Test
    fun `navigates from note to highlight to document`() {
        val document = documentRepository.save(Document(
            readwiseId = "rw-doc-nav",
            url = "https://example.com/nav",
            title = "Navigation Test"
        ))
        val highlight = highlightRepository.save(Highlight(
            readwiseId = "rw-hl-nav",
            document = document,
            text = "Navigable highlight"
        ))

        noteRepository.save(Note(
            readwiseId = "rw-note-nav",
            highlight = highlight,
            content = "Navigable note"
        ))

        val found = noteRepository.findByReadwiseId("rw-note-nav")

        assertNotNull(found)
        assertEquals("Navigable highlight", found.highlight?.text)
        assertEquals("Navigation Test", found.highlight?.document?.title)
    }
}
