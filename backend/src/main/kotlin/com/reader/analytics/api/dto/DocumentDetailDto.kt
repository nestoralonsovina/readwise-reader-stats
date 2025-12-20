package com.reader.analytics.api.dto

data class DocumentDetailResponse(
    val id: String,
    val readwiseId: String,
    val title: String?,
    val author: String?,
    val sourceUrl: String,
    val source: String,
    val coverUrl: String?,
    val category: String?,
    val location: String?,
    val wordCount: Int?,
    val readingProgress: Int,
    val savedAt: String?,
    val firstOpenedAt: String?,
    val lastOpenedAt: String?,
    val tags: List<String>,
    val highlights: List<HighlightDto>,
    val stats: DocumentStatsDto
)

data class HighlightDto(
    val id: String,
    val text: String,
    val note: String?,
    val createdAt: String?
)

data class DocumentStatsDto(
    val highlightCount: Int,
    val notesCount: Int,
    val estimatedReadingTime: Int
)
