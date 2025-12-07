# Readwise Reader Analytics

Analytics dashboard for your Readwise Reader library. Syncs documents and highlights, tracks reading progress over time, and surfaces insights about your reading habits.

## Architecture

Event-driven bounded contexts with clean separation:

```
                      API Layer
                    SyncController
                          |
                          v
                 Sync Bounded Context
    +---------------------------------------------+
    |  Domain        Application    Infrastructure|
    |  - Events      - SyncService  - Readwise API|
    |  - Entities    - Stores       - JPA Repos   |
    +---------------------+-----------------------+
                          | publishes
                          v
              +-----------------------+
              |  DocumentSyncedEvent  |
              |  HighlightSyncedEvent |
              +-----------+-----------+
                          | subscribes
              +-----------+-----------+
              v                       v
     Library Context           Tracking Context
    +------------------+      +------------------+
    | - Document       |      | - ProgressSnap   |
    | - Highlight      |      | - LocationChange |
    | - Tag            |      | - TrackingStore  |
    +------------------+      +------------------+
              |                       |
              +-----------+-----------+
                          | queries
                          v
                 Analytics Context
              +------------------------+
              | - AnalyticsService     |
              | - AnalyticsStore       |
              | - PostgreSQL queries   |
              +------------------------+
                          |
                          v
                    API Layer
               AnalyticsController
```

## Tech Stack

- **Kotlin 2.2** + **Spring Boot 4.0**
- **PostgreSQL 16** with Hibernate 7.1
- **Spring RestClient** for Readwise API
- **GraalVM Native Image** support

## Quick Start

Prerequisites: Docker, Java 21

```bash
# Start database
cd backend && docker compose up -d

# Run application
./gradlew bootRun --args='--spring.profiles.active=local'

# Trigger sync
curl -X POST http://localhost:8080/sync
```

## API Endpoints

### Sync
| Method | Path    | Description                      |
|--------|---------|----------------------------------|
| POST   | /sync   | Trigger incremental document sync |

### Analytics
| Method | Path                           | Description                    |
|--------|--------------------------------|--------------------------------|
| GET    | /api/analytics/dashboard       | Dashboard summary (all metrics)|
| GET    | /api/analytics/reading/stats   | Time-series reading stats      |
| GET    | /api/analytics/reading/streak  | Current & longest streak       |
| GET    | /api/analytics/reading/peak-hours | Reading activity by hour    |
| GET    | /api/analytics/pipeline        | Content pipeline metrics       |
| GET    | /api/analytics/highlights      | Highlight statistics           |

Query parameters:
- `days` - Period in days (default: 7 for dashboard, 30 for highlights)
- `startDate`, `endDate` - Custom date range (ISO format)
- `granularity` - DAILY, WEEKLY, MONTHLY (for reading stats)

## Project Structure

```
backend/src/main/kotlin/com/reader/analytics/
   api/                    # REST controllers
      AnalyticsController  # Analytics endpoints
      dto/                # Response DTOs
   sync/                   # Sync bounded context
      domain/             # Entities, events
      application/        # Use cases, port interfaces
      infrastructure/     # API clients, JPA adapters
   library/                # Library bounded context
      domain/             # Document, Highlight, Tag entities
      application/        # DocumentStore, EventListener
      infrastructure/     # JPA repositories, adapters
   tracking/               # Tracking bounded context
      domain/             # ReadingProgressSnapshot, LocationChange
      application/        # TrackingStore, TrackingEventListener
      infrastructure/     # JPA repositories, adapters
   analytics/              # Analytics bounded context
      domain/             # DateRange, Granularity, projections
      application/        # AnalyticsStore, AnalyticsService
      infrastructure/     # Native PostgreSQL queries via JdbcTemplate
```

## Development

```bash
./gradlew build          # Build + test
./gradlew test           # Run tests only
./gradlew bootRun        # Run app (needs DB)
```

## Testing Philosophy

- **TDD**: Tests written before implementation
- **Fakes over mocks**: Explicit, readable test doubles
- **Repository tests**: `@DataJpaTest` with H2 in-memory
- **Unit tests**: No Spring context needed
