package com.reader.analytics.sync.infrastructure.persistence

import com.reader.analytics.sync.application.CursorStore
import com.reader.analytics.sync.domain.SyncCursor
import org.springframework.stereotype.Component

@Component
class JpaCursorStore(
    private val repository: SyncCursorRepository
) : CursorStore {

    override fun findByCursorType(cursorType: String): SyncCursor? =
        repository.findByCursorType(cursorType)

    override fun save(cursor: SyncCursor): SyncCursor =
        repository.save(cursor)
}
