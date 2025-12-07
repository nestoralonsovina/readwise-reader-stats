package com.reader.analytics.api

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("local")
@OpenAPIDefinition(
    info = Info(
        title = "Readwise Reader Analytics API",
        version = "1.0.0",
        description = """
            Analytics API for Readwise Reader data. Provides insights into reading habits,
            content pipeline metrics, and highlight statistics.

            ## Features
            - **Reading Analytics**: Track words read, completion rates, and reading streaks
            - **Peak Hours**: Discover when you read most productively
            - **Content Pipeline**: Monitor your reading queue and backlog
            - **Highlights**: Analyze highlighting patterns and top documents

            ## Authentication
            Currently no authentication required (local deployment).
        """,
        contact = Contact(name = "Readwise Reader Analytics")
    ),
    tags = [
        Tag(name = "Analytics", description = "Reading analytics, statistics, and insights"),
        Tag(name = "Sync", description = "Synchronize data from Readwise Reader API")
    ]
)
class OpenApiConfig
