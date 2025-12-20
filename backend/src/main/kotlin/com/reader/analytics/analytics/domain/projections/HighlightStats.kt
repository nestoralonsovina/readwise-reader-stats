package com.reader.analytics.analytics.domain.projections

data class HighlightStats(
    val totalHighlights: Int,
    val highlightsWithNotes: Int,
    val highlightsThisPeriod: Int,
    val highlightsPreviousPeriod: Int,
    val averageHighlightsPerDocument: Double,
    val mostHighlightedDocuments: List<DocumentHighlightCount>
) {
    val notePercentage: Double
        get() = if (totalHighlights == 0) 0.0
                else (highlightsWithNotes.toDouble() / totalHighlights) * 100

    val periodChange: Int
        get() = highlightsThisPeriod - highlightsPreviousPeriod

    val periodChangePercent: Double?
        get() = if (highlightsPreviousPeriod == 0) null
                else ((highlightsThisPeriod - highlightsPreviousPeriod).toDouble() / highlightsPreviousPeriod) * 100
}

data class DocumentHighlightCount(
    val documentId: String,
    val title: String?,
    val highlightCount: Int,
    val category: String?,
    val imageUrl: String?,
    val hasNotes: Boolean
)
