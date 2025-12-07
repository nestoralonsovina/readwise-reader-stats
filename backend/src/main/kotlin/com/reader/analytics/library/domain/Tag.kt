package com.reader.analytics.library.domain

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "tags")
data class Tag(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(unique = true, nullable = false)
    val name: String
)
