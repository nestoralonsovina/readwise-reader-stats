package com.reader.analytics.api.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Dashboard summary response containing all key metrics")
data class DashboardResponse(
    @Schema(description = "Time period for the metrics")
    val period: PeriodInfo,

    @Schema(description = "Summary metrics for the dashboard cards")
    val summary: SummaryDto,

    @Schema(description = "Additional quick statistics")
    val quickStats: QuickStatsDto
)

@Schema(description = "Information about the query time period")
data class PeriodInfo(
    @Schema(description = "Start date of the period (ISO format)", example = "2024-01-01")
    val startDate: String,

    @Schema(description = "End date of the period (ISO format)", example = "2024-01-07")
    val endDate: String,

    @Schema(description = "Human-readable label for the period", example = "Last 7 days")
    val label: String
)

@Schema(description = "Summary metrics for the dashboard")
data class SummaryDto(
    @Schema(description = "Total words read in the period (calculated from reading progress)", example = "15420")
    val wordsRead: Long,

    @Schema(description = "Number of articles completed (reached 100% progress)", example = "5")
    val articlesCompleted: Int,

    @Schema(description = "Current consecutive reading streak in days", example = "7")
    val currentStreak: Int,

    @Schema(description = "Documents waiting to be read (new + later locations)", example = "42")
    val backlogSize: Int,

    @Schema(description = "Number of highlights created in the period", example = "23")
    val highlightsCreated: Int
)

@Schema(description = "Quick statistics for at-a-glance metrics")
data class QuickStatsDto(
    @Schema(description = "Percentage of started documents that were completed (0.0 to 1.0)", example = "0.35")
    val completionRate: Double
)
