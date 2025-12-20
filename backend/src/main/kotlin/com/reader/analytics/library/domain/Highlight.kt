package com.reader.analytics.library.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "highlights")
class Highlight(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(unique = true, nullable = false)
    val readwiseId: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    var document: Document,

    @Column(columnDefinition = "TEXT", nullable = false)
    var text: String,

    var highlightedAt: Instant? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Highlight) return false
        return readwiseId == other.readwiseId
    }

    override fun hashCode(): Int = readwiseId.hashCode()

    override fun toString(): String =
        "Highlight(readwiseId='$readwiseId', text='${text.take(50)}...')"
}
