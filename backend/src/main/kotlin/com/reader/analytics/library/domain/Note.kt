package com.reader.analytics.library.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notes")
data class Note(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(unique = true, nullable = false)
    val readwiseId: String,

    @Column(nullable = false)
    val parentId: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    val content: String,

    val createdAt: Instant? = null
)
