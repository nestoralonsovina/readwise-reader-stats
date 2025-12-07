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

    fun getLocationBreakdown(): List<LocationBreakdown>

    fun getCategoryBreakdown(): List<CategoryBreakdown>

    fun getHighlightStats(dateRange: DateRange): HighlightStats

    fun getMostHighlightedDocuments(limit: Int): List<DocumentHighlightCount>

    fun getColorDistribution(): Map<String, Int>
}
