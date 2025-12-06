package com.reader.analytics.sync.infrastructure

import com.reader.analytics.sync.infrastructure.readwise.RateLimiter
import com.reader.analytics.sync.infrastructure.readwise.dto.DocumentDto
import com.reader.analytics.sync.infrastructure.readwise.dto.DocumentListResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.Instant
import java.time.format.DateTimeFormatter

@Component
class ReadwiseWebClient(
    @Value("\${readwise.api.token}") private val token: String,
    @Value("\${readwise.api.rate-limit-per-minute:20}") private val rateLimitPerMinute: Int
) : ReadwiseClient {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val restClient = RestClient.builder()
        .baseUrl("https://readwise.io")
        .defaultHeader("Authorization", "Token $token")
        .defaultHeader("Content-Type", "application/json")
        .build()

    private val rateLimiter = RateLimiter(rateLimitPerMinute)

    // ==================== Reader API v3 (Documents) ====================

    override fun fetchDocuments(updatedAfter: Instant?): Sequence<DocumentDto> = sequence {
        var pageCursor: String? = null

        do {
            rateLimiter.acquire()

            val response = fetchDocumentPage(updatedAfter, pageCursor)

            response.results
                .filter { it.category != "highlight" && it.category != "note" } // Skip child docs
                .forEach { yield(it) }

            pageCursor = response.nextPageCursor

            logger.debug("Fetched ${response.results.size} documents, next cursor: $pageCursor")
        } while (pageCursor != null)
    }

    private fun fetchDocumentPage(updatedAfter: Instant?, pageCursor: String?): DocumentListResponse {
        return try {
            restClient.get()
                .uri { builder ->
                    builder.path("/api/v3/list/")
                    updatedAfter?.let { builder.queryParam("updatedAfter", it.toIsoString()) }
                    pageCursor?.let { builder.queryParam("pageCursor", it) }
                    builder.build()
                }
                .retrieve()
                .body(DocumentListResponse::class.java)
                ?: throw HttpClientErrorException(HttpStatus.NOT_FOUND, "Empty response from documents endpoint")
        } catch (e: RestClientResponseException) {
            throw HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, e.responseBodyAsString)
        }
    }

    private fun Instant.toIsoString(): String =
        DateTimeFormatter.ISO_INSTANT.format(this)
}