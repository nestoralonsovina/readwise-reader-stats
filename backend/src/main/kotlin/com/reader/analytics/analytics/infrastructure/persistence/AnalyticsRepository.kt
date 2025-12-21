package com.reader.analytics.analytics.infrastructure.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Repository
class AnalyticsRepository(
    private val jdbcTemplate: JdbcTemplate
) {

    fun getReadingStatsByPeriod(
        startDate: LocalDate,
        endDate: LocalDate,
        truncUnit: String
    ): List<RawDailyStats> {
        val sql = """
            WITH progress_deltas AS (
                SELECT
                    document_id,
                    reading_progress,
                    word_count,
                    DATE_TRUNC('$truncUnit', recorded_at) AS period,
                    reading_progress - LAG(reading_progress, 1, 0) OVER (
                        PARTITION BY document_id
                        ORDER BY recorded_at
                    ) AS progress_delta
                FROM reading_progress_snapshots
                WHERE recorded_at >= ? AND recorded_at < ?
            ),
            period_stats AS (
                SELECT
                    period,
                    SUM(CASE
                        WHEN progress_delta > 0 AND word_count IS NOT NULL
                        THEN (progress_delta * word_count)::bigint
                        ELSE 0
                    END) AS words_read,
                    COUNT(DISTINCT CASE WHEN progress_delta > 0 THEN document_id END) AS articles_progressed,
                    COUNT(DISTINCT CASE WHEN reading_progress = 1 AND progress_delta > 0 THEN document_id END) AS articles_completed
                FROM progress_deltas
                WHERE progress_delta > 0
                GROUP BY period
            )
            SELECT
                period::date AS date,
                COALESCE(words_read, 0) AS words_read,
                COALESCE(articles_progressed, 0) AS articles_progressed,
                COALESCE(articles_completed, 0) AS articles_completed
            FROM period_stats
            ORDER BY period
        """.trimIndent()

        return jdbcTemplate.query(
            sql,
            { rs, _ ->
                RawDailyStats(
                    date = rs.getDate("date").toLocalDate(),
                    wordsRead = rs.getLong("words_read"),
                    articlesProgressed = rs.getInt("articles_progressed"),
                    articlesCompleted = rs.getInt("articles_completed")
                )
            },
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        )
    }

    fun getTotalWordsRead(startDate: LocalDate, endDate: LocalDate): Long {
        val sql = """
            WITH progress_deltas AS (
                SELECT
                    document_id,
                    reading_progress,
                    word_count,
                    reading_progress - LAG(reading_progress, 1, 0) OVER (
                        PARTITION BY document_id
                        ORDER BY recorded_at
                    ) AS progress_delta
                FROM reading_progress_snapshots
                WHERE recorded_at >= ? AND recorded_at < ?
            )
            SELECT COALESCE(SUM(
                CASE
                    WHEN progress_delta > 0 AND word_count IS NOT NULL
                    THEN (progress_delta * word_count)::bigint
                    ELSE 0
                END
            ), 0) AS total_words
            FROM progress_deltas
            WHERE progress_delta > 0
        """.trimIndent()

        return jdbcTemplate.queryForObject(
            sql,
            Long::class.java,
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        ) ?: 0L
    }

    fun getArticlesCompleted(startDate: LocalDate, endDate: LocalDate): Int {
        val sql = """
            SELECT COUNT(DISTINCT document_id)
            FROM reading_progress_snapshots
            WHERE reading_progress = 1.0
            AND recorded_at >= ? AND recorded_at < ?
        """.trimIndent()

        return jdbcTemplate.queryForObject(
            sql,
            Int::class.java,
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        ) ?: 0
    }

    fun getReadingStreak(): RawStreakData {
        val sql = """
            WITH daily_activity AS (
                SELECT DISTINCT DATE(recorded_at) AS activity_date
                FROM reading_progress_snapshots
                WHERE reading_progress > 0
            ),
            streak_groups AS (
                SELECT
                    activity_date,
                    activity_date - (ROW_NUMBER() OVER (ORDER BY activity_date))::integer AS streak_group
                FROM daily_activity
            ),
            streaks AS (
                SELECT
                    streak_group,
                    MIN(activity_date) AS streak_start,
                    MAX(activity_date) AS streak_end,
                    COUNT(*) AS streak_length
                FROM streak_groups
                GROUP BY streak_group
            ),
            current_streak AS (
                SELECT
                    streak_length,
                    streak_start,
                    streak_end
                FROM streaks
                WHERE streak_end >= CURRENT_DATE - INTERVAL '1 day'
                ORDER BY streak_end DESC
                LIMIT 1
            ),
            longest_streak AS (
                SELECT
                    streak_length,
                    streak_start,
                    streak_end
                FROM streaks
                ORDER BY streak_length DESC
                LIMIT 1
            )
            SELECT
                COALESCE((SELECT streak_length FROM current_streak), 0) AS current_streak,
                COALESCE((SELECT streak_length FROM longest_streak), 0) AS longest_streak,
                (SELECT streak_start FROM current_streak) AS current_streak_start,
                (SELECT streak_start FROM longest_streak) AS longest_streak_start,
                (SELECT streak_end FROM longest_streak) AS longest_streak_end
        """.trimIndent()

        return jdbcTemplate.queryForObject(sql) { rs, _ ->
            RawStreakData(
                currentStreak = rs.getInt("current_streak"),
                longestStreak = rs.getInt("longest_streak"),
                currentStreakStart = rs.getDate("current_streak_start")?.toLocalDate(),
                longestStreakStart = rs.getDate("longest_streak_start")?.toLocalDate(),
                longestStreakEnd = rs.getDate("longest_streak_end")?.toLocalDate()
            )
        } ?: RawStreakData(0, 0, null, null, null)
    }

    fun getPeakReadingHours(startDate: LocalDate, endDate: LocalDate): List<RawHourlyActivity> {
        val sql = """
            SELECT
                EXTRACT(HOUR FROM last_opened_at)::integer AS hour,
                COUNT(*) AS activity_count
            FROM reading_progress_snapshots
            WHERE last_opened_at IS NOT NULL
            AND recorded_at >= ? AND recorded_at < ?
            GROUP BY EXTRACT(HOUR FROM last_opened_at)
            ORDER BY hour
        """.trimIndent()

        return jdbcTemplate.query(
            sql,
            { rs, _ ->
                RawHourlyActivity(
                    hour = rs.getInt("hour"),
                    activityCount = rs.getInt("activity_count")
                )
            },
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        )
    }

    fun getPipelineStats(): RawPipelineStats {
        val sql = """
            WITH latest_progress AS (
                SELECT DISTINCT ON (document_id)
                    document_id,
                    reading_progress
                FROM reading_progress_snapshots
                ORDER BY document_id, recorded_at DESC
            )
            SELECT
                COUNT(*) FILTER (WHERE d.location IN ('new', 'later')) AS backlog_size,
                COUNT(*) FILTER (WHERE lp.reading_progress > 0 AND lp.reading_progress < 1) AS in_progress,
                COUNT(*) FILTER (WHERE lp.reading_progress = 1) AS completed,
                COUNT(*) FILTER (WHERE d.location = 'archive') AS archived
            FROM documents d
            LEFT JOIN latest_progress lp ON lp.document_id = d.readwise_id
        """.trimIndent()

        return jdbcTemplate.queryForObject(sql) { rs, _ ->
            RawPipelineStats(
                backlogSize = rs.getInt("backlog_size"),
                inProgressCount = rs.getInt("in_progress"),
                completedCount = rs.getInt("completed"),
                archivedCount = rs.getInt("archived")
            )
        } ?: RawPipelineStats(0, 0, 0, 0)
    }

    fun getPipelinePeriodStats(startDate: LocalDate, endDate: LocalDate): RawPipelinePeriodStats {
        val addedSql = """
            SELECT COUNT(*) FROM documents
            WHERE saved_at >= ? AND saved_at < ?
        """.trimIndent()

        val added = jdbcTemplate.queryForObject(
            addedSql, Int::class.java,
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        ) ?: 0

        val completedSql = """
            SELECT COUNT(DISTINCT document_id)
            FROM reading_progress_snapshots
            WHERE reading_progress = 1.0
            AND recorded_at >= ? AND recorded_at < ?
        """.trimIndent()

        val completed = jdbcTemplate.queryForObject(
            completedSql, Int::class.java,
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        ) ?: 0

        val latencySql = """
            SELECT AVG(EXTRACT(EPOCH FROM (rps.first_opened_at - d.saved_at)))
            FROM documents d
            JOIN LATERAL (
                SELECT first_opened_at
                FROM reading_progress_snapshots
                WHERE document_id = d.readwise_id
                AND first_opened_at IS NOT NULL
                ORDER BY recorded_at
                LIMIT 1
            ) rps ON true
            WHERE d.saved_at IS NOT NULL
            AND rps.first_opened_at IS NOT NULL
        """.trimIndent()

        val avgLatencySeconds = try {
            jdbcTemplate.queryForObject(latencySql, Double::class.java)
        } catch (e: Exception) {
            null
        }

        return RawPipelinePeriodStats(
            documentsAdded = added,
            documentsCompleted = completed,
            avgQueueLatencySeconds = avgLatencySeconds
        )
    }

    fun getLocationBreakdown(startDate: LocalDate, endDate: LocalDate): List<RawLocationBreakdown> {
        val sql = """
            SELECT
                COALESCE(location, 'unknown') AS location,
                COUNT(*) AS count
            FROM documents
            WHERE saved_at >= ? AND saved_at < ?
            GROUP BY location
            ORDER BY count DESC
        """.trimIndent()

        return jdbcTemplate.query(
            sql,
            { rs, _ ->
                RawLocationBreakdown(
                    location = rs.getString("location"),
                    count = rs.getInt("count")
                )
            },
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        )
    }

    fun getCategoryBreakdown(startDate: LocalDate, endDate: LocalDate): List<RawCategoryBreakdown> {
        val sql = """
            WITH latest_progress AS (
                SELECT DISTINCT ON (document_id)
                    document_id,
                    reading_progress
                FROM reading_progress_snapshots
                ORDER BY document_id, recorded_at DESC
            )
            SELECT
                COALESCE(d.category, 'uncategorized') AS category,
                COUNT(*) AS count,
                AVG(COALESCE(lp.reading_progress, 0)) AS avg_progress
            FROM documents d
            LEFT JOIN latest_progress lp ON lp.document_id = d.readwise_id
            WHERE d.saved_at >= ? AND d.saved_at < ?
            GROUP BY d.category
            ORDER BY count DESC
        """.trimIndent()

        return jdbcTemplate.query(
            sql,
            { rs, _ ->
                RawCategoryBreakdown(
                    category = rs.getString("category"),
                    count = rs.getInt("count"),
                    averageProgress = rs.getDouble("avg_progress")
                )
            },
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        )
    }

    fun getHighlightStats(
        currentStart: LocalDate,
        currentEnd: LocalDate,
        previousStart: LocalDate,
        previousEnd: LocalDate
    ): RawHighlightStats {
        val totalSql = "SELECT COUNT(*) FROM highlights"
        val total = jdbcTemplate.queryForObject(totalSql, Int::class.java) ?: 0

        val withNotesSql = """
            SELECT COUNT(DISTINCT h.id)
            FROM highlights h
            WHERE EXISTS (SELECT 1 FROM notes n WHERE n.highlight_id = h.id)
        """.trimIndent()
        val withNotes = jdbcTemplate.queryForObject(withNotesSql, Int::class.java) ?: 0

        val periodSql = """
            SELECT COUNT(*) FROM highlights
            WHERE highlighted_at >= ? AND highlighted_at < ?
        """.trimIndent()
        val currentPeriod = jdbcTemplate.queryForObject(
            periodSql, Int::class.java,
            currentStart.atStartOfDay(),
            currentEnd.plusDays(1).atStartOfDay()
        ) ?: 0

        val previousPeriod = jdbcTemplate.queryForObject(
            periodSql, Int::class.java,
            previousStart.atStartOfDay(),
            previousEnd.plusDays(1).atStartOfDay()
        ) ?: 0

        val avgSql = """
            SELECT COALESCE(AVG(highlight_count), 0) FROM (
                SELECT COUNT(*) AS highlight_count
                FROM highlights
                GROUP BY document_id
            ) AS doc_counts
        """.trimIndent()
        val avg = jdbcTemplate.queryForObject(avgSql, Double::class.java) ?: 0.0

        return RawHighlightStats(
            totalHighlights = total,
            highlightsWithNotes = withNotes,
            highlightsThisPeriod = currentPeriod,
            highlightsPreviousPeriod = previousPeriod,
            averagePerDocument = avg
        )
    }

    fun getMostHighlightedDocuments(
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int
    ): List<RawDocumentHighlightCount> {
        val sql = """
            SELECT
                d.readwise_id AS document_id,
                d.title,
                d.category,
                d.image_url,
                COUNT(h.id) AS highlight_count,
                BOOL_OR(EXISTS (
                    SELECT 1 FROM notes n WHERE n.highlight_id = h.id
                )) OR EXISTS (
                    SELECT 1 FROM notes n WHERE n.document_id = d.id
                ) AS has_notes
            FROM documents d
            JOIN highlights h ON h.document_id = d.id
            WHERE h.highlighted_at >= ? AND h.highlighted_at < ?
            GROUP BY d.id, d.readwise_id, d.title, d.category, d.image_url
            ORDER BY highlight_count DESC
            LIMIT ?
        """.trimIndent()

        return jdbcTemplate.query(
            sql,
            { rs, _ ->
                RawDocumentHighlightCount(
                    documentId = rs.getString("document_id"),
                    title = rs.getString("title"),
                    category = rs.getString("category"),
                    imageUrl = rs.getString("image_url"),
                    highlightCount = rs.getInt("highlight_count"),
                    hasNotes = rs.getBoolean("has_notes")
                )
            },
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay(),
            limit
        )
    }

    fun getCompletionRate(startDate: LocalDate, endDate: LocalDate): Double {
        val sql = """
            WITH started AS (
                SELECT COUNT(DISTINCT document_id) AS count
                FROM reading_progress_snapshots
                WHERE reading_progress > 0
                AND recorded_at >= ? AND recorded_at < ?
            ),
            completed AS (
                SELECT COUNT(DISTINCT document_id) AS count
                FROM reading_progress_snapshots
                WHERE reading_progress = 1.0
                AND recorded_at >= ? AND recorded_at < ?
            )
            SELECT
                CASE
                    WHEN (SELECT count FROM started) = 0 THEN 0
                    ELSE (SELECT count FROM completed)::float / (SELECT count FROM started)
                END AS rate
        """.trimIndent()

        return jdbcTemplate.queryForObject(
            sql, Double::class.java,
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay(),
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        ) ?: 0.0
    }

    fun getWordsReadDocuments(
        startDate: LocalDate,
        endDate: LocalDate,
        cursor: UUID?,
        limit: Int
    ): List<RawWordsReadDocument> {
        val sql = """
            WITH latest_progress AS (
                SELECT DISTINCT ON (document_id)
                    document_id,
                    reading_progress,
                    word_count
                FROM reading_progress_snapshots
                WHERE recorded_at >= ? AND recorded_at < ?
                ORDER BY document_id, recorded_at DESC
            )
            SELECT d.id, d.readwise_id, d.title, d.author, d.url, d.image_url, d.category,
                   ROUND(COALESCE(lp.reading_progress, 0) * COALESCE(lp.word_count, 0))::bigint AS words_read,
                   COALESCE(lp.reading_progress, 0) AS reading_progress
            FROM latest_progress lp
            JOIN documents d ON d.readwise_id = lp.document_id
            WHERE COALESCE(lp.reading_progress, 0) > 0
              AND COALESCE(lp.word_count, 0) > 0
              ${if (cursor != null) "AND d.id < ?::uuid" else ""}
            ORDER BY words_read DESC, d.id DESC
            LIMIT ?
        """.trimIndent()

        val params = mutableListOf<Any>(
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        )
        if (cursor != null) params.add(cursor.toString())
        params.add(limit)

        return jdbcTemplate.query(sql, { rs, _ ->
            RawWordsReadDocument(
                id = UUID.fromString(rs.getString("id")),
                readwiseId = rs.getString("readwise_id"),
                title = rs.getString("title"),
                author = rs.getString("author"),
                url = rs.getString("url"),
                imageUrl = rs.getString("image_url"),
                category = rs.getString("category"),
                wordsRead = rs.getLong("words_read"),
                readingProgress = rs.getDouble("reading_progress")
            )
        }, *params.toTypedArray())
    }

    fun getCompletedDocuments(
        startDate: LocalDate,
        endDate: LocalDate,
        cursor: UUID?,
        limit: Int
    ): List<RawCompletedDocument> {
        val sql = """
            WITH completion_events AS (
                SELECT DISTINCT ON (document_id)
                    document_id,
                    recorded_at AS completed_at
                FROM reading_progress_snapshots
                WHERE reading_progress = 1.0
                  AND recorded_at >= ? AND recorded_at < ?
                ORDER BY document_id, recorded_at ASC
            )
            SELECT d.id, d.readwise_id, d.title, d.author, d.url, d.image_url, d.category,
                   ce.completed_at
            FROM completion_events ce
            JOIN documents d ON d.readwise_id = ce.document_id
            ${if (cursor != null) "WHERE d.id < ?::uuid" else ""}
            ORDER BY ce.completed_at DESC, d.id DESC
            LIMIT ?
        """.trimIndent()

        val params = mutableListOf<Any>(
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay()
        )
        if (cursor != null) params.add(cursor.toString())
        params.add(limit)

        return jdbcTemplate.query(sql, { rs, _ ->
            RawCompletedDocument(
                id = UUID.fromString(rs.getString("id")),
                readwiseId = rs.getString("readwise_id"),
                title = rs.getString("title"),
                author = rs.getString("author"),
                url = rs.getString("url"),
                imageUrl = rs.getString("image_url"),
                category = rs.getString("category"),
                completedAt = rs.getTimestamp("completed_at").toInstant()
            )
        }, *params.toTypedArray())
    }

    fun getBacklogDocuments(cursor: UUID?, limit: Int): List<RawBacklogDocument> {
        val sql = """
            WITH latest_progress AS (
                SELECT DISTINCT ON (document_id)
                    document_id,
                    reading_progress
                FROM reading_progress_snapshots
                ORDER BY document_id, recorded_at DESC
            )
            SELECT d.id, d.readwise_id, d.title, d.author, d.url, d.image_url, d.category,
                   d.saved_at,
                   COALESCE(EXTRACT(DAY FROM NOW() - d.saved_at)::int, 0) AS days_waiting
            FROM documents d
            LEFT JOIN latest_progress lp ON lp.document_id = d.readwise_id
            WHERE COALESCE(lp.reading_progress, 0) < 0.1
              AND d.location IN ('new', 'later', 'shortlist')
              ${if (cursor != null) "AND d.id > ?::uuid" else ""}
            ORDER BY d.saved_at ASC NULLS LAST, d.id ASC
            LIMIT ?
        """.trimIndent()

        val params = mutableListOf<Any>()
        if (cursor != null) params.add(cursor.toString())
        params.add(limit)

        return jdbcTemplate.query(sql, { rs, _ ->
            RawBacklogDocument(
                id = UUID.fromString(rs.getString("id")),
                readwiseId = rs.getString("readwise_id"),
                title = rs.getString("title"),
                author = rs.getString("author"),
                url = rs.getString("url"),
                imageUrl = rs.getString("image_url"),
                category = rs.getString("category"),
                savedAt = rs.getTimestamp("saved_at")?.toInstant(),
                daysWaiting = rs.getInt("days_waiting")
            )
        }, *params.toTypedArray())
    }

    fun getBacklogCount(): Int {
        val sql = """
            WITH latest_progress AS (
                SELECT DISTINCT ON (document_id)
                    document_id,
                    reading_progress
                FROM reading_progress_snapshots
                ORDER BY document_id, recorded_at DESC
            )
            SELECT COUNT(*)
            FROM documents d
            LEFT JOIN latest_progress lp ON lp.document_id = d.readwise_id
            WHERE COALESCE(lp.reading_progress, 0) < 0.1
              AND d.location IN ('new', 'later', 'shortlist')
        """.trimIndent()

        return jdbcTemplate.queryForObject(sql, Int::class.java) ?: 0
    }
}

data class RawDailyStats(
    val date: LocalDate,
    val wordsRead: Long,
    val articlesProgressed: Int,
    val articlesCompleted: Int
)

data class RawStreakData(
    val currentStreak: Int,
    val longestStreak: Int,
    val currentStreakStart: LocalDate?,
    val longestStreakStart: LocalDate?,
    val longestStreakEnd: LocalDate?
)

data class RawHourlyActivity(
    val hour: Int,
    val activityCount: Int
)

data class RawPipelineStats(
    val backlogSize: Int,
    val inProgressCount: Int,
    val completedCount: Int,
    val archivedCount: Int
)

data class RawPipelinePeriodStats(
    val documentsAdded: Int,
    val documentsCompleted: Int,
    val avgQueueLatencySeconds: Double?
)

data class RawLocationBreakdown(
    val location: String,
    val count: Int
)

data class RawCategoryBreakdown(
    val category: String,
    val count: Int,
    val averageProgress: Double
)

data class RawHighlightStats(
    val totalHighlights: Int,
    val highlightsWithNotes: Int,
    val highlightsThisPeriod: Int,
    val highlightsPreviousPeriod: Int,
    val averagePerDocument: Double
)

data class RawDocumentHighlightCount(
    val documentId: String,
    val title: String?,
    val category: String?,
    val imageUrl: String?,
    val highlightCount: Int,
    val hasNotes: Boolean
)

data class RawWordsReadDocument(
    val id: UUID,
    val readwiseId: String,
    val title: String?,
    val author: String?,
    val url: String,
    val imageUrl: String?,
    val category: String?,
    val wordsRead: Long,
    val readingProgress: Double
)

data class RawCompletedDocument(
    val id: UUID,
    val readwiseId: String,
    val title: String?,
    val author: String?,
    val url: String,
    val imageUrl: String?,
    val category: String?,
    val completedAt: Instant
)

data class RawBacklogDocument(
    val id: UUID,
    val readwiseId: String,
    val title: String?,
    val author: String?,
    val url: String,
    val imageUrl: String?,
    val category: String?,
    val savedAt: Instant?,
    val daysWaiting: Int
)
