package com.reader.analytics.tracking.infrastructure.persistence

import com.reader.analytics.tracking.application.TrackingStore
import com.reader.analytics.tracking.domain.LocationChange
import com.reader.analytics.tracking.domain.ReadingProgressSnapshot
import org.springframework.stereotype.Component

@Component
class JpaTrackingStore(
    private val snapshotRepository: SnapshotRepository,
    private val locationChangeRepository: LocationChangeRepository
) : TrackingStore {

    override fun findLatestSnapshot(documentId: String): ReadingProgressSnapshot? =
        snapshotRepository.findTopByDocumentIdOrderByRecordedAtDesc(documentId)

    override fun saveSnapshot(snapshot: ReadingProgressSnapshot): ReadingProgressSnapshot =
        snapshotRepository.save(snapshot)

    override fun findLatestLocation(documentId: String): LocationChange? =
        locationChangeRepository.findTopByDocumentIdOrderByChangedAtDesc(documentId)

    override fun saveLocationChange(change: LocationChange): LocationChange =
        locationChangeRepository.save(change)
}
