package com.reader.analytics.library.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notes")
class Note(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(unique = true, nullable = false)
    val readwiseId: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    var document: Document? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "highlight_id")
    var highlight: Highlight? = null,

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,

    var createdAt: Instant? = null
) {
    init {
        require((document != null) xor (highlight != null)) {
            "Note must have exactly one parent: either document or highlight, not both or neither"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Note) return false
        return readwiseId == other.readwiseId
    }

    override fun hashCode(): Int = readwiseId.hashCode()

    override fun toString(): String =
        "Note(readwiseId='$readwiseId', content='${content.take(50)}...')"
}
