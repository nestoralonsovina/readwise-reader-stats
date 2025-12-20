package com.reader.analytics.sync.infrastructure

import com.reader.analytics.sync.infrastructure.readwise.RateLimitEvent
import com.reader.analytics.sync.infrastructure.readwise.RateLimitRetryHandler
import com.reader.analytics.sync.infrastructure.readwise.RetryConfig
import com.reader.analytics.sync.infrastructure.readwise.dto.DocumentDto
import com.reader.analytics.sync.infrastructure.readwise.dto.DocumentListResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

@Component
class ReadwiseWebClient(
    @Value("\${readwise.api.token}") private val token: String,
    @Value("\${readwise.api.retry.max-attempts:3}") private val maxRetryAttempts: Int,
    @Value("\${readwise.api.retry.base-delay-seconds:5}") private val baseDelaySeconds: Long,
    @Value("\${readwise.api.retry.max-delay-seconds:120}") private val maxDelaySeconds: Long
) : ReadwiseClient {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val restClient = RestClient.builder()
        .baseUrl("https://readwise.io")
        .defaultHeader("Authorization", "Token $token")
        .defaultHeader("Content-Type", "application/json")
        .build()

    private val retryHandler = RateLimitRetryHandler(
        RetryConfig(
            maxAttempts = maxRetryAttempts,
            baseDelay = Duration.ofSeconds(baseDelaySeconds),
            maxDelay = Duration.ofSeconds(maxDelaySeconds)
        )
    )

    // ==================== Reader API v3 (Documents) ====================

    override fun fetchDocuments(
        updatedAfter: Instant?,
        onRateLimited: ((RateLimitEvent) -> Unit)?,
        onRateLimitCleared: (() -> Unit)?
    ): Sequence<DocumentDto> = sequence {
        logger.info("Starting document fetch from Readwise API [updatedAfter={}]", updatedAfter)

        var pageCursor: String? = null
        var pageCount = 0
        var totalItems = 0

        do {
            logger.debug("Fetching page [updatedAfter={}, pageCursor={}]", updatedAfter, pageCursor)

            val response = fetchDocumentPage(updatedAfter, pageCursor, onRateLimited, onRateLimitCleared)
            pageCount++
            totalItems += response.results.size

            response.results.forEach { yield(it) }

            pageCursor = response.nextPageCursor

            logger.debug(
                "Received {} items [nextCursor={}]",
                response.results.size, pageCursor
            )
        } while (pageCursor != null)

        logger.info(
            "Completed document fetch [totalItems={}, totalPages={}]",
            totalItems, pageCount
        )
    }

    private fun fetchDocumentPage(
        updatedAfter: Instant?,
        pageCursor: String?,
        onRateLimited: ((RateLimitEvent) -> Unit)?,
        onRateLimitCleared: (() -> Unit)?
    ): DocumentListResponse {
        return retryHandler.executeWithRetry(
            operation = "fetchDocumentPage[cursor=$pageCursor]",
            onRateLimited = onRateLimited,
            onRateLimitCleared = onRateLimitCleared
        ) {
            restClient.get()
                .uri { builder ->
                    builder.path("/api/v3/list/")
                    updatedAfter?.let { builder.queryParam("updatedAfter", it.toIsoString()) }
                    pageCursor?.let { builder.queryParam("pageCursor", it) }
                    builder.build()
                }
                .retrieve()
                .body(DocumentListResponse::class.java)
                ?: run {
                    logger.warn(
                        "Empty response from Readwise API [updatedAfter={}, pageCursor={}]",
                        updatedAfter, pageCursor
                    )
                    throw HttpClientErrorException(HttpStatus.NOT_FOUND, "Empty response from documents endpoint")
                }
        }
    }

    private fun Instant.toIsoString(): String =
        DateTimeFormatter.ISO_INSTANT.format(this)
}