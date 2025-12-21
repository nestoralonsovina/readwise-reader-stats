package com.reader.analytics.tracking.application

import com.reader.analytics.sync.domain.events.DocumentSyncedEvent
import com.reader.analytics.tracking.domain.LocationChange
import com.reader.analytics.tracking.domain.ReadingProgressSnapshot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TrackingEventListenerTest {

    private lateinit var trackingStore: FakeTrackingStore
    private lateinit var listener: TrackingEventListener
    private val fixedClock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneId.of("UTC"))

    @BeforeEach
    fun setUp() {
        trackingStore = FakeTrackingStore()
        listener = TrackingEventListener(trackingStore, fixedClock)
    }

    @Test
    fun `creates snapshot for new document with reading progress`() {
        val updatedAt = Instant.parse("2024-01-15T08:30:00Z")
        val event = createDocumentSyncedEvent(
            id = "doc-1",
            readingProgress = 0.25,
            wordCount = 1000,
            updatedAt = updatedAt
        )

        listener.onDocumentSynced(event)

        val snapshot = trackingStore.findLatestSnapshot("doc-1")
        assertNotNull(snapshot)
        assertEquals("doc-1", snapshot.documentId)
        assertEquals(0.25, snapshot.readingProgress)
        assertEquals(1000, snapshot.wordCount)
        assertEquals(updatedAt, snapshot.recordedAt)
    }

    @Test
    fun `uses clock when updatedAt is null`() {
        val event = createDocumentSyncedEvent(
            id = "doc-1",
            readingProgress = 0.25,
            wordCount = 1000,
            updatedAt = null
        )

        listener.onDocumentSynced(event)

        val snapshot = trackingStore.findLatestSnapshot("doc-1")
        assertNotNull(snapshot)
        assertEquals(fixedClock.instant(), snapshot.recordedAt)
    }

    @Test
    fun `does not create snapshot when progress unchanged`() {
        // Initial snapshot already exists
        trackingStore.saveSnapshot(
            ReadingProgressSnapshot(
                documentId = "doc-1",
                readingProgress = 0.5,
                wordCount = 1000,
                firstOpenedAt = null,
                lastOpenedAt = null,
                recordedAt = Instant.parse("2024-01-14T10:00:00Z")
            )
        )

        // Event with same progress
        val event = createDocumentSyncedEvent(
            id = "doc-1",
            readingProgress = 0.5,
            wordCount = 1000
        )

        listener.onDocumentSynced(event)

        // Should still have only the original snapshot
        val snapshots = trackingStore.getAllSnapshots("doc-1")
        assertEquals(1, snapshots.size)
        assertEquals(Instant.parse("2024-01-14T10:00:00Z"), snapshots[0].recordedAt)
    }

    @Test
    fun `creates snapshot when progress changes`() {
        // Initial snapshot
        trackingStore.saveSnapshot(
            ReadingProgressSnapshot(
                documentId = "doc-1",
                readingProgress = 0.25,
                wordCount = 1000,
                firstOpenedAt = null,
                lastOpenedAt = null,
                recordedAt = Instant.parse("2024-01-14T10:00:00Z")
            )
        )

        // Event with changed progress
        val updatedAt = Instant.parse("2024-01-15T14:00:00Z")
        val event = createDocumentSyncedEvent(
            id = "doc-1",
            readingProgress = 0.75,
            wordCount = 1000,
            updatedAt = updatedAt
        )

        listener.onDocumentSynced(event)

        // Should have two snapshots
        val snapshots = trackingStore.getAllSnapshots("doc-1")
        assertEquals(2, snapshots.size)

        val latest = trackingStore.findLatestSnapshot("doc-1")
        assertNotNull(latest)
        assertEquals(0.75, latest.readingProgress)
        assertEquals(updatedAt, latest.recordedAt)
    }

    @Test
    fun `creates location change for new document`() {
        val event = createDocumentSyncedEvent(
            id = "doc-1",
            location = "new",
            category = "article"
        )

        listener.onDocumentSynced(event)

        val locationChange = trackingStore.findLatestLocation("doc-1")
        assertNotNull(locationChange)
        assertEquals("doc-1", locationChange.documentId)
        assertEquals(null, locationChange.fromLocation)
        assertEquals("new", locationChange.toLocation)
        assertEquals("article", locationChange.category)
        assertEquals(fixedClock.instant(), locationChange.changedAt)
    }

    @Test
    fun `does not create location change when location unchanged`() {
        trackingStore.saveLocationChange(
            LocationChange(
                documentId = "doc-1",
                fromLocation = null,
                toLocation = "new",
                changedAt = Instant.parse("2024-01-14T10:00:00Z"),
                category = "article"
            )
        )

        val event = createDocumentSyncedEvent(
            id = "doc-1",
            location = "new",
            category = "article"
        )

        listener.onDocumentSynced(event)

        val changes = trackingStore.getAllLocationChanges("doc-1")
        assertEquals(1, changes.size)
    }

    @Test
    fun `creates location change when location changes`() {
        trackingStore.saveLocationChange(
            LocationChange(
                documentId = "doc-1",
                fromLocation = null,
                toLocation = "new",
                changedAt = Instant.parse("2024-01-14T10:00:00Z"),
                category = "article"
            )
        )

        val event = createDocumentSyncedEvent(
            id = "doc-1",
            location = "archive",
            category = "article"
        )

        listener.onDocumentSynced(event)

        val changes = trackingStore.getAllLocationChanges("doc-1")
        assertEquals(2, changes.size)

        val latest = trackingStore.findLatestLocation("doc-1")
        assertNotNull(latest)
        assertEquals("new", latest.fromLocation)
        assertEquals("archive", latest.toLocation)
    }

    private fun createDocumentSyncedEvent(
        id: String,
        readingProgress: Double? = null,
        wordCount: Int? = null,
        location: String? = "new",
        category: String? = "article",
        updatedAt: Instant? = Instant.now()
    ) = DocumentSyncedEvent(
        id = id,
        url = "https://example.com/$id",
        title = "Test Article",
        author = null,
        category = category,
        location = location,
        readingProgress = readingProgress,
        wordCount = wordCount,
        savedAt = Instant.now(),
        updatedAt = updatedAt,
        firstOpenedAt = null,
        lastOpenedAt = null,
        tags = emptyList(),
        parentId = null,
        imageUrl = null
    )

    class FakeTrackingStore : TrackingStore {
        private val snapshotsByDocument = mutableMapOf<String, MutableList<ReadingProgressSnapshot>>()
        private val locationChangesByDocument = mutableMapOf<String, MutableList<LocationChange>>()

        override fun findLatestSnapshot(documentId: String): ReadingProgressSnapshot? =
            snapshotsByDocument[documentId]?.maxByOrNull { it.recordedAt }

        override fun saveSnapshot(snapshot: ReadingProgressSnapshot): ReadingProgressSnapshot {
            val id = snapshot.id ?: UUID.randomUUID()
            val saved = snapshot.copy(id = id)
            snapshotsByDocument.getOrPut(snapshot.documentId) { mutableListOf() }.add(saved)
            return saved
        }

        fun getAllSnapshots(documentId: String): List<ReadingProgressSnapshot> =
            snapshotsByDocument[documentId] ?: emptyList()

        override fun findLatestLocation(documentId: String): LocationChange? =
            locationChangesByDocument[documentId]?.maxByOrNull { it.changedAt }

        override fun saveLocationChange(change: LocationChange): LocationChange {
            val id = change.id ?: UUID.randomUUID()
            val saved = change.copy(id = id)
            locationChangesByDocument.getOrPut(change.documentId) { mutableListOf() }.add(saved)
            return saved
        }

        fun getAllLocationChanges(documentId: String): List<LocationChange> =
            locationChangesByDocument[documentId] ?: emptyList()
    }
}
