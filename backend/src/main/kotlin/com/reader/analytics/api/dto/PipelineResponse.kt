package com.reader.analytics.api.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Content pipeline statistics response")
data class PipelineResponse(
    @Schema(description = "Current state of documents across pipeline stages")
    val current: CurrentPipelineDto,

    @Schema(description = "Period-specific pipeline metrics")
    val period: PipelinePeriodDto,

    @Schema(description = "Breakdown by location and category")
    val breakdown: BreakdownDto
)

@Schema(description = "Current document counts by pipeline stage")
data class CurrentPipelineDto(
    @Schema(description = "Documents in backlog (new + later locations, not started)", example = "42")
    val backlog: Int,

    @Schema(description = "Documents currently being read (0% < progress < 100%)", example = "15")
    val inProgress: Int,

    @Schema(description = "Documents completed (100% progress)", example = "128")
    val completed: Int,

    @Schema(description = "Documents in archive location", example = "89")
    val archived: Int,

    @Schema(description = "Total documents across all stages", example = "274")
    val total: Int
)

@Schema(description = "Pipeline metrics for the specified period")
data class PipelinePeriodDto(
    @Schema(description = "New documents added in the period", example = "12")
    val documentsAdded: Int,

    @Schema(description = "Documents completed in the period", example = "8")
    val documentsCompleted: Int,

    @Schema(
        description = "Ratio of documents saved to documents read (< 1 means reading faster than saving)",
        example = "1.5"
    )
    val saveToReadRatio: Double,

    @Schema(
        description = "Average time from save to first read (null if no data)",
        example = "72.5"
    )
    val averageQueueLatencyHours: Double?
)

@Schema(description = "Document breakdown by location and category")
data class BreakdownDto(
    @Schema(description = "Documents grouped by Readwise location")
    val byLocation: List<LocationDto>,

    @Schema(description = "Documents grouped by content category")
    val byCategory: List<CategoryDto>
)

@Schema(description = "Document count for a specific location")
data class LocationDto(
    @Schema(
        description = "Readwise Reader location",
        example = "later",
        allowableValues = ["new", "later", "shortlist", "archive", "feed"]
    )
    val location: String,

    @Schema(description = "Number of documents in this location", example = "25")
    val count: Int,

    @Schema(description = "Percentage of total documents", example = "18.5")
    val percentage: Double
)

@Schema(description = "Document statistics for a specific category")
data class CategoryDto(
    @Schema(
        description = "Content category from Readwise",
        example = "article",
        allowableValues = ["article", "book", "pdf", "epub", "tweet", "video", "podcast", "email", "rss", "supplemental"]
    )
    val category: String,

    @Schema(description = "Number of documents in this category", example = "45")
    val count: Int,

    @Schema(description = "Average reading progress for this category (0.0 to 1.0)", example = "0.42")
    val averageProgress: Double,

    @Schema(description = "Human-readable average progress percentage", example = "42%")
    val averageProgressPercent: String
)
