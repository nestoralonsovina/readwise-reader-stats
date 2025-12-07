package com.reader.analytics.analytics.domain.projections

import java.time.Duration

data class PipelineStats(
    val backlogSize: Int,
    val inProgressCount: Int,
    val completedCount: Int,
    val archivedCount: Int,
    val saveToReadRatio: Double,
    val averageQueueLatency: Duration?,
    val documentsAddedThisPeriod: Int,
    val documentsCompletedThisPeriod: Int
)

data class LocationBreakdown(
    val location: String,
    val count: Int,
    val percentage: Double
)

data class CategoryBreakdown(
    val category: String,
    val count: Int,
    val averageProgress: Double
)
