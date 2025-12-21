package com.reader.analytics.analytics.application

import com.reader.analytics.analytics.domain.DateRange
import com.reader.analytics.analytics.domain.Granularity
import com.reader.analytics.analytics.domain.projections.*

interface AnalyticsStore {

    fun getReadingStats(dateRange: DateRange, granularity: Granularity): List<DailyReadingStats>

    fun getTotalWordsRead(dateRange: DateRange): Long

    fun getArticlesCompleted(dateRange: DateRange): Int

    fun getReadingStreak(): ReadingStreak

    fun getPeakReadingHours(dateRange: DateRange): PeakReadingHours

    fun getCompletionRate(dateRange: DateRange): Double

    fun getPipelineStats(dateRange: DateRange): PipelineStats

    fun getLocationBreakdown(dateRange: DateRange): List<LocationBreakdown>

    fun getCategoryBreakdown(dateRange: DateRange): List<CategoryBreakdown>

    fun getHighlightStats(dateRange: DateRange): HighlightStats

    fun getMostHighlightedDocuments(dateRange: DateRange, limit: Int): List<DocumentHighlightCount>

    // Drill-down methods
    fun getWordsReadDocuments(
        dateRange: DateRange,
        cursor: java.util.UUID?,
        limit: Int
    ): DrillDownPage<WordsReadDocument>

    fun getCompletedDocuments(
        dateRange: DateRange,
        cursor: java.util.UUID?,
        limit: Int
    ): DrillDownPage<CompletedDocument>

    fun getBacklogDocuments(
        cursor: java.util.UUID?,
        limit: Int
    ): DrillDownPage<BacklogDocument>

    fun getBacklogCount(): Int
}
