package com.reader.analytics.shared

import java.net.URI

object UrlUtils {

    fun extractDomain(url: String): String {
        if (url.isEmpty()) return url

        return try {
            val uri = URI(url)
            val host = uri.host ?: return url
            host.removePrefix("www.")
        } catch (e: Exception) {
            url
        }
    }
}
