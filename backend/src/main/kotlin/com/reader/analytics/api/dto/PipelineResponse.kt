package com.reader.analytics.api.dto

data class PipelineResponse(
    val current: CurrentPipelineDto,
    val period: PipelinePeriodDto,
    val breakdown: BreakdownDto
)

data class CurrentPipelineDto(
    val backlog: Int,
    val inProgress: Int,
    val completed: Int,
    val archived: Int,
    val total: Int
)

data class PipelinePeriodDto(
    val documentsAdded: Int,
    val documentsCompleted: Int,
    val saveToReadRatio: Double,
    val averageQueueLatencyHours: Double?
)

data class BreakdownDto(
    val byLocation: List<LocationDto>,
    val byCategory: List<CategoryDto>
)

data class LocationDto(
    val location: String,
    val count: Int,
    val percentage: Double
)

data class CategoryDto(
    val category: String,
    val count: Int,
    val averageProgress: Double,
    val averageProgressPercent: String
)
