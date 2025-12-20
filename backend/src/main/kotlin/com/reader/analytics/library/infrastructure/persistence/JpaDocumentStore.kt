package com.reader.analytics.library.infrastructure.persistence

import com.reader.analytics.library.application.DocumentStore
import com.reader.analytics.library.domain.Document
import com.reader.analytics.library.domain.Highlight
import com.reader.analytics.library.domain.Note
import com.reader.analytics.library.domain.Tag
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Component
class JpaDocumentStore(
    private val documentRepository: DocumentRepository,
    private val tagRepository: TagRepository,
    private val highlightRepository: HighlightRepository,
    private val noteRepository: NoteRepository
) : DocumentStore {

    override fun findByReadwiseId(readwiseId: String): Document? =
        documentRepository.findByReadwiseId(readwiseId)

    override fun findById(id: UUID): Document? =
        documentRepository.findById(id).getOrNull()

    override fun save(document: Document): Document =
        documentRepository.save(document)

    @Transactional
    override fun findOrCreateTags(tagNames: List<String>): MutableSet<Tag> {
        if (tagNames.isEmpty()) return mutableSetOf()

        val existingTags = tagRepository.findByNameIn(tagNames)
        val existingNames = existingTags.map { it.name }.toSet()
        val newTags = tagNames
            .filter { it !in existingNames }
            .map { Tag(name = it) }
            .let { tagRepository.saveAll(it) }

        return (existingTags + newTags).toMutableSet()
    }

    override fun findHighlightByReadwiseId(readwiseId: String): Highlight? =
        highlightRepository.findByReadwiseId(readwiseId)

    override fun saveHighlight(highlight: Highlight): Highlight =
        highlightRepository.save(highlight)

    override fun findNoteByReadwiseId(readwiseId: String): Note? =
        noteRepository.findByReadwiseId(readwiseId)

    override fun saveNote(note: Note): Note =
        noteRepository.save(note)
}
