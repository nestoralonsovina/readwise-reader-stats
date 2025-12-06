package com.reader.analytics.sync.infrastructure.readwise.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class HighlightDto(
    val id: String,
    val text: String,
    val note: String?,
    val color: String?,
    val location: Int?,
    @JsonProperty("highlighted_at") val highlightedAt: Instant?
)
