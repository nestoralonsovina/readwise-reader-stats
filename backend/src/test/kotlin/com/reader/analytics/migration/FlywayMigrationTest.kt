package com.reader.analytics.migration

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var flyway: Flyway

    @Test
    fun `all flyway migrations apply cleanly`() {
        val applied = flyway.info().applied()
        assertTrue(applied.isNotEmpty(), "At least one migration should have been applied")
    }

    @Test
    fun `all expected tables exist after migration`() {
        val tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'PUBLIC'",
            String::class.java
        )

        val expectedTables = listOf(
            "documents", "highlights", "notes", "tags", "document_tags",
            "reading_progress_snapshots", "location_changes",
            "sync_runs", "sync_cursors"
        )

        for (table in expectedTables) {
            assertTrue(tables.contains(table.uppercase()) || tables.contains(table.lowercase()),
                "Table $table should exist after migration. Found: $tables")
        }
    }

    @Test
    fun `sync_logs table does not exist after V3 migration`() {
        val tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'PUBLIC'",
            String::class.java
        )

        assertFalse(
            tables.any { it.equals("sync_logs", ignoreCase = true) },
            "sync_logs table should not exist after V3 migration"
        )
    }

    @Test
    fun `analytics indexes exist after V2 migration`() {
        val indexes = jdbcTemplate.queryForList(
            "SELECT index_name FROM information_schema.indexes WHERE table_schema = 'PUBLIC'",
            String::class.java
        )

        val expectedIndexes = listOf(
            "idx_rps_document_recorded",
            "idx_rps_recorded_at",
            "idx_documents_location",
            "idx_documents_saved_at",
            "idx_documents_category",
            "idx_highlights_document_id",
            "idx_highlights_highlighted_at",
            "idx_notes_highlight_id",
            "idx_notes_document_id",
            "idx_sync_runs_status",
            "idx_sync_runs_started_at"
        )

        for (index in expectedIndexes) {
            assertTrue(
                indexes.any { it.equals(index, ignoreCase = true) },
                "Index $index should exist after V2 migration"
            )
        }
    }
}