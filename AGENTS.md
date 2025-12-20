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

## Readwise APIs

This project integrates with two Readwise APIs:

### Reader API (v3)
Documents and articles from the Reader app.

- **Documentation:** https://readwise.io/reader_api
- **Base URL:** `https://readwise.io/api/v3/`
- **Rate Limit:** 20 req/min (50 for create/update)

| Endpoint | Method | Path |
|----------|--------|------|
| Create Document | POST | `/save/` |
| List Documents | GET | `/list/` |
| Update Document | PATCH | `/update/{id}/` |
| Delete Document | DELETE | `/delete/{id}/` |
| List Tags | GET | `/tags/` |

### Highlights API (v2)
Highlights from Kindle, books, articles, and other sources.

- **Documentation:** https://readwise.io/api_deets
- **Base URL:** `https://readwise.io/api/v2/`
- **Rate Limit:** 240 req/min (20 for list endpoints)

| Endpoint | Method | Path |
|----------|--------|------|
| Create Highlights | POST | `/highlights/` |
| List Highlights | GET | `/highlights/` |
| Export Highlights | GET | `/export/` |
| List Books | GET | `/books/` |
| Daily Review | GET | `/review/` |
| Highlight/Book Tags | CRUD | `/{type}/{id}/tags/` |

### Authentication
All endpoints require: `Authorization: Token {ACCESS_TOKEN}`

### API Testing
Bruno collection available at `bruno/readwise/` with requests for all endpoints.

## Data Model

### Implemented Entities
- **Document**: Articles, PDFs, EPUBs synced from Readwise (tracks reading_progress, word_count, location)
- **Highlight**: User highlights with notes, colors, favorites
- **Tag**: Document and highlight tagging
- **ReadingProgressSnapshot**: Time-series tracking of reading progress changes (documentId, readingProgress, wordCount, firstOpenedAt, lastOpenedAt, recordedAt)
- **LocationChange**: Tracks document flow through pipeline (documentId, fromLocation, toLocation, changedAt, category)
- **SyncLog/SyncCursor**: Incremental sync metadata

### Planned Entities
- **DailyStats/DailyStatsByCategory/DailyStatsBySource**: Materialized aggregates

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

### Library Context (Complete)
- Subscribes to `DocumentSyncedEvent` and `HighlightSyncedEvent`
- Persists **Document** entity (UUID PK, Readwise ID as unique field)
- Persists **Highlight** entity (ManyToOne with Document)
- Persists **Tag** entity (ManyToMany with Document, normalized)
- Upsert semantics: creates new or updates existing based on Readwise ID
- Category/Location stored as raw strings (multi-tenant ready)

### Tracking Context (Complete)
- Subscribes to `DocumentSyncedEvent`
- **ReadingProgressSnapshot**: Records reading progress changes over time
  - Only creates snapshot when `readingProgress` actually changes
  - Captures `firstOpenedAt`, `lastOpenedAt` for peak hours analysis
  - Enables: words read, reading velocity, completion events, streaks
- **LocationChange**: Records document pipeline transitions
  - Tracks moves between locations (new → later → shortlist → archive)
  - Captures `fromLocation`, `toLocation`, `category`
  - Enables: queue latency, save-to-read ratio, backlog size
- Uses `TrackingStore` interface with `JpaTrackingStore` adapter
- 6 unit tests with `FakeTrackingStore`

### Analytics Context (Complete)
- Query-time computation using native PostgreSQL queries via JdbcTemplate
- **Domain layer**: `DateRange` value object, `Granularity` enum, projection data classes
- **Application layer**: `AnalyticsStore` port interface, `AnalyticsService` facade
- **Infrastructure layer**: `AnalyticsRepository` with native SQL, `JpaAnalyticsStore` adapter
- **API layer**: `AnalyticsController` with REST endpoints, response DTOs

**Key Metrics Provided:**
- Reading volume: words read (calculated from progress deltas), articles completed
- Reading behavior: streaks (consecutive reading days), peak hours, completion rate
- Content pipeline: backlog size, in-progress, completed, archived counts, queue latency
- Highlights: total, color distribution, most highlighted documents

**PostgreSQL Features Used:**
- Window functions: `LAG()` for progress deltas, `ROW_NUMBER()` for streak calculation
- `COUNT(*) FILTER (WHERE ...)` for conditional aggregation
- `DATE_TRUNC()` for time-based grouping
- `EXTRACT(HOUR FROM ...)` for peak hours analysis

**Design: Query-time vs Stored Aggregates:**
- MVP uses query-time computation (no stored aggregates)
- Projection data classes (not JPA entities) enable easy migration to materialized views
- `AnalyticsStore` interface abstracts query implementation—can swap to materialized views later

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| Event publishing over direct persistence | Sync context doesn't own Document entity; other contexts subscribe |
| Sequence return type for fetchDocuments | Memory-efficient lazy evaluation for large result sets |
| Fake test doubles over mocks | More readable, explicit behavior in tests |
| Store interfaces in application layer | Decouples domain from JPA; enables easy testing |
| Tracking uses documentId string, not FK | Decouples Tracking from Library; no entity dependencies between contexts |
| Change detection before snapshot | Only record when values change; reduces storage, enables delta calculations |
| Clock injection in TrackingEventListener | Enables deterministic testing with fixed timestamps |
| JdbcTemplate over JPA for analytics | Native PostgreSQL window functions not expressible in JPQL |
| Projection classes (not entities) | MVP flexibility; easy migration to materialized views/entities later |
| Query-time computation for analytics | MVP phase—can evolve to materialized views without changing API |

## Testing Approach

- **Unit tests**: Fakes for all dependencies (no Spring context)
- **Repository tests**: `@DataJpaTest` with H2 in-memory DB
- **TDD**: Tests written before implementation
