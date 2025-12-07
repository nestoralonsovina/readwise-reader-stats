package com.reader.analytics.analytics.domain.projections

data class PeakReadingHours(
    val hourlyDistribution: Map<Int, Int>,
    val peakHour: Int,
    val peakHourPercentage: Double
)

data class HourlyActivity(
    val hour: Int,
    val activityCount: Int
)
