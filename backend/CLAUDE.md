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
Fetches from Readwise API, publishes domain events, streams progress via SSE.

| Component | Purpose |
|-----------|---------|
| `SyncOrchestrator` | Mutex check, spawns async sync task |
| `SyncExecutorImpl` | Phased sync (documents → highlights → notes) |
| `SyncProgressEmitter` | SSE event broadcasting to connected clients |
| `ReadwiseClient` | API client with rate limiting |
| `RateLimitRetryHandler` | 429 retry with exponential backoff |
| `SyncRunStore` | Persistence port for SyncRun |
| Events | `DocumentSyncedEvent`, `HighlightSyncedEvent`, `NoteSyncedEvent` |

Key domain types: `SyncRun`, `SyncRunStatus`, `SyncPhase`, `SyncProgressEvent`

### Library Context
Subscribes to events, persists entities.

| Entity | Description |
|--------|-------------|
| `Document` | Articles, PDFs, EPUBs with reading progress |
| `Highlight` | Text selections from documents |
| `Note` | User annotations on documents or highlights |
| `Tag` | ManyToMany with documents |

| Service | Purpose |
|---------|---------|
| `DocumentService` | Document detail assembly with highlights and notes |
| `DrillDownService` | Cursor-paginated drill-down queries (words-read, completed, backlog) |

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

**Async sync**: `SyncOrchestrator.startSync()` creates `SyncRun`, spawns `SyncExecutorImpl` on virtual thread. Progress streamed via SSE (`SyncProgressEmitter`). Cancel via `SyncOrchestrator.cancel()`.

**Event-driven**: Sync publishes domain events, other contexts subscribe. No coupling.

**Port/Adapter**: Application layer defines interfaces (`SyncRunStore`, `DocumentStore`, `TrackingStore`, `AnalyticsStore`), infrastructure implements.

**Query-time analytics**: No stored aggregates. PostgreSQL window functions (`LAG`, `ROW_NUMBER`, `DISTINCT ON`) for calculations.

**Cursor pagination**: Drill-down endpoints use keyset pagination (not offset) for stable paging through large result sets.

## Readwise API

| API | Rate Limit | Used For |
|-----|------------|----------|
| Reader V3 | 20 req/min | Documents, highlights, notes |
| Highlights V2 | 240 req/min | Not currently used |

Auth: `Authorization: Token {ACCESS_TOKEN}`

## Testing

- **Unit tests**: Fakes for stores (`FakeAnalyticsStore`, `FakeDocumentStore`, etc.) in `src/testFixtures`
- **Repository tests**: `@DataJpaTest` with H2 for JPA queries
- **Integration tests**: `@SpringBootTest` for event listeners and full flow
- **TDD**: Write tests before implementation
- Run: `./gradlew test` or `./gradlew test --tests "ClassName.method"`