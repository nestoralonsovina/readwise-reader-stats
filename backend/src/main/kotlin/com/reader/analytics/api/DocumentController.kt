package com.reader.analytics.api

import com.reader.analytics.api.dto.*
import com.reader.analytics.library.application.DocumentService
import com.reader.analytics.shared.UrlUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents")
class DocumentController(
    private val documentService: DocumentService
) {

    @GetMapping("/{id}")
    @Operation(
        summary = "Get document detail",
        description = "Returns detailed information about a document including highlights, notes, and reading stats."
    )
    @ApiResponse(responseCode = "200", description = "Document details retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Document not found")
    fun getDocumentDetail(@PathVariable id: UUID): DocumentDetailResponse {
        val detail = documentService.getDocumentDetail(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found")

        return DocumentDetailResponse(
            id = detail.id.toString(),
            readwiseId = detail.readwiseId,
            title = detail.title,
            author = detail.author,
            sourceUrl = detail.url,
            source = UrlUtils.extractDomain(detail.url),
            coverUrl = detail.imageUrl,
            category = detail.category,
            location = detail.location,
            wordCount = detail.wordCount,
            readingProgress = ((detail.readingProgress ?: 0.0) * 100).toInt(),
            savedAt = detail.savedAt?.toString(),
            firstOpenedAt = detail.firstOpenedAt?.toString(),
            lastOpenedAt = detail.lastOpenedAt?.toString(),
            tags = detail.tags,
            highlights = detail.highlights.map { hl ->
                HighlightDto(
                    id = hl.id.toString(),
                    text = hl.text,
                    note = hl.note,
                    createdAt = hl.highlightedAt?.toString()
                )
            },
            stats = DocumentStatsDto(
                highlightCount = detail.highlightCount,
                notesCount = detail.notesCount,
                estimatedReadingTime = detail.estimatedReadingTimeMinutes
            )
        )
    }
}
