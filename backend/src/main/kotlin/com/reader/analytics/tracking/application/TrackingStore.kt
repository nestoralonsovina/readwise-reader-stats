package com.reader.analytics.tracking.application

import com.reader.analytics.tracking.domain.LocationChange
import com.reader.analytics.tracking.domain.ReadingProgressSnapshot

interface TrackingStore {
    fun findLatestSnapshot(documentId: String): ReadingProgressSnapshot?
    fun saveSnapshot(snapshot: ReadingProgressSnapshot): ReadingProgressSnapshot

    fun findLatestLocation(documentId: String): LocationChange?
    fun saveLocationChange(change: LocationChange): LocationChange
}
