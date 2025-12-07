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
            document = document,
            text = "This is highlighted text",
            note = "My note",
            color = "yellow",
            locationIndex = 100,
            highlightedAt = Instant.parse("2024-01-15T10:00:00Z")
        )

        val saved = highlightRepository.save(highlight)
        val retrieved = highlightRepository.findById(saved.id!!).orElse(null)

        assertNotNull(retrieved)
        assertEquals("rw-hl-123", retrieved.readwiseId)
        assertEquals("This is highlighted text", retrieved.text)
        assertEquals("My note", retrieved.note)
        assertEquals("yellow", retrieved.color)
        assertEquals(100, retrieved.locationIndex)
    }

    @Test
    fun `finds highlight by readwise id`() {
        val document = documentRepository.save(Document(
            readwiseId = "rw-doc-2",
            url = "https://example.com/article2"
        ))

        highlightRepository.save(Highlight(
            readwiseId = "rw-hl-456",
            document = document,
            text = "Another highlight"
        ))

        val found = highlightRepository.findByReadwiseId("rw-hl-456")

        assertNotNull(found)
        assertEquals("Another highlight", found.text)
    }

    @Test
    fun `finds all highlights by document`() {
        val document = documentRepository.save(Document(
            readwiseId = "rw-doc-3",
            url = "https://example.com/article3"
        ))

        highlightRepository.save(Highlight(
            readwiseId = "rw-hl-1",
            document = document,
            text = "First highlight"
        ))
        highlightRepository.save(Highlight(
            readwiseId = "rw-hl-2",
            document = document,
            text = "Second highlight"
        ))

        val highlights = highlightRepository.findByDocument(document)

        assertEquals(2, highlights.size)
    }
}
