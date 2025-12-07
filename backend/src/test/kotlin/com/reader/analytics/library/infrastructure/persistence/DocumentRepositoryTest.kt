package com.reader.analytics.library.infrastructure.persistence

import com.reader.analytics.library.domain.Document
import com.reader.analytics.library.domain.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DataJpaTest
class DocumentRepositoryTest {

    @Autowired
    private lateinit var repository: DocumentRepository

    @Autowired
    private lateinit var tagRepository: TagRepository

    @Test
    fun `saves and retrieves document by id`() {
        val document = Document(
            readwiseId = "rw-123",
            url = "https://example.com/article",
            title = "Test Article",
            author = "Test Author",
            category = "article",
            location = "new",
            readingProgress = 0.5,
            wordCount = 1000,
            savedAt = Instant.parse("2024-01-15T10:00:00Z"),
            updatedAt = Instant.parse("2024-01-16T10:00:00Z"),
            parentId = null
        )

        val saved = repository.save(document)
        val retrieved = repository.findById(saved.id!!).orElse(null)

        assertNotNull(retrieved)
        assertEquals("rw-123", retrieved.readwiseId)
        assertEquals("Test Article", retrieved.title)
    }

    @Test
    fun `finds document by readwise id`() {
        val document = Document(
            readwiseId = "rw-456",
            url = "https://example.com/article2",
            title = "Another Article",
            author = null,
            category = null,
            location = null,
            readingProgress = null,
            wordCount = null,
            savedAt = null,
            updatedAt = null,
            parentId = null
        )
        repository.save(document)

        val found = repository.findByReadwiseId("rw-456")

        assertNotNull(found)
        assertEquals("Another Article", found.title)
    }

    @Test
    fun `returns null when document not found by readwise id`() {
        val found = repository.findByReadwiseId("non-existent")

        assertNull(found)
    }

    @Test
    fun `saves document with tags`() {
        val tag1 = tagRepository.save(Tag(name = "kotlin"))
        val tag2 = tagRepository.save(Tag(name = "spring"))

        val document = Document(
            readwiseId = "rw-789",
            url = "https://example.com/article3",
            title = "Tagged Article",
            tags = mutableSetOf(tag1, tag2)
        )
        val saved = repository.save(document)

        val retrieved = repository.findById(saved.id!!).orElse(null)

        assertEquals(2, retrieved?.tags?.size)
        assertTrue(retrieved?.tags?.any { it.name == "kotlin" } == true)
        assertTrue(retrieved?.tags?.any { it.name == "spring" } == true)
    }

    @Test
    fun `tags are shared across documents`() {
        val sharedTag = tagRepository.save(Tag(name = "shared"))

        val doc1 = repository.save(Document(
            readwiseId = "rw-shared-1",
            url = "https://example.com/shared1",
            tags = mutableSetOf(sharedTag)
        ))
        val doc2 = repository.save(Document(
            readwiseId = "rw-shared-2",
            url = "https://example.com/shared2",
            tags = mutableSetOf(sharedTag)
        ))

        val retrieved1 = repository.findById(doc1.id!!).orElse(null)
        val retrieved2 = repository.findById(doc2.id!!).orElse(null)

        assertEquals(sharedTag.id, retrieved1?.tags?.first()?.id)
        assertEquals(sharedTag.id, retrieved2?.tags?.first()?.id)
    }
}
