package com.reader.analytics.tracking.application

import com.reader.analytics.sync.domain.events.DocumentSyncedEvent
import com.reader.analytics.tracking.domain.LocationChange
import com.reader.analytics.tracking.domain.ReadingProgressSnapshot
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Component
class TrackingEventListener(
    private val trackingStore: TrackingStore,
    private val clock: Clock = Clock.systemUTC()
) {
    @EventListener
    @Transactional
    fun onDocumentSynced(event: DocumentSyncedEvent) {
        trackProgressChange(event)
        trackLocationChange(event)
    }

    private fun trackProgressChange(event: DocumentSyncedEvent) {
        val currentProgress = event.readingProgress ?: return

        val latestSnapshot = trackingStore.findLatestSnapshot(event.id)
        if (latestSnapshot != null && latestSnapshot.readingProgress == currentProgress) {
            return
        }

        trackingStore.saveSnapshot(
            ReadingProgressSnapshot(
                documentId = event.id,
                readingProgress = currentProgress,
                wordCount = event.wordCount,
                firstOpenedAt = event.firstOpenedAt,
                lastOpenedAt = event.lastOpenedAt,
                recordedAt = event.updatedAt ?: clock.instant()
            )
        )
    }

    private fun trackLocationChange(event: DocumentSyncedEvent) {
        val currentLocation = event.location ?: return

        val latestLocation = trackingStore.findLatestLocation(event.id)
        if (latestLocation != null && latestLocation.toLocation == currentLocation) {
            return
        }

        trackingStore.saveLocationChange(
            LocationChange(
                documentId = event.id,
                fromLocation = latestLocation?.toLocation,
                toLocation = currentLocation,
                changedAt = clock.instant(),
                category = event.category
            )
        )
    }
}
