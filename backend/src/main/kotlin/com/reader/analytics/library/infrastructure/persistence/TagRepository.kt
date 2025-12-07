package com.reader.analytics.library.infrastructure.persistence

import com.reader.analytics.library.domain.Tag
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TagRepository : JpaRepository<Tag, UUID> {
    fun findByName(name: String): Tag?
    fun findByNameIn(names: Collection<String>): List<Tag>
}
