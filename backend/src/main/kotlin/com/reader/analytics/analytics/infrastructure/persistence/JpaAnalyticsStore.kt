package com.reader.analytics.analytics.infrastructure.persistence

import com.reader.analytics.analytics.application.AnalyticsStore
import com.reader.analytics.analytics.domain.DateRange
import com.reader.analytics.analytics.domain.Granularity
import com.reader.analytics.analytics.domain.projections.*
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class JpaAnalyticsStore(
    private val repository: AnalyticsRepository
) : AnalyticsStore {

    override fun getReadingStats(
        dateRange: DateRange,
        granularity: Granularity
    ): List<DailyReadingStats> {
        val raw = repository.getReadingStatsByPeriod(
            dateRange.start,
            dateRange.end,
            granularity.toPostgresTrunc()
        )
        return raw.map { it.toDailyReadingStats() }
    }

    override fun getTotalWordsRead(dateRange: DateRange): Long =
        repository.getTotalWordsRead(dateRange.start, dateRange.end)

    override fun getArticlesCompleted(dateRange: DateRange): Int =
        repository.getArticlesCompleted(dateRange.start, dateRange.end)

    override fun getReadingStreak(): ReadingStreak {
        val raw = repository.getReadingStreak()
        return ReadingStreak(
            currentStreak = raw.currentStreak,
            longestStreak = raw.longestStreak,
            currentStreakStartDate = raw.currentStreakStart,
            longestStreakStartDate = raw.longestStreakStart,
            longestStreakEndDate = raw.longestStreakEnd
        )
    }

    override fun getPeakReadingHours(dateRange: DateRange): PeakReadingHours {
        val hourly = repository.getPeakReadingHours(dateRange.start, dateRange.end)
        val distribution = hourly.associate { it.hour to it.activityCount }
        val total = hourly.sumOf { it.activityCount }
        val peak = hourly.maxByOrNull { it.activityCount }

        return PeakReadingHours(
            hourlyDistribution = distribution,
            peakHour = peak?.hour ?: 0,
            peakHourPercentage = if (total > 0 && peak != null) {
                (peak.activityCount.toDouble() / total) * 100
            } else 0.0
        )
    }

    override fun getCompletionRate(dateRange: DateRange): Double =
        repository.getCompletionRate(dateRange.start, dateRange.end)

    override fun getPipelineStats(dateRange: DateRange): PipelineStats {
        val current = repository.getPipelineStats()
        val period = repository.getPipelinePeriodStats(dateRange.start, dateRange.end)

        val saveToReadRatio = if (period.documentsAdded > 0) {
            period.documentsCompleted.toDouble() / period.documentsAdded
        } else 0.0

        return PipelineStats(
            backlogSize = current.backlogSize,
            inProgressCount = current.inProgressCount,
            completedCount = current.completedCount,
            archivedCount = current.archivedCount,
            saveToReadRatio = saveToReadRatio,
            averageQueueLatency = period.avgQueueLatencySeconds?.let {
                Duration.ofSeconds(it.toLong())
            },
            documentsAddedThisPeriod = period.documentsAdded,
            documentsCompletedThisPeriod = period.documentsCompleted
        )
    }

    override fun getLocationBreakdown(): List<LocationBreakdown> {
        val raw = repository.getLocationBreakdown()
        val total = raw.sumOf { it.count }
        return raw.map {
            LocationBreakdown(
                location = it.location,
                count = it.count,
                percentage = if (total > 0) (it.count.toDouble() / total) * 100 else 0.0
            )
        }
    }

    override fun getCategoryBreakdown(): List<CategoryBreakdown> =
        repository.getCategoryBreakdown().map {
            CategoryBreakdown(
                category = it.category,
                count = it.count,
                averageProgress = it.averageProgress
            )
        }

    override fun getHighlightStats(dateRange: DateRange): HighlightStats {
        val previousRange = dateRange.previousPeriod()
        val raw = repository.getHighlightStats(
            currentStart = dateRange.start,
            currentEnd = dateRange.end,
            previousStart = previousRange.start,
            previousEnd = previousRange.end
        )
        val topDocs = getMostHighlightedDocuments(10)

        return HighlightStats(
            totalHighlights = raw.totalHighlights,
            highlightsWithNotes = raw.highlightsWithNotes,
            highlightsThisPeriod = raw.highlightsThisPeriod,
            highlightsPreviousPeriod = raw.highlightsPreviousPeriod,
            averageHighlightsPerDocument = raw.averagePerDocument,
            mostHighlightedDocuments = topDocs
        )
    }

    override fun getMostHighlightedDocuments(limit: Int): List<DocumentHighlightCount> =
        repository.getMostHighlightedDocuments(limit).map {
            DocumentHighlightCount(
                documentId = it.documentId,
                title = it.title,
                highlightCount = it.highlightCount,
                category = it.category,
                imageUrl = it.imageUrl,
                hasNotes = it.hasNotes
            )
        }

    private fun RawDailyStats.toDailyReadingStats() = DailyReadingStats(
        date = date,
        wordsRead = wordsRead,
        articlesProgressed = articlesProgressed,
        articlesCompleted = articlesCompleted
    )

    override fun getWordsReadDocuments(
        dateRange: DateRange,
        cursor: UUID?,
        limit: Int
    ): DrillDownPage<WordsReadDocument> {
        val results = repository.getWordsReadDocuments(
            dateRange.start,
            dateRange.end,
            cursor,
            limit + 1
        )

        val hasMore = results.size > limit
        val items = results.take(limit).map {
            WordsReadDocument(
                id = it.id,
                readwiseId = it.readwiseId,
                title = it.title,
                author = it.author,
                url = it.url,
                imageUrl = it.imageUrl,
                category = it.category,
                wordsRead = it.wordsRead,
                readingProgress = it.readingProgress
            )
        }

        return DrillDownPage(
            items = items,
            hasMore = hasMore,
            nextCursor = if (hasMore) items.lastOrNull()?.id else null
        )
    }

    override fun getCompletedDocuments(
        dateRange: DateRange,
        cursor: UUID?,
        limit: Int
    ): DrillDownPage<CompletedDocument> {
        val results = repository.getCompletedDocuments(
            dateRange.start,
            dateRange.end,
            cursor,
            limit + 1
        )

        val hasMore = results.size > limit
        val items = results.take(limit).map {
            CompletedDocument(
                id = it.id,
                readwiseId = it.readwiseId,
                title = it.title,
                author = it.author,
                url = it.url,
                imageUrl = it.imageUrl,
                category = it.category,
                completedAt = it.completedAt
            )
        }

        return DrillDownPage(
            items = items,
            hasMore = hasMore,
            nextCursor = if (hasMore) items.lastOrNull()?.id else null
        )
    }

    override fun getBacklogDocuments(
        cursor: UUID?,
        limit: Int
    ): DrillDownPage<BacklogDocument> {
        val results = repository.getBacklogDocuments(cursor, limit + 1)

        val hasMore = results.size > limit
        val items = results.take(limit).map {
            BacklogDocument(
                id = it.id,
                readwiseId = it.readwiseId,
                title = it.title,
                author = it.author,
                url = it.url,
                imageUrl = it.imageUrl,
                category = it.category,
                savedAt = it.savedAt ?: java.time.Instant.now(),
                daysWaiting = it.daysWaiting
            )
        }

        return DrillDownPage(
            items = items,
            hasMore = hasMore,
            nextCursor = if (hasMore) items.lastOrNull()?.id else null
        )
    }

    override fun getBacklogCount(): Int = repository.getBacklogCount()
}
