package com.reader.analytics.api.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Highlight statistics response")
data class HighlightResponse(
    @Schema(description = "Summary metrics for highlights")
    val summary: HighlightSummaryDto,

    @Schema(description = "Documents with the most highlights")
    val topDocuments: List<TopDocumentDto>
)

@Schema(description = "Summary metrics for highlights")
data class HighlightSummaryDto(
    @Schema(description = "Total number of highlights in library", example = "523")
    val total: Int,

    @Schema(description = "Highlights with notes attached", example = "145")
    val withNotes: Int,

    @Schema(description = "Percentage of highlights with notes", example = "27.7")
    val notePercentage: Double,

    @Schema(description = "Highlights created in the specified period", example = "42")
    val thisPeriod: Int,

    @Schema(description = "Highlights in the previous equivalent period", example = "35")
    val previousPeriod: Int,

    @Schema(description = "Change from previous period (thisPeriod - previousPeriod)", example = "7")
    val periodChange: Int,

    @Schema(description = "Percentage change from previous period (null if previous was 0)", example = "20.0")
    val periodChangePercent: Double?,

    @Schema(description = "Average highlights per document (for documents with at least one)", example = "4.2")
    val averagePerDocument: Double
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
    val imageUrl: String?,

    @Schema(description = "Whether any highlight in this document has a note", example = "true")
    val hasNotes: Boolean
)
