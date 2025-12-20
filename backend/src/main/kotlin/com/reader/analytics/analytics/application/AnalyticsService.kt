package com.reader.analytics.analytics.application

import com.reader.analytics.analytics.domain.DateRange
import com.reader.analytics.analytics.domain.Granularity
import com.reader.analytics.analytics.domain.projections.*
import org.springframework.stereotype.Service

@Service
class AnalyticsService(
    private val analyticsStore: AnalyticsStore
) {

    fun getDashboardSummary(dateRange: DateRange): DashboardSummary {
        val totalWords = analyticsStore.getTotalWordsRead(dateRange)
        val articlesCompleted = analyticsStore.getArticlesCompleted(dateRange)
        val streak = analyticsStore.getReadingStreak()
        val pipeline = analyticsStore.getPipelineStats(dateRange)
        val highlights = analyticsStore.getHighlightStats(dateRange)
        val completionRate = analyticsStore.getCompletionRate(dateRange)

        return DashboardSummary(
            totalWordsReadThisPeriod = totalWords,
            articlesCompletedThisPeriod = articlesCompleted,
            currentStreak = streak.currentStreak,
            backlogSize = pipeline.backlogSize,
            highlightsThisPeriod = highlights.highlightsThisPeriod,
            completionRate = completionRate
        )
    }

    fun getReadingStats(
        dateRange: DateRange,
        granularity: Granularity = Granularity.DAILY
    ): List<DailyReadingStats> =
        analyticsStore.getReadingStats(dateRange, granularity)

    fun getReadingStreak(): ReadingStreak =
        analyticsStore.getReadingStreak()

    fun getPeakReadingHours(dateRange: DateRange): PeakReadingHours =
        analyticsStore.getPeakReadingHours(dateRange)

    fun getPipelineStats(dateRange: DateRange): PipelineStats =
        analyticsStore.getPipelineStats(dateRange)

    fun getLocationBreakdown(): List<LocationBreakdown> =
        analyticsStore.getLocationBreakdown()

    fun getCategoryBreakdown(): List<CategoryBreakdown> =
        analyticsStore.getCategoryBreakdown()

    fun getHighlightStats(dateRange: DateRange): HighlightStats =
        analyticsStore.getHighlightStats(dateRange)

    fun getWordsReadDrillDown(
        dateRange: DateRange,
        cursor: java.util.UUID?,
        limit: Int
    ): WordsReadDrillDown {
        val currentTotal = analyticsStore.getTotalWordsRead(dateRange)
        val previousTotal = analyticsStore.getTotalWordsRead(dateRange.previousPeriod())
        val documents = analyticsStore.getWordsReadDocuments(dateRange, cursor, limit)

        val changePercent = if (previousTotal > 0) {
            ((currentTotal - previousTotal).toDouble() / previousTotal) * 100
        } else {
            null
        }

        return WordsReadDrillDown(
            total = currentTotal,
            changePercent = changePercent,
            documents = documents
        )
    }

    fun getCompletedDrillDown(
        dateRange: DateRange,
        cursor: java.util.UUID?,
        limit: Int
    ): CompletedDrillDown {
        val currentTotal = analyticsStore.getArticlesCompleted(dateRange)
        val previousTotal = analyticsStore.getArticlesCompleted(dateRange.previousPeriod())
        val documents = analyticsStore.getCompletedDocuments(dateRange, cursor, limit)

        val changePercent = if (previousTotal > 0) {
            ((currentTotal - previousTotal).toDouble() / previousTotal) * 100
        } else {
            null
        }

        return CompletedDrillDown(
            total = currentTotal,
            changePercent = changePercent,
            documents = documents
        )
    }

    fun getBacklogDrillDown(
        cursor: java.util.UUID?,
        limit: Int
    ): BacklogDrillDown {
        val total = analyticsStore.getBacklogCount()
        val documents = analyticsStore.getBacklogDocuments(cursor, limit)

        return BacklogDrillDown(
            total = total,
            changePercent = null,
            documents = documents
        )
    }
}
