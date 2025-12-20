package com.reader.analytics.analytics.domain.projections

data class HighlightStats(
    val totalHighlights: Int,
    val highlightsThisPeriod: Int,
    val averageHighlightsPerDocument: Double,
    val colorDistribution: Map<String, Int>,
    val mostHighlightedDocuments: List<DocumentHighlightCount>
)

data class DocumentHighlightCount(
    val documentId: String,
    val title: String?,
    val highlightCount: Int,
    val category: String?,
    val imageUrl: String?
)
