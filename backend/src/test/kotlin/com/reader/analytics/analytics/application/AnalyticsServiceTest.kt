package com.reader.analytics.analytics.application

import com.reader.analytics.analytics.domain.DateRange
import com.reader.analytics.analytics.domain.Granularity
import com.reader.analytics.analytics.domain.projections.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import kotlin.test.assertEquals

class AnalyticsServiceTest {

    private lateinit var fakeStore: FakeAnalyticsStore
    private lateinit var service: AnalyticsService

    @BeforeEach
    fun setUp() {
        fakeStore = FakeAnalyticsStore()
        service = AnalyticsService(fakeStore)
    }

    @Test
    fun `dashboard summary aggregates all metrics from store`() {
        fakeStore.totalWordsRead = 5000L
        fakeStore.articlesCompleted = 3
        fakeStore.streak = ReadingStreak(
            currentStreak = 5,
            longestStreak = 10,
            currentStreakStartDate = LocalDate.now().minusDays(4),
            longestStreakStartDate = LocalDate.now().minusDays(20),
            longestStreakEndDate = LocalDate.now().minusDays(10)
        )
        fakeStore.pipelineStats = PipelineStats(
            backlogSize = 25,
            inProgressCount = 5,
            completedCount = 50,
            archivedCount = 100,
            saveToReadRatio = 0.75,
            averageQueueLatency = Duration.ofHours(48),
            documentsAddedThisPeriod = 10,
            documentsCompletedThisPeriod = 8
        )
        fakeStore.highlightStats = HighlightStats(
            totalHighlights = 200,
            highlightsWithNotes = 50,
            highlightsThisPeriod = 15,
            highlightsPreviousPeriod = 10,
            averageHighlightsPerDocument = 2.5,
            mostHighlightedDocuments = emptyList()
        )
        fakeStore.completionRate = 0.6

        val summary = service.getDashboardSummary(DateRange.lastWeek())

        assertEquals(5000L, summary.totalWordsReadThisPeriod)
        assertEquals(3, summary.articlesCompletedThisPeriod)
        assertEquals(5, summary.currentStreak)
        assertEquals(25, summary.backlogSize)
        assertEquals(15, summary.highlightsThisPeriod)
        assertEquals(0.6, summary.completionRate)
    }

    @Test
    fun `reading stats delegates to store with correct parameters`() {
        val dateRange = DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 7))

        service.getReadingStats(dateRange, Granularity.DAILY)

        assertEquals(dateRange, fakeStore.lastReadingStatsDateRange)
        assertEquals(Granularity.DAILY, fakeStore.lastReadingStatsGranularity)
    }

    @Test
    fun `reading streak delegates to store`() {
        val expectedStreak = ReadingStreak(
            currentStreak = 7,
            longestStreak = 14,
            currentStreakStartDate = LocalDate.now().minusDays(6),
            longestStreakStartDate = null,
            longestStreakEndDate = null
        )
        fakeStore.streak = expectedStreak

        val result = service.getReadingStreak()

        assertEquals(expectedStreak, result)
    }

    @Test
    fun `pipeline stats delegates to store with date range`() {
        val dateRange = DateRange.lastMonth()

        service.getPipelineStats(dateRange)

        assertEquals(dateRange, fakeStore.lastPipelineStatsDateRange)
    }

    @Test
    fun `highlight stats delegates to store with date range`() {
        val dateRange = DateRange.lastWeek()

        service.getHighlightStats(dateRange)

        assertEquals(dateRange, fakeStore.lastHighlightStatsDateRange)
    }

    class FakeAnalyticsStore : AnalyticsStore {
        var totalWordsRead: Long = 0L
        var articlesCompleted: Int = 0
        var streak = ReadingStreak(0, 0, null, null, null)
        var pipelineStats = PipelineStats(0, 0, 0, 0, 0.0, null, 0, 0)
        var highlightStats = HighlightStats(0, 0, 0, 0, 0.0, emptyList())
        var completionRate: Double = 0.0
        var peakReadingHours = PeakReadingHours(emptyMap(), 0, 0.0)
        var readingStats: List<DailyReadingStats> = emptyList()
        var locationBreakdownResult: List<LocationBreakdown> = emptyList()
        var categoryBreakdownResult: List<CategoryBreakdown> = emptyList()
        var mostHighlightedDocuments: List<DocumentHighlightCount> = emptyList()

        var lastReadingStatsDateRange: DateRange? = null
        var lastReadingStatsGranularity: Granularity? = null
        var lastPipelineStatsDateRange: DateRange? = null
        var lastHighlightStatsDateRange: DateRange? = null

        override fun getReadingStats(dateRange: DateRange, granularity: Granularity): List<DailyReadingStats> {
            lastReadingStatsDateRange = dateRange
            lastReadingStatsGranularity = granularity
            return readingStats
        }

        override fun getTotalWordsRead(dateRange: DateRange) = totalWordsRead

        override fun getArticlesCompleted(dateRange: DateRange) = articlesCompleted

        override fun getReadingStreak() = streak

        override fun getPeakReadingHours(dateRange: DateRange) = peakReadingHours

        override fun getCompletionRate(dateRange: DateRange) = completionRate

        override fun getPipelineStats(dateRange: DateRange): PipelineStats {
            lastPipelineStatsDateRange = dateRange
            return pipelineStats
        }

        override fun getLocationBreakdown() = locationBreakdownResult

        override fun getCategoryBreakdown() = categoryBreakdownResult

        override fun getHighlightStats(dateRange: DateRange): HighlightStats {
            lastHighlightStatsDateRange = dateRange
            return highlightStats
        }

        override fun getMostHighlightedDocuments(limit: Int) = mostHighlightedDocuments.take(limit)
    }
}
