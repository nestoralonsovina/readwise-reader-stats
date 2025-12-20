package com.reader.analytics.sync.application

import java.util.UUID

sealed class SyncStartResult {
    data class Started(val syncId: UUID) : SyncStartResult()
    data class AlreadyRunning(val activeSyncId: UUID) : SyncStartResult()
}
