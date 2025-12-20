package com.reader.analytics.analytics.infrastructure.persistence

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.LocalDate

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
            SELECT
                COUNT(*) FILTER (WHERE location IN ('new', 'later')) AS backlog_size,
                COUNT(*) FILTER (WHERE reading_progress > 0 AND reading_progress < 1) AS in_progress,
                COUNT(*) FILTER (WHERE reading_progress = 1) AS completed,
                COUNT(*) FILTER (WHERE location = 'archive') AS archived
            FROM documents
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

    fun getLocationBreakdown(): List<RawLocationBreakdown> {
        val sql = """
            SELECT
                COALESCE(location, 'unknown') AS location,
                COUNT(*) AS count
            FROM documents
            GROUP BY location
            ORDER BY count DESC
        """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            RawLocationBreakdown(
                location = rs.getString("location"),
                count = rs.getInt("count")
            )
        }
    }

    fun getCategoryBreakdown(): List<RawCategoryBreakdown> {
        val sql = """
            SELECT
                COALESCE(category, 'uncategorized') AS category,
                COUNT(*) AS count,
                AVG(COALESCE(reading_progress, 0)) AS avg_progress
            FROM documents
            GROUP BY category
            ORDER BY count DESC
        """.trimIndent()

        return jdbcTemplate.query(sql) { rs, _ ->
            RawCategoryBreakdown(
                category = rs.getString("category"),
                count = rs.getInt("count"),
                averageProgress = rs.getDouble("avg_progress")
            )
        }
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
            SELECT COUNT(*) FROM highlights
            WHERE note IS NOT NULL AND note != ''
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
                GROUP BY document_readwise_id
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

    fun getMostHighlightedDocuments(limit: Int): List<RawDocumentHighlightCount> {
        val sql = """
            SELECT
                d.readwise_id AS document_id,
                d.title,
                d.category,
                d.image_url,
                COUNT(h.id) AS highlight_count,
                BOOL_OR(h.note IS NOT NULL AND h.note != '') AS has_notes
            FROM documents d
            JOIN highlights h ON h.document_readwise_id = d.readwise_id
            GROUP BY d.id, d.readwise_id, d.title, d.category, d.image_url
            ORDER BY highlight_count DESC
            LIMIT ?
        """.trimIndent()

        return jdbcTemplate.query(sql, { rs, _ ->
            RawDocumentHighlightCount(
                documentId = rs.getString("document_id"),
                title = rs.getString("title"),
                category = rs.getString("category"),
                imageUrl = rs.getString("image_url"),
                highlightCount = rs.getInt("highlight_count"),
                hasNotes = rs.getBoolean("has_notes")
            )
        }, limit)
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
