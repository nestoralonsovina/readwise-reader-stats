# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Readwise Reader Analytics - a Spring Boot 4.0 application that syncs data from the Readwise Reader API and provides reading analytics including reading volume, behavior patterns, content pipeline metrics, and highlight statistics.

## Build Commands

All commands run from `backend/` directory:

```bash
./gradlew build              # Build and run tests
./gradlew test               # Run all tests
./gradlew test --tests "ClassName.methodName"  # Run single test
./gradlew bootRun            # Run application (requires DB)
./gradlew nativeCompile      # Build GraalVM native image
./gradlew nativeTest         # Run tests in native image
./gradlew bootBuildImage     # Build Docker image
```

## Local Development

Start PostgreSQL:
```bash
cd backend && docker compose up -d
```

Run with local profile:
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Database: PostgreSQL 16 on `localhost:5432`, database `readwise_analytics`, user/password: `postgres/postgres`

## Tech Stack

- **Kotlin 2.2** with Spring Boot 4.0
- **Java 21** (configured in gradle.properties)
- **Spring Data JPA** with Hibernate 7.1
- **PostgreSQL** database
- **Spring RestClient** for Readwise API integration
- **GraalVM Native Image** support enabled

## Planned Data Model

Core entities to implement:
- **Document**: Articles, PDFs, EPUBs synced from Readwise (tracks reading_progress, word_count, location)
- **Highlight**: User highlights with notes, colors, favorites
- **Tag**: Document and highlight tagging
- **ReadingProgressSnapshot**: Time-series tracking of reading progress changes
- **LocationChange**: Tracks document flow through pipeline (new � later � archive)
- **DailyStats/DailyStatsByCategory/DailyStatsBySource**: Materialized aggregates
- **SyncLog/SyncCursor**: Incremental sync metadata

## Key Analytics to Support

- Reading volume: words read, articles completed, reading streaks
- Reading behavior: completion rates, peak reading hours, reading velocity
- Content pipeline: backlog size, save-to-read ratio, queue latency
- Highlights: density, color distribution, most highlighted content

## Architecture Philosophy

### Bounded Contexts
Each feature area is a self-contained bounded context with:
- **Domain layer**: Entities, value objects, domain events
- **Application layer**: Use cases, orchestration, port interfaces
- **Infrastructure layer**: External integrations, persistence adapters

### Event-Driven Design
Contexts communicate via domain events (Spring ApplicationEventPublisher):
- Sync context publishes `DocumentSyncedEvent`
- Other contexts subscribe without coupling to sync internals

### Repository Adapters
Application layer defines interfaces (`CursorStore`, `LogStore`). Infrastructure provides implementations (`JpaCursorStore`). This enables:
- Testing with fakes (no database needed)
- Swapping implementations (e.g., Redis cache)

## Current Implementation

### Sync Context (Complete)
- Incremental sync from Readwise Reader API
- Cursor-based pagination with rate limiting (20 req/min)
- Audit logging (SyncLog tracks each run)
- Domain events published for each document

### Pending Contexts
- **Library**: Persist documents, track reading progress
- **Analytics**: Aggregate stats, streaks, insights

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| Event publishing over direct persistence | Sync context doesn't own Document entity; other contexts subscribe |
| Sequence return type for fetchDocuments | Memory-efficient lazy evaluation for large result sets |
| Fake test doubles over mocks | More readable, explicit behavior in tests |
| Store interfaces in application layer | Decouples domain from JPA; enables easy testing |

## Testing Approach

- **Unit tests**: Fakes for all dependencies (no Spring context)
- **Repository tests**: `@DataJpaTest` with H2 in-memory DB
- **TDD**: Tests written before implementation
