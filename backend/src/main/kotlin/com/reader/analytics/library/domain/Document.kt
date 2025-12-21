package com.reader.analytics.library.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "documents")
data class Document(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(unique = true, nullable = false)
    val readwiseId: String,

    @Column(nullable = false)
    val url: String,

    val title: String? = null,
    val author: String? = null,
    val category: String? = null,
    val location: String? = null,
    val wordCount: Int? = null,
    val savedAt: Instant? = null,
    val updatedAt: Instant? = null,
    val firstOpenedAt: Instant? = null,
    val lastOpenedAt: Instant? = null,
    val parentId: String? = null,

    @Column(length = 2048)
    val imageUrl: String? = null,

    @ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.EAGER)
    @JoinTable(
        name = "document_tags",
        joinColumns = [JoinColumn(name = "document_id")],
        inverseJoinColumns = [JoinColumn(name = "tag_id")]
    )
    val tags: MutableSet<Tag> = mutableSetOf()
)
