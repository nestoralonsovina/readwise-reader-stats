package com.reader.analytics.api

import com.reader.analytics.analytics.application.AnalyticsService
import com.reader.analytics.analytics.domain.DateRange
import com.reader.analytics.analytics.domain.Granularity
import com.reader.analytics.api.dto.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/analytics")
class AnalyticsController(
    private val analyticsService: AnalyticsService
) {

    @GetMapping("/dashboard")
    fun getDashboard(
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
    fun getReadingStats(
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
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
    fun getPeakReadingHours(
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
    fun getPipelineStats(
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
    fun getHighlightStats(
        @RequestParam(defaultValue = "30") days: Int,
        @RequestParam(defaultValue = "10") topDocumentsLimit: Int
    ): HighlightResponse {
        val dateRange = DateRange.lastNDays(days)
        val stats = analyticsService.getHighlightStats(dateRange)
        val total = stats.colorDistribution.values.sum()

        return HighlightResponse(
            summary = HighlightSummaryDto(
                total = stats.totalHighlights,
                thisPeriod = stats.highlightsThisPeriod,
                averagePerDocument = stats.averageHighlightsPerDocument
            ),
            colorDistribution = stats.colorDistribution.map { (color, count) ->
                ColorDto(
                    color = color,
                    count = count,
                    percentage = if (total > 0) (count.toDouble() / total) * 100 else 0.0
                )
            }.sortedByDescending { it.count },
            topDocuments = stats.mostHighlightedDocuments.take(topDocumentsLimit).map {
                TopDocumentDto(
                    documentId = it.documentId,
                    title = it.title,
                    category = it.category,
                    highlightCount = it.highlightCount
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
