package com.reader.analytics.api

import com.reader.analytics.sync.infrastructure.ReadwiseClient
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/sync")
class SyncController(private val readwiseClient: ReadwiseClient) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun triggerSync() {
        val documents = readwiseClient.fetchDocuments()
        documents.forEach { doc ->
            log.info("Document: id={}, title={}, category={}, progress={}",
                doc.id, doc.title, doc.category, doc.readingProgress)
        }
    }
}