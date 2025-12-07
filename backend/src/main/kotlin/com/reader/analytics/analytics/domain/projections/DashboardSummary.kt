package com.reader.analytics.analytics.domain.projections

data class DashboardSummary(
    val totalWordsReadThisPeriod: Long,
    val articlesCompletedThisPeriod: Int,
    val currentStreak: Int,
    val backlogSize: Int,
    val highlightsThisPeriod: Int,
    val completionRate: Double
)
