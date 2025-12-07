package com.reader.analytics.api.dto

data class ReadingStatsResponse(
    val period: PeriodInfo,
    val granularity: String,
    val stats: List<DailyStatsDto>,
    val totals: TotalsDto
)

data class DailyStatsDto(
    val date: String,
    val wordsRead: Long,
    val articlesProgressed: Int,
    val articlesCompleted: Int
)

data class TotalsDto(
    val totalWordsRead: Long,
    val totalArticlesCompleted: Int
)

data class StreakResponse(
    val current: StreakDto,
    val longest: StreakDto
)

data class StreakDto(
    val days: Int,
    val startDate: String?,
    val endDate: String?
)

data class PeakHoursResponse(
    val distribution: List<HourlyActivityDto>,
    val peakHour: Int,
    val peakHourLabel: String,
    val peakPercentage: Double
)

data class HourlyActivityDto(
    val hour: Int,
    val label: String,
    val activityCount: Int,
    val percentage: Double
)
