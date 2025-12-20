package com.reader.analytics.analytics.infrastructure.persistence

import com.reader.analytics.analytics.application.AnalyticsStore
import com.reader.analytics.analytics.domain.DateRange
import com.reader.analytics.analytics.domain.Granularity
import com.reader.analytics.analytics.domain.projections.*
import org.springframework.stereotype.Component
import java.time.Duration

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
        val raw = repository.getHighlightStats(dateRange.start, dateRange.end)
        val colors = getColorDistribution()
        val topDocs = getMostHighlightedDocuments(10)

        return HighlightStats(
            totalHighlights = raw.totalHighlights,
            highlightsThisPeriod = raw.highlightsThisPeriod,
            averageHighlightsPerDocument = raw.averagePerDocument,
            colorDistribution = colors,
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
                imageUrl = it.imageUrl
            )
        }

    override fun getColorDistribution(): Map<String, Int> =
        repository.getColorDistribution().associate { it.color to it.count }

    private fun RawDailyStats.toDailyReadingStats() = DailyReadingStats(
        date = date,
        wordsRead = wordsRead,
        articlesProgressed = articlesProgressed,
        articlesCompleted = articlesCompleted
    )
}
