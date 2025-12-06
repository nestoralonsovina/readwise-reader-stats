package com.reader.analytics.sync.application

import com.reader.analytics.sync.domain.SyncLog

interface LogStore {
    fun save(log: SyncLog): SyncLog
}
