package com.reader.analytics.sync.infrastructure.persistence

import com.reader.analytics.sync.application.LogStore
import com.reader.analytics.sync.domain.SyncLog
import org.springframework.stereotype.Component

@Component
class JpaLogStore(
    private val repository: SyncLogRepository
) : LogStore {

    override fun save(log: SyncLog): SyncLog =
        repository.save(log)
}
