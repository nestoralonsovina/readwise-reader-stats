package com.reader.analytics.library.infrastructure.persistence

import com.reader.analytics.library.domain.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DataJpaTest
class TagRepositoryTest {

    @Autowired
    private lateinit var repository: TagRepository

    @Test
    fun `saves and retrieves tag by name`() {
        val tag = Tag(name = "programming")
        repository.save(tag)

        val found = repository.findByName("programming")

        assertNotNull(found)
        assertEquals("programming", found.name)
    }

    @Test
    fun `finds all tags by names`() {
        repository.saveAll(listOf(
            Tag(name = "kotlin"),
            Tag(name = "spring"),
            Tag(name = "jpa")
        ))

        val found = repository.findByNameIn(listOf("kotlin", "spring"))

        assertEquals(2, found.size)
    }

    @Test
    fun `returns empty list when no tags match`() {
        val found = repository.findByNameIn(listOf("nonexistent"))

        assertEquals(0, found.size)
    }
}
