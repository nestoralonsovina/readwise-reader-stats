package com.reader.analytics.analytics.application

import com.reader.analytics.analytics.domain.DateRange
import com.reader.analytics.analytics.domain.Granularity
import com.reader.analytics.analytics.domain.projections.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun `words read drill-down returns documents with change percent`() {
        val doc1 = WordsReadDocument(
            id = UUID.randomUUID(),
            readwiseId = "rw-1",
            title = "Article 1",
            author = "Author 1",
            url = "https://example.com/article1",
            imageUrl = null,
            category = "article",
            wordsRead = 5000,
            readingProgress = 1.0
        )
        fakeStore.wordsReadDocuments = DrillDownPage(
            items = listOf(doc1),
            hasMore = false,
            nextCursor = null
        )
        fakeStore.totalWordsRead = 5000
        fakeStore.previousPeriodWordsRead = 4000

        val dateRange = DateRange.lastMonth()
        val result = service.getWordsReadDrillDown(dateRange, cursor = null, limit = 20)

        assertEquals(5000L, result.total)
        assertEquals(25.0, result.changePercent)
        assertEquals(1, result.documents.items.size)
        assertEquals(doc1.id, result.documents.items[0].id)
    }

    @Test
    fun `words read drill-down returns null change percent when no previous data`() {
        fakeStore.wordsReadDocuments = DrillDownPage(emptyList(), false, null)
        fakeStore.totalWordsRead = 1000
        fakeStore.previousPeriodWordsRead = 0

        val result = service.getWordsReadDrillDown(DateRange.lastMonth(), null, 20)

        assertNull(result.changePercent)
    }

    @Test
    fun `completed drill-down returns documents with change percent`() {
        val doc1 = CompletedDocument(
            id = UUID.randomUUID(),
            readwiseId = "rw-1",
            title = "Completed Article",
            author = "Author 1",
            url = "https://example.com/article1",
            imageUrl = null,
            category = "article",
            completedAt = Instant.now()
        )
        fakeStore.completedDocuments = DrillDownPage(
            items = listOf(doc1),
            hasMore = false,
            nextCursor = null
        )
        fakeStore.articlesCompleted = 10
        fakeStore.previousPeriodCompleted = 8

        val result = service.getCompletedDrillDown(DateRange.lastMonth(), null, 20)

        assertEquals(10, result.total)
        assertEquals(25.0, result.changePercent)
        assertEquals(1, result.documents.items.size)
    }

    @Test
    fun `backlog drill-down returns documents with change percent`() {
        val doc1 = BacklogDocument(
            id = UUID.randomUUID(),
            readwiseId = "rw-1",
            title = "Backlog Item",
            author = "Author 1",
            url = "https://example.com/article1",
            imageUrl = null,
            category = "article",
            savedAt = Instant.now().minusSeconds(86400 * 30),
            daysWaiting = 30
        )
        fakeStore.backlogDocuments = DrillDownPage(
            items = listOf(doc1),
            hasMore = false,
            nextCursor = null
        )
        fakeStore.backlogTotalCount = 50
        fakeStore.pipelineStats = PipelineStats(
            backlogSize = 50,
            inProgressCount = 5,
            completedCount = 100,
            archivedCount = 200,
            saveToReadRatio = 0.8,
            averageQueueLatency = null,
            documentsAddedThisPeriod = 10,
            documentsCompletedThisPeriod = 8
        )

        val result = service.getBacklogDrillDown(null, 20)

        assertEquals(50, result.total)
        assertEquals(1, result.documents.items.size)
    }

    class FakeAnalyticsStore : AnalyticsStore {
        var totalWordsRead: Long = 0L
        var previousPeriodWordsRead: Long = 0L
        private var totalWordsReadCalls = 0
        var articlesCompleted: Int = 0
        var previousPeriodCompleted: Int = 0
        private var articlesCompletedCalls = 0
        var streak = ReadingStreak(0, 0, null, null, null)
        var pipelineStats = PipelineStats(0, 0, 0, 0, 0.0, null, 0, 0)
        var highlightStats = HighlightStats(0, 0, 0, 0, 0.0, emptyList())
        var completionRate: Double = 0.0
        var peakReadingHours = PeakReadingHours(emptyMap(), 0, 0.0)
        var readingStats: List<DailyReadingStats> = emptyList()
        var locationBreakdownResult: List<LocationBreakdown> = emptyList()
        var categoryBreakdownResult: List<CategoryBreakdown> = emptyList()
        var mostHighlightedDocuments: List<DocumentHighlightCount> = emptyList()

        var wordsReadDocuments: DrillDownPage<WordsReadDocument> = DrillDownPage(emptyList(), false, null)
        var completedDocuments: DrillDownPage<CompletedDocument> = DrillDownPage(emptyList(), false, null)
        var backlogDocuments: DrillDownPage<BacklogDocument> = DrillDownPage(emptyList(), false, null)
        var backlogTotalCount: Int = 0

        var lastReadingStatsDateRange: DateRange? = null
        var lastReadingStatsGranularity: Granularity? = null
        var lastPipelineStatsDateRange: DateRange? = null
        var lastHighlightStatsDateRange: DateRange? = null

        override fun getReadingStats(dateRange: DateRange, granularity: Granularity): List<DailyReadingStats> {
            lastReadingStatsDateRange = dateRange
            lastReadingStatsGranularity = granularity
            return readingStats
        }

        override fun getTotalWordsRead(dateRange: DateRange): Long {
            totalWordsReadCalls++
            return if (totalWordsReadCalls == 1) totalWordsRead else previousPeriodWordsRead
        }

        override fun getArticlesCompleted(dateRange: DateRange): Int {
            articlesCompletedCalls++
            return if (articlesCompletedCalls == 1) articlesCompleted else previousPeriodCompleted
        }

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

        override fun getWordsReadDocuments(
            dateRange: DateRange,
            cursor: UUID?,
            limit: Int
        ): DrillDownPage<WordsReadDocument> = wordsReadDocuments

        override fun getCompletedDocuments(
            dateRange: DateRange,
            cursor: UUID?,
            limit: Int
        ): DrillDownPage<CompletedDocument> = completedDocuments

        override fun getBacklogDocuments(
            cursor: UUID?,
            limit: Int
        ): DrillDownPage<BacklogDocument> = backlogDocuments

        override fun getBacklogCount(): Int = backlogTotalCount
    }
}
