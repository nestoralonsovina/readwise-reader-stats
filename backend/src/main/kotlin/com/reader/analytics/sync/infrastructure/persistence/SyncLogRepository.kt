package com.reader.analytics.sync.infrastructure.persistence

import com.reader.analytics.sync.domain.SyncLog
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SyncLogRepository : JpaRepository<SyncLog, UUID>
