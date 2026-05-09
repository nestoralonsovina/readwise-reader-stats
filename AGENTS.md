# Readwise Reader Analytics

Analytics dashboard for Readwise Reader library. Syncs documents, highlights, and notes from the Readwise API and surfaces reading insights.

## Project Structure

```
├── backend/           # Spring Boot 4.0 + Kotlin (see backend/CLAUDE.md)
├── readwise-analytics/ # Angular 21 dashboard (see readwise-analytics/CLAUDE.md)
├── bruno/             # API collection for testing
├── spec/              # Implementation specs
└── design/            # UI mockups
```

## Quick Start

### Docker (Full Stack)

```bash
# Start everything with hot-reload
docker compose -f docker-compose.dev.yml up --build

# With auto-rebuild on backend changes
docker compose -f docker-compose.dev.yml watch
```

### Manual (Separate Terminals)

```bash
# Backend
cd backend && docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'

# Frontend
cd readwise-analytics
bun install && bun run start
```

Backend: http://localhost:8080 | Frontend: http://localhost:4200

### Hot Reload Behavior

| Component | Reload | Mechanism |
|-----------|--------|-----------|
| Frontend | Instant | Volume mount + Angular HMR |
| Backend | Container rebuild | `docker compose watch` |

## Architecture

Event-driven bounded contexts with clean separation:

```
Sync Context → publishes events → Library, Tracking contexts → Analytics queries
```

| Context | Purpose | Key Entities |
|---------|---------|--------------|
| Sync | Readwise API integration, async orchestration | SyncRun, SyncPhase, SyncCursor, SyncProgressEvent |
| Library | Document storage | Document, Highlight, Note, Tag |
| Tracking | Progress time-series | ReadingProgressSnapshot, LocationChange |
| Analytics | Query-time metrics | Projections via PostgreSQL |

## API Endpoints

| Path | Description |
|------|-------------|
| `POST /sync` | Trigger async sync (returns syncId immediately) |
| `GET /sync/{syncId}` | Poll sync status |
| `GET /sync/{syncId}/stream` | SSE stream of sync progress events |
| `GET /sync/active` | Check if sync is running |
| `DELETE /sync/{syncId}` | Cancel running sync |
| `GET /sync/history` | Past sync runs |
| `GET /api/analytics/dashboard` | All dashboard metrics |
| `GET /api/analytics/reading/stats` | Time-series reading data |
| `GET /api/analytics/reading/streak` | Current and longest streak |
| `GET /api/analytics/reading/peak-hours` | Hourly activity distribution |
| `GET /api/analytics/pipeline` | Content pipeline metrics |
| `GET /api/analytics/highlights` | Highlight statistics |
| `GET /api/analytics/drill-down/words-read` | Words-read drill-down (cursor) |
| `GET /api/analytics/drill-down/completed` | Completed-docs drill-down (cursor) |
| `GET /api/analytics/drill-down/backlog` | Backlog drill-down (cursor) |
| `GET /api/documents/{id}` | Document detail with highlights/notes |

Query params: `startDate`, `endDate` (ISO format), `granularity` (DAILY/WEEKLY/MONTHLY)

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Kotlin 2.2, Spring Boot 4.0, PostgreSQL 16, Hibernate 7.1 |
| Frontend | Angular 21, Tailwind CSS 4, Spartan UI, ApexCharts |
| Tooling | Gradle, Bun, GraalVM Native Image |

## Design Specs

Specs are in `spec/` as named markdown files. Each spec follows the template in `spec/template.md`. See `spec/template.md` for the expected structure.

## Testing

### Backend
```bash
./gradlew test                                # All tests
./gradlew test --tests "ClassName.method"    # Single test
```

- **Unit tests**: Fakes for stores in `testFixtures`
- **Repository tests**: `@DataJpaTest` with H2
- **Integration tests**: `@SpringBootTest` for full context

### Frontend
```bash
cd readwise-analytics && bun run test         # Vitest
```