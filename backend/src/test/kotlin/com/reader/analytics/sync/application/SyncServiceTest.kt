package com.reader.analytics.sync.application

import com.reader.analytics.sync.domain.SyncCursor
import com.reader.analytics.sync.domain.SyncLog
import com.reader.analytics.sync.domain.SyncStatus
import com.reader.analytics.sync.domain.events.DocumentSyncedEvent
import com.reader.analytics.sync.infrastructure.ReadwiseClient
import com.reader.analytics.sync.infrastructure.persistence.SyncCursorRepository
import com.reader.analytics.sync.infrastructure.persistence.SyncLogRepository
import com.reader.analytics.sync.infrastructure.readwise.dto.DocumentDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SyncServiceTest {

    private lateinit var readwiseClient: FakeReadwiseClient
    private lateinit var cursorRepository: FakeSyncCursorRepository
    private lateinit var logRepository: FakeSyncLogRepository
    private lateinit var eventPublisher: FakeEventPublisher
    private lateinit var syncService: SyncService

    @BeforeEach
    fun setUp() {
        readwiseClient = FakeReadwiseClient()
        cursorRepository = FakeSyncCursorRepository()
        logRepository = FakeSyncLogRepository()
        eventPublisher = FakeEventPublisher()
        syncService = SyncService(readwiseClient, cursorRepository, logRepository, eventPublisher)
    }

    @Test
    fun `first sync fetches all documents without updatedAfter filter`() {
        readwiseClient.documents = listOf(
            createDocumentDto("doc-1", "Article 1")
        )

        syncService.sync()

        assertEquals(null, readwiseClient.lastUpdatedAfter)
    }

    @Test
    fun `incremental sync uses updatedAfter from cursor`() {
        val lastSyncTime = Instant.parse("2024-01-15T10:00:00Z")
        cursorRepository.save(SyncCursor("documents", lastSyncTime, null))
        readwiseClient.documents = listOf(
            createDocumentDto("doc-1", "Article 1")
        )

        syncService.sync()

        assertEquals(lastSyncTime, readwiseClient.lastUpdatedAfter)
    }

    @Test
    fun `publishes DocumentSyncedEvent for each document`() {
        readwiseClient.documents = listOf(
            createDocumentDto("doc-1", "Article 1"),
            createDocumentDto("doc-2", "Article 2")
        )

        syncService.sync()

        assertEquals(2, eventPublisher.publishedEvents.size)
        val event1 = eventPublisher.publishedEvents[0] as DocumentSyncedEvent
        val event2 = eventPublisher.publishedEvents[1] as DocumentSyncedEvent
        assertEquals("doc-1", event1.id)
        assertEquals("Article 1", event1.title)
        assertEquals("doc-2", event2.id)
        assertEquals("Article 2", event2.title)
    }

    @Test
    fun `updates cursor with latest updatedAt after sync`() {
        readwiseClient.documents = listOf(
            createDocumentDto("doc-1", "Article 1", updatedAt = Instant.parse("2024-01-15T10:00:00Z")),
            createDocumentDto("doc-2", "Article 2", updatedAt = Instant.parse("2024-01-16T10:00:00Z"))
        )

        syncService.sync()

        val cursor = cursorRepository.findByCursorType("documents")
        assertNotNull(cursor)
        assertEquals(Instant.parse("2024-01-16T10:00:00Z"), cursor.lastSyncedAt)
    }

    @Test
    fun `creates sync log with completed status and counts`() {
        readwiseClient.documents = listOf(
            createDocumentDto("doc-1", "Article 1"),
            createDocumentDto("doc-2", "Article 2")
        )

        val result = syncService.sync()

        assertEquals(SyncStatus.COMPLETED, result.status)
        assertEquals(2, result.documentsProcessed)
        assertNotNull(result.completedAt)
    }

    @Test
    fun `creates sync log with failed status on error`() {
        readwiseClient.shouldThrowError = true

        val result = syncService.sync()

        assertEquals(SyncStatus.FAILED, result.status)
        assertNotNull(result.errorMessage)
        assertNotNull(result.completedAt)
    }

    @Test
    fun `returns empty sync log when no documents to process`() {
        readwiseClient.documents = emptyList()

        val result = syncService.sync()

        assertEquals(SyncStatus.COMPLETED, result.status)
        assertEquals(0, result.documentsProcessed)
    }

    private fun createDocumentDto(
        id: String,
        title: String,
        updatedAt: Instant = Instant.now()
    ) = DocumentDto(
        id = id,
        url = "https://example.com/$id",
        sourceUrl = null,
        title = title,
        author = "Test Author",
        source = "web",
        category = "article",
        location = "new",
        tags = emptyMap(),
        siteName = "Example",
        wordCount = 1000,
        readingProgress = 0.0,
        publishedDate = null,
        savedAt = Instant.now(),
        createdAt = Instant.now(),
        updatedAt = updatedAt,
        firstOpenedAt = null,
        lastOpenedAt = null,
        parentId = null,
        summary = null,
        notes = null,
        imageUrl = null
    )

    // Test doubles

    class FakeReadwiseClient : ReadwiseClient {
        var documents: List<DocumentDto> = emptyList()
        var lastUpdatedAfter: Instant? = null
        var shouldThrowError = false

        override fun fetchDocuments(updatedAfter: Instant?): Sequence<DocumentDto> {
            lastUpdatedAfter = updatedAfter
            if (shouldThrowError) {
                throw RuntimeException("API error")
            }
            return documents.asSequence()
        }
    }

    class FakeSyncCursorRepository : SyncCursorRepository {
        private val cursors = mutableMapOf<String, SyncCursor>()

        override fun findByCursorType(cursorType: String): SyncCursor? = cursors[cursorType]

        override fun <S : SyncCursor> save(entity: S): S {
            cursors[entity.cursorType] = entity
            @Suppress("UNCHECKED_CAST")
            return entity
        }

        // Unused JpaRepository methods - minimal implementation
        override fun findAll() = cursors.values.toList()
        override fun findById(id: String) = java.util.Optional.ofNullable(cursors[id])
        override fun existsById(id: String) = cursors.containsKey(id)
        override fun count() = cursors.size.toLong()
        override fun deleteById(id: String) { cursors.remove(id) }
        override fun delete(entity: SyncCursor) { cursors.remove(entity.cursorType) }
        override fun deleteAllById(ids: MutableIterable<String>) { ids.forEach { cursors.remove(it) } }
        override fun deleteAll(entities: MutableIterable<SyncCursor>) { entities.forEach { cursors.remove(it.cursorType) } }
        override fun deleteAll() { cursors.clear() }
        override fun <S : SyncCursor> saveAll(entities: MutableIterable<S>): MutableList<S> {
            entities.forEach { save(it) }
            return entities.toMutableList()
        }
        override fun findAllById(ids: MutableIterable<String>) = ids.mapNotNull { cursors[it] }
        override fun flush() {}
        override fun <S : SyncCursor> saveAndFlush(entity: S) = save(entity)
        override fun <S : SyncCursor> saveAllAndFlush(entities: MutableIterable<S>) = saveAll(entities)
        override fun deleteAllInBatch(entities: MutableIterable<SyncCursor>) { deleteAll(entities) }
        override fun deleteAllByIdInBatch(ids: MutableIterable<String>) { deleteAllById(ids) }
        override fun deleteAllInBatch() { deleteAll() }
        override fun getReferenceById(id: String) = cursors[id]!!
        override fun getById(id: String) = cursors[id]!!
        override fun getOne(id: String) = cursors[id]!!
        override fun findAll(sort: org.springframework.data.domain.Sort) = findAll()
        override fun findAll(pageable: org.springframework.data.domain.Pageable) = org.springframework.data.domain.PageImpl(findAll())
        override fun findAll(example: org.springframework.data.domain.Example<SyncCursor>) = findAll()
        override fun findAll(example: org.springframework.data.domain.Example<SyncCursor>, sort: org.springframework.data.domain.Sort) = findAll()
        override fun <S : SyncCursor> findAll(example: org.springframework.data.domain.Example<S>, pageable: org.springframework.data.domain.Pageable) = org.springframework.data.domain.PageImpl(findAll()) as org.springframework.data.domain.Page<S>
        override fun <S : SyncCursor> findOne(example: org.springframework.data.domain.Example<S>) = java.util.Optional.empty<S>()
        override fun <S : SyncCursor> exists(example: org.springframework.data.domain.Example<S>) = false
        override fun <S : SyncCursor> count(example: org.springframework.data.domain.Example<S>) = 0L
        override fun <S : SyncCursor, R : Any?> findBy(example: org.springframework.data.domain.Example<S>, queryFunction: java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R>): R {
            throw UnsupportedOperationException()
        }
    }

    class FakeSyncLogRepository : SyncLogRepository {
        private val logs = mutableMapOf<UUID, SyncLog>()

        override fun <S : SyncLog> save(entity: S): S {
            val id = entity.id ?: UUID.randomUUID()
            val saved = entity.copy(id = id) as S
            logs[id] = saved
            return saved
        }

        override fun findById(id: UUID) = java.util.Optional.ofNullable(logs[id])

        // Unused JpaRepository methods - minimal implementation
        override fun findAll() = logs.values.toList()
        override fun existsById(id: UUID) = logs.containsKey(id)
        override fun count() = logs.size.toLong()
        override fun deleteById(id: UUID) { logs.remove(id) }
        override fun delete(entity: SyncLog) { entity.id?.let { logs.remove(it) } }
        override fun deleteAllById(ids: MutableIterable<UUID>) { ids.forEach { logs.remove(it) } }
        override fun deleteAll(entities: MutableIterable<SyncLog>) { entities.forEach { it.id?.let { id -> logs.remove(id) } } }
        override fun deleteAll() { logs.clear() }
        override fun <S : SyncLog> saveAll(entities: MutableIterable<S>): MutableList<S> {
            entities.forEach { save(it) }
            return entities.toMutableList()
        }
        override fun findAllById(ids: MutableIterable<UUID>) = ids.mapNotNull { logs[it] }
        override fun flush() {}
        override fun <S : SyncLog> saveAndFlush(entity: S) = save(entity)
        override fun <S : SyncLog> saveAllAndFlush(entities: MutableIterable<S>) = saveAll(entities)
        override fun deleteAllInBatch(entities: MutableIterable<SyncLog>) { deleteAll(entities) }
        override fun deleteAllByIdInBatch(ids: MutableIterable<UUID>) { deleteAllById(ids) }
        override fun deleteAllInBatch() { deleteAll() }
        override fun getReferenceById(id: UUID) = logs[id]!!
        override fun getById(id: UUID) = logs[id]!!
        override fun getOne(id: UUID) = logs[id]!!
        override fun findAll(sort: org.springframework.data.domain.Sort) = findAll()
        override fun findAll(pageable: org.springframework.data.domain.Pageable) = org.springframework.data.domain.PageImpl(findAll())
        override fun findAll(example: org.springframework.data.domain.Example<SyncLog>) = findAll()
        override fun findAll(example: org.springframework.data.domain.Example<SyncLog>, sort: org.springframework.data.domain.Sort) = findAll()
        override fun <S : SyncLog> findAll(example: org.springframework.data.domain.Example<S>, pageable: org.springframework.data.domain.Pageable) = org.springframework.data.domain.PageImpl(findAll()) as org.springframework.data.domain.Page<S>
        override fun <S : SyncLog> findOne(example: org.springframework.data.domain.Example<S>) = java.util.Optional.empty<S>()
        override fun <S : SyncLog> exists(example: org.springframework.data.domain.Example<S>) = false
        override fun <S : SyncLog> count(example: org.springframework.data.domain.Example<S>) = 0L
        override fun <S : SyncLog, R : Any?> findBy(example: org.springframework.data.domain.Example<S>, queryFunction: java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R>): R {
            throw UnsupportedOperationException()
        }
    }

    class FakeEventPublisher : ApplicationEventPublisher {
        val publishedEvents = mutableListOf<Any>()

        override fun publishEvent(event: Any) {
            publishedEvents.add(event)
        }
    }
}
