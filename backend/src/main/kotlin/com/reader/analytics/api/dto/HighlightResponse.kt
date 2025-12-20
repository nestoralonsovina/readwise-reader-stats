package com.reader.analytics.api.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Highlight statistics response")
data class HighlightResponse(
    @Schema(description = "Summary metrics for highlights")
    val summary: HighlightSummaryDto,

    @Schema(description = "Breakdown of highlights by color")
    val colorDistribution: List<ColorDto>,

    @Schema(description = "Documents with the most highlights")
    val topDocuments: List<TopDocumentDto>
)

@Schema(description = "Summary metrics for highlights")
data class HighlightSummaryDto(
    @Schema(description = "Total number of highlights in library", example = "523")
    val total: Int,

    @Schema(description = "Highlights created in the specified period", example = "42")
    val thisPeriod: Int,

    @Schema(description = "Average highlights per document (for documents with at least one)", example = "4.2")
    val averagePerDocument: Double
)

@Schema(description = "Highlight count for a specific color")
data class ColorDto(
    @Schema(
        description = "Highlight color name from Readwise",
        example = "yellow",
        allowableValues = ["yellow", "blue", "green", "orange", "pink", "purple", "red"]
    )
    val color: String,

    @Schema(description = "Number of highlights with this color", example = "156")
    val count: Int,

    @Schema(description = "Percentage of total highlights", example = "29.8")
    val percentage: Double
)

@Schema(description = "Document with highlight count")
data class TopDocumentDto(
    @Schema(description = "Readwise document ID", example = "01ABC234DEF567890")
    val documentId: String,

    @Schema(description = "Document title (may be null for untitled documents)", example = "How to Read a Book")
    val title: String?,

    @Schema(description = "Content category", example = "book")
    val category: String?,

    @Schema(description = "Number of highlights in this document", example = "45")
    val highlightCount: Int,

    @Schema(description = "Document cover or preview image URL", example = "https://images.unsplash.com/photo-123", nullable = true)
    val imageUrl: String?
)
