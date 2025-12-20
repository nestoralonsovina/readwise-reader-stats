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
    fun `saves and retrieves highlight with document reference`() {
        val document = documentRepository.save(Document(
            readwiseId = "rw-doc-1",
            url = "https://example.com/article"
        ))

        val highlight = Highlight(
            readwiseId = "rw-hl-123",
            document = document,
            text = "This is highlighted text",
            highlightedAt = Instant.parse("2024-01-15T10:00:00Z")
        )

        val saved = highlightRepository.save(highlight)
        val retrieved = highlightRepository.findById(saved.id!!).orElse(null)

        assertNotNull(retrieved)
        assertEquals("rw-hl-123", retrieved.readwiseId)
        assertEquals("This is highlighted text", retrieved.text)
        assertEquals(document.id, retrieved.document.id)
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
        assertEquals(document.id, found.document.id)
    }

    @Test
    fun `navigates from highlight to document`() {
        val document = documentRepository.save(Document(
            readwiseId = "rw-doc-nav",
            url = "https://example.com/nav-test",
            title = "Navigation Test Article"
        ))

        highlightRepository.save(Highlight(
            readwiseId = "rw-hl-nav",
            document = document,
            text = "Navigable highlight"
        ))

        val found = highlightRepository.findByReadwiseId("rw-hl-nav")

        assertNotNull(found)
        assertEquals("Navigation Test Article", found.document.title)
        assertEquals("rw-doc-nav", found.document.readwiseId)
    }
}
