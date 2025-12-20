# Backend - Spring Boot 4.0

## Commands

```bash
./gradlew build                              # Build + test
./gradlew test                               # Run tests
./gradlew test --tests "ClassName.method"    # Single test
./gradlew bootRun --args='--spring.profiles.active=local'  # Run app
docker compose up -d                         # Start PostgreSQL
```

Database: `localhost:5432/readwise_analytics` (postgres/postgres)

## Bounded Contexts

### Sync Context
Fetches from Readwise API, publishes domain events.

| Component | Purpose |
|-----------|---------|
| `SyncService` | Orchestrates sync, publishes events |
| `ReadwiseClient` | API client with rate limiting |
| `RateLimitRetryHandler` | 429 retry with exponential backoff |
| Events | `DocumentSyncedEvent`, `HighlightSyncedEvent`, `NoteSyncedEvent` |

### Library Context
Subscribes to events, persists entities.

| Entity | Description |
|--------|-------------|
| `Document` | Articles, PDFs, EPUBs with reading progress |
| `Highlight` | Text selections from documents |
| `Note` | User annotations on documents or highlights |
| `Tag` | ManyToMany with documents |

### Tracking Context
Records time-series data for analytics.

| Entity | Purpose |
|--------|---------|
| `ReadingProgressSnapshot` | Progress changes with timestamps |
| `LocationChange` | Pipeline transitions (new → later → archive) |

### Analytics Context
Query-time computation via native PostgreSQL.

| Component | Purpose |
|-----------|---------|
| `AnalyticsService` | Facade for all metrics |
| `AnalyticsRepository` | Native SQL with window functions |
| `JpaAnalyticsStore` | Adapter implementing `AnalyticsStore` |

## Key Patterns

**Event-driven**: Sync publishes, other contexts subscribe. No coupling.

**Port/Adapter**: Application layer defines interfaces (`DocumentStore`, `TrackingStore`), infrastructure implements.

**Query-time analytics**: No stored aggregates yet. PostgreSQL window functions (`LAG`, `ROW_NUMBER`) for calculations.

## Readwise API

| API | Rate Limit | Used For |
|-----|------------|----------|
| Reader V3 | 20 req/min | Documents, highlights, notes |
| Highlights V2 | 240 req/min | Not currently used |

Auth: `Authorization: Token {ACCESS_TOKEN}`

## Testing

- **Unit tests**: Fakes for dependencies (no Spring context)
- **Repository tests**: `@DataJpaTest` with H2
- **TDD**: Tests before implementation
