package com.reader.analytics.sync.infrastructure.persistence

import com.reader.analytics.sync.application.SyncRunStore
import com.reader.analytics.sync.domain.SyncRun
import com.reader.analytics.sync.domain.SyncRunStatus
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JpaSyncRunStore(
    private val repository: SyncRunRepository
) : SyncRunStore {

    override fun save(run: SyncRun): SyncRun =
        repository.save(run)

    override fun findById(id: UUID): SyncRun? =
        repository.findByIdOrNull(id)

    override fun findByStatus(status: SyncRunStatus): SyncRun? =
        repository.findFirstByStatus(status)

    override fun findAllOrderByStartedAtDesc(limit: Int, offset: Int): List<SyncRun> =
        repository.findAllByOrderByStartedAtDesc(PageRequest.of(offset / limit, limit))

    override fun countAll(): Long =
        repository.count()

    override fun countByStatus(status: SyncRunStatus): Long =
        repository.countByStatus(status)
}
