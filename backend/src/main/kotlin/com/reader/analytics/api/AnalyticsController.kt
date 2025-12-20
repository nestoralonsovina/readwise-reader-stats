package com.reader.analytics.api

import com.reader.analytics.analytics.application.AnalyticsService
import com.reader.analytics.analytics.domain.DateRange
import com.reader.analytics.analytics.domain.Granularity
import com.reader.analytics.api.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics")
class AnalyticsController(
    private val analyticsService: AnalyticsService
) {

    @GetMapping("/dashboard")
    @Operation(
        summary = "Get dashboard summary",
        description = """
            Returns all key metrics in a single call for dashboard display.

            Includes:
            - Words read and articles completed in the period
            - Current reading streak
            - Backlog size (documents waiting to be read)
            - Highlights created in the period
            - Completion rate percentage
        """
    )
    @ApiResponse(
        responseCode = "200",
        description = "Dashboard metrics retrieved successfully",
        content = [Content(schema = Schema(implementation = DashboardResponse::class))]
    )
    fun getDashboard(
        @Parameter(description = "Number of days to include in the period", example = "7")
        @RequestParam(defaultValue = "7") days: Int
    ): DashboardResponse {
        val dateRange = DateRange.lastNDays(days)
        val summary = analyticsService.getDashboardSummary(dateRange)

        return DashboardResponse(
            period = dateRange.toPeriodInfo("Last $days days"),
            summary = SummaryDto(
                wordsRead = summary.totalWordsReadThisPeriod,
                articlesCompleted = summary.articlesCompletedThisPeriod,
                currentStreak = summary.currentStreak,
                backlogSize = summary.backlogSize,
                highlightsCreated = summary.highlightsThisPeriod
            ),
            quickStats = QuickStatsDto(
                completionRate = summary.completionRate
            )
        )
    }

    @GetMapping("/reading/stats")
    @Operation(
        summary = "Get reading statistics time-series",
        description = """
            Returns reading statistics grouped by time period.

            Use this endpoint to build charts showing reading progress over time.
            Supports daily, weekly, and monthly granularity.

            If no date range is provided, defaults to the last 7 days.
        """
    )
    @ApiResponse(
        responseCode = "200",
        description = "Reading statistics retrieved successfully",
        content = [Content(schema = Schema(implementation = ReadingStatsResponse::class))]
    )
    fun getReadingStats(
        @Parameter(description = "Start date (ISO format: YYYY-MM-DD)", example = "2024-01-01")
        @RequestParam(required = false) startDate: String?,

        @Parameter(description = "End date (ISO format: YYYY-MM-DD)", example = "2024-01-31")
        @RequestParam(required = false) endDate: String?,

        @Parameter(
            description = "Time grouping granularity",
            schema = Schema(allowableValues = ["DAILY", "WEEKLY", "MONTHLY"])
        )
        @RequestParam(defaultValue = "DAILY") granularity: String
    ): ReadingStatsResponse {
        val dateRange = parseDateRange(startDate, endDate)
        val gran = Granularity.valueOf(granularity.uppercase())
        val stats = analyticsService.getReadingStats(dateRange, gran)

        return ReadingStatsResponse(
            period = dateRange.toPeriodInfo(),
            granularity = gran.name.lowercase(),
            stats = stats.map {
                DailyStatsDto(
                    date = it.date.toString(),
                    wordsRead = it.wordsRead,
                    articlesProgressed = it.articlesProgressed,
                    articlesCompleted = it.articlesCompleted
                )
            },
            totals = TotalsDto(
                totalWordsRead = stats.sumOf { it.wordsRead },
                totalArticlesCompleted = stats.sumOf { it.articlesCompleted }
            )
        )
    }

    @GetMapping("/reading/streak")
    @Operation(
        summary = "Get reading streak information",
        description = """
            Returns current and longest reading streak.

            A streak is defined as consecutive days with reading activity
            (any progress recorded on a document).
        """
    )
    @ApiResponse(
        responseCode = "200",
        description = "Streak information retrieved successfully",
        content = [Content(schema = Schema(implementation = StreakResponse::class))]
    )
    fun getReadingStreak(): StreakResponse {
        val streak = analyticsService.getReadingStreak()
        return StreakResponse(
            current = StreakDto(
                days = streak.currentStreak,
                startDate = streak.currentStreakStartDate?.toString(),
                endDate = LocalDate.now().toString()
            ),
            longest = StreakDto(
                days = streak.longestStreak,
                startDate = streak.longestStreakStartDate?.toString(),
                endDate = streak.longestStreakEndDate?.toString()
            )
        )
    }

    @GetMapping("/reading/peak-hours")
    @Operation(
        summary = "Get peak reading hours",
        description = """
            Returns hourly distribution of reading activity.

            Shows which hours of the day have the most reading activity,
            useful for understanding reading patterns and productivity.
        """
    )
    @ApiResponse(
        responseCode = "200",
        description = "Peak hours data retrieved successfully",
        content = [Content(schema = Schema(implementation = PeakHoursResponse::class))]
    )
    fun getPeakReadingHours(
        @Parameter(description = "Number of days to analyze", example = "30")
        @RequestParam(defaultValue = "30") days: Int
    ): PeakHoursResponse {
        val dateRange = DateRange.lastNDays(days)
        val peak = analyticsService.getPeakReadingHours(dateRange)
        val total = peak.hourlyDistribution.values.sum()

        return PeakHoursResponse(
            distribution = peak.hourlyDistribution.map { (hour, count) ->
                HourlyActivityDto(
                    hour = hour,
                    label = formatHour(hour),
                    activityCount = count,
                    percentage = if (total > 0) (count.toDouble() / total) * 100 else 0.0
                )
            }.sortedBy { it.hour },
            peakHour = peak.peakHour,
            peakHourLabel = formatHourRange(peak.peakHour),
            peakPercentage = peak.peakHourPercentage
        )
    }

    @GetMapping("/pipeline")
    @Operation(
        summary = "Get content pipeline statistics",
        description = """
            Returns metrics about your reading queue and content flow.

            Includes:
            - Current state: backlog, in-progress, completed, archived counts
            - Period metrics: documents added, completed, save-to-read ratio
            - Breakdown by location (new, later, shortlist, archive, feed)
            - Breakdown by category (article, book, pdf, etc.)
        """
    )
    @ApiResponse(
        responseCode = "200",
        description = "Pipeline statistics retrieved successfully",
        content = [Content(schema = Schema(implementation = PipelineResponse::class))]
    )
    fun getPipelineStats(
        @Parameter(description = "Number of days for period metrics", example = "7")
        @RequestParam(defaultValue = "7") days: Int
    ): PipelineResponse {
        val dateRange = DateRange.lastNDays(days)
        val pipeline = analyticsService.getPipelineStats(dateRange)
        val locations = analyticsService.getLocationBreakdown()
        val categories = analyticsService.getCategoryBreakdown()

        return PipelineResponse(
            current = CurrentPipelineDto(
                backlog = pipeline.backlogSize,
                inProgress = pipeline.inProgressCount,
                completed = pipeline.completedCount,
                archived = pipeline.archivedCount,
                total = pipeline.backlogSize + pipeline.inProgressCount +
                        pipeline.completedCount + pipeline.archivedCount
            ),
            period = PipelinePeriodDto(
                documentsAdded = pipeline.documentsAddedThisPeriod,
                documentsCompleted = pipeline.documentsCompletedThisPeriod,
                saveToReadRatio = pipeline.saveToReadRatio,
                averageQueueLatencyHours = pipeline.averageQueueLatency?.toHours()?.toDouble()
            ),
            breakdown = BreakdownDto(
                byLocation = locations.map {
                    LocationDto(
                        location = it.location,
                        count = it.count,
                        percentage = it.percentage
                    )
                },
                byCategory = categories.map {
                    CategoryDto(
                        category = it.category,
                        count = it.count,
                        averageProgress = it.averageProgress,
                        averageProgressPercent = "${(it.averageProgress * 100).toInt()}%"
                    )
                }
            )
        )
    }

    @GetMapping("/highlights")
    @Operation(
        summary = "Get highlight statistics",
        description = """
            Returns statistics about highlights across your library.

            Includes:
            - Summary: total highlights, highlights this period, average per document
            - Color distribution: breakdown by highlight color
            - Top documents: most highlighted documents
        """
    )
    @ApiResponse(
        responseCode = "200",
        description = "Highlight statistics retrieved successfully",
        content = [Content(schema = Schema(implementation = HighlightResponse::class))]
    )
    fun getHighlightStats(
        @Parameter(description = "Number of days for period metrics", example = "30")
        @RequestParam(defaultValue = "30") days: Int,

        @Parameter(description = "Maximum number of top documents to return", example = "10")
        @RequestParam(defaultValue = "10") topDocumentsLimit: Int
    ): HighlightResponse {
        val dateRange = DateRange.lastNDays(days)
        val stats = analyticsService.getHighlightStats(dateRange)

        return HighlightResponse(
            summary = HighlightSummaryDto(
                total = stats.totalHighlights,
                withNotes = stats.highlightsWithNotes,
                notePercentage = stats.notePercentage,
                thisPeriod = stats.highlightsThisPeriod,
                previousPeriod = stats.highlightsPreviousPeriod,
                periodChange = stats.periodChange,
                periodChangePercent = stats.periodChangePercent,
                averagePerDocument = stats.averageHighlightsPerDocument
            ),
            topDocuments = stats.mostHighlightedDocuments.take(topDocumentsLimit).map {
                TopDocumentDto(
                    documentId = it.documentId,
                    title = it.title,
                    category = it.category,
                    highlightCount = it.highlightCount,
                    imageUrl = it.imageUrl,
                    hasNotes = it.hasNotes
                )
            }
        )
    }

    private fun parseDateRange(startDate: String?, endDate: String?): DateRange {
        return if (startDate != null && endDate != null) {
            DateRange(
                LocalDate.parse(startDate),
                LocalDate.parse(endDate)
            )
        } else {
            DateRange.lastWeek()
        }
    }

    private fun DateRange.toPeriodInfo(label: String? = null): PeriodInfo = PeriodInfo(
        startDate = start.toString(),
        endDate = end.toString(),
        label = label ?: "Custom range"
    )

    private fun formatHour(hour: Int): String {
        val suffix = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$displayHour $suffix"
    }

    private fun formatHourRange(hour: Int): String {
        val nextHour = (hour + 1) % 24
        return "${formatHour(hour)} - ${formatHour(nextHour)}"
    }
}
