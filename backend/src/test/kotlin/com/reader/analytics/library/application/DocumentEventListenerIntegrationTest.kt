package com.reader.analytics.library.application

import com.reader.analytics.library.infrastructure.persistence.DocumentRepository
import com.reader.analytics.library.infrastructure.persistence.JpaDocumentStore
import com.reader.analytics.library.infrastructure.persistence.TagRepository
import com.reader.analytics.sync.domain.events.DocumentSyncedEvent
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DataJpaTest
@Import(JpaDocumentStore::class, DocumentEventListener::class)
class DocumentEventListenerIntegrationTest {

    @Autowired
    private lateinit var listener: DocumentEventListener

    @Autowired
    private lateinit var documentRepository: DocumentRepository

    @Autowired
    private lateinit var tagRepository: TagRepository

    @Test
    fun `persists document and tags from sync event`() {
        val event = DocumentSyncedEvent(
            id = "rw-integration-test",
            url = "https://example.com/integration",
            title = "Integration Test Article",
            author = "Test Author",
            category = "article",
            location = "new",
            readingProgress = 0.5,
            wordCount = 500,
            savedAt = Instant.parse("2024-01-10T10:00:00Z"),
            updatedAt = Instant.parse("2024-01-15T10:00:00Z"),
            firstOpenedAt = null,
            lastOpenedAt = null,
            tags = listOf("test", "integration"),
            parentId = null,
            imageUrl = null,
            highlights = emptyList()
        )

        listener.onDocumentSynced(event)

        val document = documentRepository.findByReadwiseId("rw-integration-test")
        assertNotNull(document)
        assertEquals("Integration Test Article", document.title)
        assertEquals("Test Author", document.author)
        assertEquals("article", document.category)
        assertEquals("new", document.location)
        assertEquals(0.5, document.readingProgress)
        assertEquals(500, document.wordCount)
        assertEquals(2, document.tags.size)

        assertNotNull(tagRepository.findByName("test"))
        assertNotNull(tagRepository.findByName("integration"))
    }

    @Test
    fun `updates existing document on second sync`() {
        val event1 = DocumentSyncedEvent(
            id = "rw-update-test",
            url = "https://example.com/update",
            title = "Original Title",
            author = null,
            category = null,
            location = "new",
            readingProgress = 0.0,
            wordCount = null,
            savedAt = Instant.now(),
            updatedAt = Instant.now(),
            firstOpenedAt = null,
            lastOpenedAt = null,
            tags = emptyList(),
            parentId = null,
            imageUrl = null,
            highlights = emptyList()
        )
        listener.onDocumentSynced(event1)

        val event2 = DocumentSyncedEvent(
            id = "rw-update-test",
            url = "https://example.com/update",
            title = "Updated Title",
            author = "New Author",
            category = "article",
            location = "archive",
            readingProgress = 1.0,
            wordCount = 1000,
            savedAt = Instant.now(),
            updatedAt = Instant.now(),
            firstOpenedAt = null,
            lastOpenedAt = null,
            tags = listOf("updated"),
            parentId = null,
            imageUrl = null,
            highlights = emptyList()
        )
        listener.onDocumentSynced(event2)

        val documents = documentRepository.findAll()
        assertEquals(1, documents.size)

        val document = documentRepository.findByReadwiseId("rw-update-test")
        assertNotNull(document)
        assertEquals("Updated Title", document.title)
        assertEquals("New Author", document.author)
        assertEquals("archive", document.location)
        assertEquals(1.0, document.readingProgress)
        assertEquals(1, document.tags.size)
    }

    @Test
    fun `reuses existing tags across documents`() {
        val event1 = DocumentSyncedEvent(
            id = "rw-shared-tag-1",
            url = "https://example.com/shared1",
            title = "Document 1",
            author = null,
            category = null,
            location = null,
            readingProgress = null,
            wordCount = null,
            savedAt = null,
            updatedAt = null,
            firstOpenedAt = null,
            lastOpenedAt = null,
            tags = listOf("shared-tag"),
            parentId = null,
            imageUrl = null,
            highlights = emptyList()
        )
        listener.onDocumentSynced(event1)

        val event2 = DocumentSyncedEvent(
            id = "rw-shared-tag-2",
            url = "https://example.com/shared2",
            title = "Document 2",
            author = null,
            category = null,
            location = null,
            readingProgress = null,
            wordCount = null,
            savedAt = null,
            updatedAt = null,
            firstOpenedAt = null,
            lastOpenedAt = null,
            tags = listOf("shared-tag"),
            parentId = null,
            imageUrl = null,
            highlights = emptyList()
        )
        listener.onDocumentSynced(event2)

        val allTags = tagRepository.findAll()
        assertEquals(1, allTags.size)
        assertEquals("shared-tag", allTags.first().name)

        val doc1 = documentRepository.findByReadwiseId("rw-shared-tag-1")
        val doc2 = documentRepository.findByReadwiseId("rw-shared-tag-2")
        assertEquals(doc1?.tags?.first()?.id, doc2?.tags?.first()?.id)
    }
}
