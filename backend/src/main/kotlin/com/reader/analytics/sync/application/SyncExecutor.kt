package com.reader.analytics.sync.application

import java.util.UUID

interface SyncExecutor {
    fun executeAsync(syncId: UUID)
}
