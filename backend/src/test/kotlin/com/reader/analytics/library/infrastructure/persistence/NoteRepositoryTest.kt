package com.reader.analytics.library.infrastructure.persistence

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

    @Test
    fun `saves and retrieves note by id`() {
        val note = Note(
            readwiseId = "rw-note-123",
            parentId = "rw-hl-456",
            content = "This is my annotation",
            createdAt = Instant.parse("2024-01-15T10:00:00Z")
        )

        val saved = noteRepository.save(note)
        val retrieved = noteRepository.findById(saved.id!!).orElse(null)

        assertNotNull(retrieved)
        assertEquals("rw-note-123", retrieved.readwiseId)
        assertEquals("rw-hl-456", retrieved.parentId)
        assertEquals("This is my annotation", retrieved.content)
        assertEquals(Instant.parse("2024-01-15T10:00:00Z"), retrieved.createdAt)
    }

    @Test
    fun `finds note by readwise id`() {
        noteRepository.save(Note(
            readwiseId = "rw-note-789",
            parentId = "rw-doc-123",
            content = "Note on document"
        ))

        val found = noteRepository.findByReadwiseId("rw-note-789")

        assertNotNull(found)
        assertEquals("Note on document", found.content)
        assertEquals("rw-doc-123", found.parentId)
    }

    @Test
    fun `returns null when note not found`() {
        val found = noteRepository.findByReadwiseId("nonexistent")

        assertNull(found)
    }

    @Test
    fun `enforces unique constraint on readwiseId`() {
        noteRepository.save(Note(
            readwiseId = "rw-note-duplicate",
            parentId = "rw-hl-1",
            content = "First note"
        ))

        assertFailsWith<DataIntegrityViolationException> {
            noteRepository.saveAndFlush(Note(
                readwiseId = "rw-note-duplicate",
                parentId = "rw-hl-2",
                content = "Second note with same readwiseId"
            ))
        }
    }

    @Test
    fun `can save note without existing parent`() {
        val note = Note(
            readwiseId = "rw-note-orphan",
            parentId = "rw-parent-not-synced",
            content = "Orphan note"
        )

        val saved = noteRepository.save(note)
        val retrieved = noteRepository.findByReadwiseId("rw-note-orphan")

        assertNotNull(retrieved)
        assertEquals("Orphan note", retrieved.content)
        assertEquals("rw-parent-not-synced", retrieved.parentId)
    }
}
