package com.reader.analytics.analytics.domain.projections

import java.time.Instant
import java.util.UUID

data class WordsReadDocument(
    val id: UUID,
    val readwiseId: String,
    val title: String?,
    val author: String?,
    val url: String,
    val imageUrl: String?,
    val category: String?,
    val wordsRead: Long,
    val readingProgress: Double
)

data class CompletedDocument(
    val id: UUID,
    val readwiseId: String,
    val title: String?,
    val author: String?,
    val url: String,
    val imageUrl: String?,
    val category: String?,
    val completedAt: Instant
)

data class BacklogDocument(
    val id: UUID,
    val readwiseId: String,
    val title: String?,
    val author: String?,
    val url: String,
    val imageUrl: String?,
    val category: String?,
    val savedAt: Instant,
    val daysWaiting: Int
)

data class DrillDownPage<T>(
    val items: List<T>,
    val hasMore: Boolean,
    val nextCursor: UUID?
)

data class WordsReadDrillDown(
    val total: Long,
    val changePercent: Double?,
    val documents: DrillDownPage<WordsReadDocument>
)

data class CompletedDrillDown(
    val total: Int,
    val changePercent: Double?,
    val documents: DrillDownPage<CompletedDocument>
)

data class BacklogDrillDown(
    val total: Int,
    val changePercent: Double?,
    val documents: DrillDownPage<BacklogDocument>
)
