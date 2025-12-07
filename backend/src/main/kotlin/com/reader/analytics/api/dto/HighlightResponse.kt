package com.reader.analytics.api.dto

data class HighlightResponse(
    val summary: HighlightSummaryDto,
    val colorDistribution: List<ColorDto>,
    val topDocuments: List<TopDocumentDto>
)

data class HighlightSummaryDto(
    val total: Int,
    val thisPeriod: Int,
    val averagePerDocument: Double
)

data class ColorDto(
    val color: String,
    val count: Int,
    val percentage: Double
)

data class TopDocumentDto(
    val documentId: String,
    val title: String?,
    val category: String?,
    val highlightCount: Int
)
