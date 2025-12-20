package com.reader.analytics.sync.infrastructure.readwise.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.reader.analytics.sync.domain.events.HighlightSyncedEvent
import java.time.Instant
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class DocumentListResponse(
    val count: Int,
    val nextPageCursor: String?,
    val results: List<DocumentDto>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DocumentDto(
    val id: String,
    val url: String,
    @JsonProperty("source_url") val sourceUrl: String?,
    val title: String?,
    val author: String?,
    val source: String?,
    val category: String?,
    val location: String?,
    val tags: Map<String, Any>?,  // API returns {} or {"tag-key": {...}}
    @JsonProperty("site_name") val siteName: String?,
    @JsonProperty("word_count") val wordCount: Int?,
    @JsonProperty("reading_progress") val readingProgress: Double?,
    @JsonProperty("published_date") val publishedDate: LocalDate?,
    @JsonProperty("saved_at") val savedAt: Instant?,
    @JsonProperty("created_at") val createdAt: Instant?,
    @JsonProperty("updated_at") val updatedAt: Instant?,
    @JsonProperty("first_opened_at") val firstOpenedAt: Instant?,
    @JsonProperty("last_opened_at") val lastOpenedAt: Instant?,
    @JsonProperty("parent_id") val parentId: String?,
    val summary: String?,
    val notes: String?,
    val content: String?,
    @JsonProperty("image_url") val imageUrl: String?
) {
    fun tagKeys(): List<String> = tags?.keys?.toList() ?: emptyList()
}

fun DocumentDto.isHighlight(): Boolean = category == "highlight"

fun DocumentDto.isDocument(): Boolean = category != "highlight" && category != "note"

fun DocumentDto.toHighlightEvent(): HighlightSyncedEvent {
    requireNotNull(parentId) { "Highlight must have parentId" }
    return HighlightSyncedEvent(
        id = id,
        documentId = parentId,
        text = content ?: "",
        note = notes,
        highlightedAt = savedAt ?: createdAt
    )
}