package com.reader.analytics.sync.infrastructure.persistence

import com.reader.analytics.sync.domain.SyncCursor
import org.springframework.data.jpa.repository.JpaRepository

interface SyncCursorRepository : JpaRepository<SyncCursor, String> {
    fun findByCursorType(cursorType: String): SyncCursor?
}
