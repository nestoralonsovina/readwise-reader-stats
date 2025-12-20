package com.reader.analytics.library.infrastructure.persistence

import com.reader.analytics.library.domain.Document
import com.reader.analytics.library.domain.Highlight
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DataJpaTest
class HighlightRepositoryTest {

    @Autowired
    private lateinit var highlightRepository: HighlightRepository

    @Autowired
    private lateinit var documentRepository: DocumentRepository

    @Test
    fun `saves and retrieves highlight by id`() {
        val document = documentRepository.save(Document(
            readwiseId = "rw-doc-1",
            url = "https://example.com/article"
        ))

        val highlight = Highlight(
            readwiseId = "rw-hl-123",
            documentReadwiseId = document.readwiseId,
            text = "This is highlighted text",
            highlightedAt = Instant.parse("2024-01-15T10:00:00Z")
        )

        val saved = highlightRepository.save(highlight)
        val retrieved = highlightRepository.findById(saved.id!!).orElse(null)

        assertNotNull(retrieved)
        assertEquals("rw-hl-123", retrieved.readwiseId)
        assertEquals("This is highlighted text", retrieved.text)
        assertEquals(document.readwiseId, retrieved.documentReadwiseId)
    }

    @Test
    fun `finds highlight by readwise id`() {
        val document = documentRepository.save(Document(
            readwiseId = "rw-doc-2",
            url = "https://example.com/article2"
        ))

        highlightRepository.save(Highlight(
            readwiseId = "rw-hl-456",
            documentReadwiseId = document.readwiseId,
            text = "Another highlight"
        ))

        val found = highlightRepository.findByReadwiseId("rw-hl-456")

        assertNotNull(found)
        assertEquals("Another highlight", found.text)
        assertEquals(document.readwiseId, found.documentReadwiseId)
    }

    @Test
    fun `can save highlight without existing document`() {
        // Highlights can be saved independently of documents
        // (they reference by readwiseId, not FK)
        val highlight = Highlight(
            readwiseId = "rw-hl-orphan",
            documentReadwiseId = "rw-doc-not-yet-synced",
            text = "Orphan highlight text"
        )

        val saved = highlightRepository.save(highlight)
        val retrieved = highlightRepository.findByReadwiseId("rw-hl-orphan")

        assertNotNull(retrieved)
        assertEquals("Orphan highlight text", retrieved.text)
        assertEquals("rw-doc-not-yet-synced", retrieved.documentReadwiseId)
    }
}
