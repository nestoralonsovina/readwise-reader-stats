package com.reader.analytics.api.dto

data class DashboardResponse(
    val period: PeriodInfo,
    val summary: SummaryDto,
    val quickStats: QuickStatsDto
)

data class PeriodInfo(
    val startDate: String,
    val endDate: String,
    val label: String
)

data class SummaryDto(
    val wordsRead: Long,
    val articlesCompleted: Int,
    val currentStreak: Int,
    val backlogSize: Int,
    val highlightsCreated: Int
)

data class QuickStatsDto(
    val completionRate: Double
)
