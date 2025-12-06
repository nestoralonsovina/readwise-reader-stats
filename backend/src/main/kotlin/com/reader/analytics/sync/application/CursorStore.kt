package com.reader.analytics.sync.application

import com.reader.analytics.sync.domain.SyncCursor

interface CursorStore {
    fun findByCursorType(cursorType: String): SyncCursor?
    fun save(cursor: SyncCursor): SyncCursor
}
