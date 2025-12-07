package com.reader.analytics.api.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Time-series reading statistics response")
data class ReadingStatsResponse(
    @Schema(description = "Time period for the statistics")
    val period: PeriodInfo,

    @Schema(description = "Granularity of the time series", example = "daily", allowableValues = ["daily", "weekly", "monthly"])
    val granularity: String,

    @Schema(description = "List of statistics per time bucket")
    val stats: List<DailyStatsDto>,

    @Schema(description = "Aggregate totals for the entire period")
    val totals: TotalsDto
)

@Schema(description = "Reading statistics for a single time bucket (day/week/month)")
data class DailyStatsDto(
    @Schema(description = "Date of the time bucket (ISO format)", example = "2024-01-15")
    val date: String,

    @Schema(description = "Words read in this time bucket", example = "2500")
    val wordsRead: Long,

    @Schema(description = "Number of articles with any reading progress", example = "8")
    val articlesProgressed: Int,

    @Schema(description = "Number of articles completed (100% progress)", example = "2")
    val articlesCompleted: Int
)

@Schema(description = "Aggregate totals across the entire period")
data class TotalsDto(
    @Schema(description = "Total words read across all time buckets", example = "15420")
    val totalWordsRead: Long,

    @Schema(description = "Total articles completed across all time buckets", example = "12")
    val totalArticlesCompleted: Int
)

@Schema(description = "Reading streak information")
data class StreakResponse(
    @Schema(description = "Current active streak (may be 0 if no recent activity)")
    val current: StreakDto,

    @Schema(description = "Longest streak ever recorded")
    val longest: StreakDto
)

@Schema(description = "Details about a reading streak")
data class StreakDto(
    @Schema(description = "Number of consecutive days", example = "7")
    val days: Int,

    @Schema(description = "Date when the streak started (ISO format)", example = "2024-01-08")
    val startDate: String?,

    @Schema(description = "Date when the streak ended (or today if current)", example = "2024-01-15")
    val endDate: String?
)

@Schema(description = "Peak reading hours analysis response")
data class PeakHoursResponse(
    @Schema(description = "Hourly distribution of reading activity (24 entries, 0-23)")
    val distribution: List<HourlyActivityDto>,

    @Schema(description = "Hour with most reading activity (0-23)", example = "21")
    val peakHour: Int,

    @Schema(description = "Human-readable label for peak hour range", example = "9 PM - 10 PM")
    val peakHourLabel: String,

    @Schema(description = "Percentage of activity during peak hour", example = "15.5")
    val peakPercentage: Double
)

@Schema(description = "Reading activity for a single hour")
data class HourlyActivityDto(
    @Schema(description = "Hour of day (0-23, where 0 is midnight)", example = "21")
    val hour: Int,

    @Schema(description = "Human-readable hour label", example = "9 PM")
    val label: String,

    @Schema(description = "Number of reading sessions in this hour", example = "45")
    val activityCount: Int,

    @Schema(description = "Percentage of total activity in this hour", example = "15.5")
    val percentage: Double
)
