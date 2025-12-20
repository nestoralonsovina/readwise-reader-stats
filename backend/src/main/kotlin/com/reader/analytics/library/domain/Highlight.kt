package com.reader.analytics.library.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "highlights")
data class Highlight(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(unique = true, nullable = false)
    val readwiseId: String,

    @Column(name = "document_readwise_id", nullable = false)
    val documentReadwiseId: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    val text: String,

    @Column(columnDefinition = "TEXT")
    val note: String? = null,

    val highlightedAt: Instant? = null
)
