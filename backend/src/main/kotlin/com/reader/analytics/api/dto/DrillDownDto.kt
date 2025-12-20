package com.reader.analytics.api.dto

data class DrillDownSummaryDto(
    val total: Long,
    val changePercent: Double?
)

data class WordsReadDocumentDto(
    val id: String,
    val title: String?,
    val author: String?,
    val source: String,
    val coverUrl: String?,
    val category: String?,
    val wordsRead: Long,
    val readingProgress: Int
)

data class CompletedDocumentDto(
    val id: String,
    val title: String?,
    val author: String?,
    val source: String,
    val coverUrl: String?,
    val category: String?,
    val completedAt: String
)

data class BacklogDocumentDto(
    val id: String,
    val title: String?,
    val author: String?,
    val source: String,
    val coverUrl: String?,
    val category: String?,
    val savedAt: String,
    val daysWaiting: Int
)

data class WordsReadDrillDownResponse(
    val summary: DrillDownSummaryDto,
    val documents: List<WordsReadDocumentDto>,
    val hasMore: Boolean,
    val nextCursor: String?
)

data class CompletedDrillDownResponse(
    val summary: DrillDownSummaryDto,
    val documents: List<CompletedDocumentDto>,
    val hasMore: Boolean,
    val nextCursor: String?
)

data class BacklogDrillDownResponse(
    val summary: DrillDownSummaryDto,
    val documents: List<BacklogDocumentDto>,
    val hasMore: Boolean,
    val nextCursor: String?
)
