package com.reader.analytics.library.application

import com.reader.analytics.library.domain.Document
import com.reader.analytics.library.domain.Highlight
import com.reader.analytics.library.domain.Tag

interface DocumentStore {
    fun findByReadwiseId(readwiseId: String): Document?
    fun save(document: Document): Document
    fun findOrCreateTags(tagNames: List<String>): MutableSet<Tag>

    fun findHighlightByReadwiseId(readwiseId: String): Highlight?
    fun saveHighlight(highlight: Highlight): Highlight
}
