package com.reader.analytics.library.domain

import java.time.Instant
import java.util.UUID

data class DocumentDetail(
    val id: UUID,
    val readwiseId: String,
    val url: String,
    val title: String?,
    val author: String?,
    val category: String?,
    val location: String?,
    val readingProgress: Double?,
    val wordCount: Int?,
    val savedAt: Instant?,
    val firstOpenedAt: Instant?,
    val lastOpenedAt: Instant?,
    val imageUrl: String?,
    val tags: List<String>,
    val highlights: List<HighlightDetail>,
    val highlightCount: Int,
    val notesCount: Int,
    val estimatedReadingTimeMinutes: Int
)

data class HighlightDetail(
    val id: UUID,
    val text: String,
    val note: String?,
    val highlightedAt: Instant?
)
