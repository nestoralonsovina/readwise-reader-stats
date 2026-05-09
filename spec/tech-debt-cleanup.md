# Tech Debt Cleanup: Sync Cancel, Dead Code, Flyway, and Indexes

## Summary

Four related issues that block production readiness: a missing sync cancel endpoint (frontend calls it, backend 404s), dead SyncLog code cluttering the codebase, no database migrations (Hibernate `ddl-auto: update` only), and zero indexes on analytics queries that will degrade at scale.

---

## Problem Statement

### 1. Sync Cancel Returns 404

The frontend already implements cancel: `SyncService.cancelSync()` calls `DELETE /sync/{syncId}`, the SSE stream handles `cancelled` events, and `SyncStatus` includes `'cancelled'`. But the backend `SyncController` has no `@DeleteMapping`. Pressing "Cancel Sync" in the UI always fails silently.

```
Frontend: this.http.delete(`/sync/${syncId}`)  →  404 Not Found
```

The infrastructure is partially in place:
- `SyncRunStatus.CANCELLED` enum value exists
- `SyncProgressEvent.Cancelled` event type exists
- `SyncProgressEmitter` already handles `Cancelled` events for cleanup scheduling

**Missing pieces:**
- No `@DeleteMapping` in `SyncController`
- No `cancel()` method in `SyncOrchestrator`
- No cooperative cancellation check in `SyncExecutorImpl`

### 2. Dead SyncLog Code

The async refactor (spec: `async-sync-technical.md`) replaced `SyncLog`/`SyncStatus`/`SyncService`/`LogStore` with `SyncRun`/`SyncRunStatus`/`SyncOrchestrator`/`SyncRunStore`. But the old code was never removed:

| Dead File | Replaced By |
|-----------|-------------|
| `SyncLog.kt` | `SyncRun.kt` |
| `SyncStatus.kt` | `SyncRunStatus.kt` |
| `SyncService.kt` | `SyncOrchestrator` + `SyncExecutorImpl` |
| `LogStore.kt` (interface) | `SyncRunStore.kt` |
| `JpaLogStore.kt` | `JpaSyncRunStore.kt` |
| `SyncLogRepository.kt` | `SyncRunRepository.kt` |
| `SyncServiceTest.kt` (399 lines) | Tests in `SyncOrchestratorTest.kt` |

`SyncService` is still a `@Service` bean. While no controller injects it, Spring still creates the bean and the `sync_logs` table still gets created by Hibernate.

### 3. No Database Migrations

The project uses Hibernate `ddl-auto: update` for local development and `create-drop` for tests. There are no Flyway or Liquibase migrations.

**Problems:**
- No versioned schema history
- No ability to evolve schema in production safely
- `ddl-auto: update` cannot create indexes from `@Index` annotations
- No rollback capability
- Schema drift between environments

### 4. No Database Indexes

Every JPA entity has `@Table(name = "...")` but none have `@Index` annotations. All analytics queries perform full table scans. The data-scalability spec (`data-scalability-improvements.md`) proposes indexes for multi-tenant scenarios, but indexes are needed **now** even for single-tenant.

**Most affected queries** (from `AnalyticsRepository`):

| Table | Column(s) | Query Pattern | Impact |
|-------|-----------|---------------|--------|
| `reading_progress_snapshots` | `(document_id, recorded_at DESC)` | `DISTINCT ON (document_id)` in 6+ CTEs | Critical — reads every row |
| `reading_progress_snapshots` | `(recorded_at)` | All date-range WHERE clauses | Critical |
| `documents` | `(location)` | `WHERE location IN ('new', 'later')` | High |
| `documents` | `(saved_at)` | Date-range filters, ORDER BY | High |
| `highlights` | `(document_id)` | JOIN, find-by-document | High |
| `notes` | `(highlight_id)` | Subquery lookups | Medium |
| `sync_runs` | `(status)` | `findFirstByStatus()` on every sync start | Medium |
| `sync_runs` | `(started_at DESC)` | History listing | Low |

---

## Technical Decisions

| Aspect | Choice | Rationale |
|--------|--------|-----------|
| Migrations | Flyway | Industry standard for Spring Boot, SQL-based, easy rollback |
| Index creation | Via Flyway migrations, not `@Index` annotations | `ddl-auto: update` doesn't create `@Index`; Flyway gives versioned, explicit control |
| Cancellation mechanism | Volatile flag on `SyncRun` | Cooperative cancellation is safer than Thread.interrupt; already persisted so survives restart |
| SyncLog removal | Delete all dead code and table in one migration | No API dependents, no need for gradual migration |

---

## Implementation

### Phase 1: Add Flyway and Initial Indexes

#### 1.1 Add Flyway dependency

**File:** `backend/build.gradle.kts`

Add to `dependencies`:
```kotlin
runtimeOnly("org.flywaydb:flyway-database-postgresql")
```

#### 1.2 Configure Flyway

**File:** `backend/src/main/resources/application-local.yml`

Add:
```yaml
flyway:
  enabled: true
  locations: classpath:db/migration
```

Keep `ddl-auto: update` for local development temporarily. After all migrations are verified, change to `ddl-auto: validate`.

**File:** `backend/src/test/resources/application.yml`

Add:
```yaml
flyway:
  enabled: true
  locations: classpath:db/migration
```

Change `ddl-auto: create-drop` to `ddl-auto: none` — Flyway will handle schema creation in tests too.

#### 1.3 Create migration directory

```
backend/src/main/resources/db/migration/
```

#### 1.4 Initial baseline migration

**File:** `backend/src/main/resources/db/migration/V1__baseline_schema.sql`

This migration captures the current schema as-is so Flyway has a baseline. Generate from the running database:

```sql
-- Baseline: capture current schema as managed by Flyway going forward
-- This migration represents the schema as it exists before Flyway was introduced

-- Note: If running on an existing database, use flyway repair + baseline
-- to mark V1 as already applied. For fresh databases, this creates everything.
```

Use `pg_dump --schema-only` or Hibernate's `ddl-auto: update` output to generate the exact DDL for all current tables: `documents`, `highlights`, `notes`, `tags`, `documents_tags`, `reading_progress_snapshots`, `location_changes`, `sync_runs`, `sync_cursors`, and `sync_logs`.

#### 1.5 Index migration

**File:** `backend/src/main/resources/db/migration/V2__add_analytics_indexes.sql`

```sql
-- Reading progress snapshots: most critical table for analytics
CREATE INDEX idx_rps_document_recorded
    ON reading_progress_snapshots(document_id, recorded_at DESC);

CREATE INDEX idx_rps_recorded_at
    ON reading_progress_snapshots(recorded_at);

-- Documents: pipeline and breakdown queries
CREATE INDEX idx_documents_location
    ON documents(location);

CREATE INDEX idx_documents_saved_at
    ON documents(saved_at);

CREATE INDEX idx_documents_category
    ON documents(category);

-- Highlights: document joins and date filters
CREATE INDEX idx_highlights_document_id
    ON highlights(document_id);

CREATE INDEX idx_highlights_highlighted_at
    ON highlights(highlighted_at);

-- Notes: parent lookups
CREATE INDEX idx_notes_highlight_id
    ON notes(highlight_id);

CREATE INDEX idx_notes_document_id
    ON notes(document_id);

-- Sync runs: active sync check and history
CREATE INDEX idx_sync_runs_status
    ON sync_runs(status);

CREATE INDEX idx_sync_runs_started_at
    ON sync_runs(started_at DESC);
```

---

### Phase 2: Sync Cancel Endpoint

#### 2.1 Add cancel method to SyncOrchestrator

**File:** `backend/src/main/kotlin/com/reader/analytics/sync/application/SyncOrchestrator.kt`

```kotlin
fun cancel(syncId: UUID): CancelResult {
    val syncRun = syncRunStore.findById(syncId)
        ?: return CancelResult.NotFound

    if (syncRun.status != SyncRunStatus.RUNNING && syncRun.status != SyncRunStatus.PENDING) {
        return CancelResult.NotCancellable(syncRun.status)
    }

    val cancelled = syncRun.copy(
        status = SyncRunStatus.CANCELLED,
        completedAt = Instant.now()
    )
    syncRunStore.save(cancelled)

    progressEmitter.emit(syncId, SyncProgressEvent.Cancelled(
        syncId = syncId,
        reason = "Cancelled by user"
    ))

    return CancelResult.Success
}
```

```kotlin
sealed class CancelResult {
    data object Success : CancelResult()
    data object NotFound : CancelResult()
    data class NotCancellable(val status: SyncRunStatus) : CancelResult()
}
```

#### 2.2 Add cooperative cancellation check in SyncExecutorImpl

**File:** `backend/src/main/kotlin/com/reader/analytics/sync/application/SyncExecutorImpl.kt`

Add a check at the start of each phase and between batch pages:

```kotlin
private fun isCancelled(syncId: UUID): Boolean {
    return syncRunStore.findById(syncId)?.status == SyncRunStatus.CANCELLED
}
```

Check before starting each phase and after each page of items. If cancelled, stop processing and return — the `cancel()` method already sets the status and emits the event.

#### 2.3 Add DELETE endpoint to SyncController

**File:** `backend/src/main/kotlin/com/reader/analytics/api/SyncController.kt`

```kotlin
@DeleteMapping("/{syncId}")
fun cancelSync(@PathVariable syncId: UUID): ResponseEntity<*> {
    return when (val result = orchestrator.cancel(syncId)) {
        is CancelResult.Success -> ResponseEntity.noContent().build()
        is CancelResult.NotFound -> ResponseEntity.notFound().build()
        is CancelResult.NotCancellable -> ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(mapOf("error" to "Sync is in status: ${result.status}"))
    }
}
```

#### 2.4 Add sync history endpoint

**File:** `backend/src/main/kotlin/com/reader/analytics/api/SyncController.kt`

```kotlin
@GetMapping("/history")
fun getHistory(@RequestParam(defaultValue = "20") limit: Int): ResponseEntity<List<SyncStatusResponse>> {
    val runs = syncRunStore.findRecent(limit)
    return ResponseEntity.ok(runs.map { it.toResponse() })
}
```

Add `findRecent(limit: Int)` to `SyncRunStore` interface and `JpaSyncRunStore` implementation.

---

### Phase 3: Remove Dead SyncLog Code

#### 3.1 Remove dead files

Delete the following files:

| File | Reason |
|------|--------|
| `sync/domain/SyncLog.kt` | Replaced by `SyncRun` |
| `sync/domain/SyncStatus.kt` | Replaced by `SyncRunStatus` |
| `sync/application/SyncService.kt` | Replaced by `SyncOrchestrator` + `SyncExecutorImpl` |
| `sync/application/LogStore.kt` | Replaced by `SyncRunStore` |
| `sync/infrastructure/persistence/JpaLogStore.kt` | Replaced by `JpaSyncRunStore` |
| `sync/infrastructure/persistence/SyncLogRepository.kt` | Replaced by `SyncRunRepository` |
| `sync/application/SyncServiceTest.kt` (test) | Tests dead code |

#### 3.2 Drop `sync_logs` table

**File:** `backend/src/main/resources/db/migration/V3__drop_sync_logs.sql`

```sql
DROP TABLE IF EXISTS sync_logs;
```

#### 3.3 Remove SyncLog import from remaining code

Search for any remaining imports or references to `SyncLog`, `SyncStatus`, `LogStore`, `JpaLogStore`, `SyncLogRepository` in production and test code and remove them.

#### 3.4 Update AGENTS.md

Update the Architecture table to replace `SyncLog` with `SyncRun` and add the cancel/history endpoints.

---

### Phase 4: Switch to Flyway-Managed Schema

#### 4.1 Change ddl-auto to validate

**File:** `backend/src/main/resources/application-local.yml`

Change:
```yaml
jpa:
  hibernate:
    ddl-auto: validate  # was: update
```

**File:** `backend/src/test/resources/application.yml`

Change:
```yaml
jpa:
  hibernate:
    ddl-auto: none  # was: create-drop — Flyway handles schema
```

#### 4.2 Generate baseline for existing databases

For databases that already have the schema (local development):

```bash
./gradlew flywayBaseline  # marks V1 as already applied
```

#### 4.3 Add migration for future schema changes

All future schema changes must be new numbered migrations in `db/migration/`. No more relying on Hibernate to auto-create tables.

---

## Files to Modify

```
backend/
├── build.gradle.kts                                           # Add Flyway dependency
├── src/main/resources/
│   ├── application-local.yml                                  # Flyway config, ddl-auto: validate
│   └── db/migration/
│       ├── V1__baseline_schema.sql                            # Capture current schema
│       ├── V2__add_analytics_indexes.sql                      # All indexes
│       └── V3__drop_sync_logs.sql                              # Drop dead table
├── src/main/kotlin/com/reader/analytics/
│   ├── api/SyncController.kt                                  # Add DELETE, GET /history
│   └── sync/application/
│       ├── SyncOrchestrator.kt                                # Add cancel(), CancelResult
│       ├── CancelResult.kt                                    # New sealed class
│       └── SyncExecutorImpl.kt                                # Add isCancelled() checks
├── src/test/resources/application.yml                         # Flyway config, ddl-auto: none
└── (delete)
    ├── sync/domain/SyncLog.kt
    ├── sync/domain/SyncStatus.kt
    ├── sync/application/SyncService.kt
    ├── sync/application/LogStore.kt
    ├── sync/infrastructure/persistence/JpaLogStore.kt
    ├── sync/infrastructure/persistence/SyncLogRepository.kt
    └── sync/application/SyncServiceTest.kt
```

---

## Testing

### Unit Tests

1. `SyncOrchestrator.cancel()` returns `Success` for running sync
2. `SyncOrchestrator.cancel()` returns `NotFound` for unknown syncId
3. `SyncOrchestrator.cancel()` returns `NotCancellable` for completed/failed sync
4. `SyncExecutorImpl` stops processing after cancellation flag set
5. `SyncRunStore.findRecent()` returns syncs ordered by startedAt DESC

### Integration Tests (@DataJpaTest)

1. Flyway migrations apply cleanly on empty database
2. Flyway migrations apply cleanly on existing database (baseline)
3. All indexes exist after V2 migration
4. `sync_logs` table does not exist after V3 migration

### API Tests

1. `DELETE /sync/{syncId}` returns 204 on successful cancel
2. `DELETE /sync/{syncId}` returns 404 for unknown sync
3. `DELETE /sync/{syncId}` returns 409 for completed/failed sync
4. `GET /sync/history` returns paginated list of past syncs

---

## Trade-offs

| Decision | Trade-off |
|----------|-----------|
| Volatile flag for cancellation | Requires cooperative checks in executor (not instant kill), but safe — no thread interruption risks |
| Flyway over Hibernate auto-DDL | More scaffolding upfront, but production-safe and version-controlled |
| Drop `sync_logs` in one migration | Can't rollback to SyncLog code, but it's dead code with no callers |
| Indexes via Flyway, not `@Index` | Can't auto-generate from entities, but Flyway is the source of truth for production schema |

---

## Out of Scope

- Multi-tenancy (`userId` on entities) — covered by `data-scalability-improvements.md`
- `DocumentCurrentState` table — covered by `data-scalability-improvements.md`
- Daily reading rollups — covered by `data-scalability-improvements.md`
- Frontend sync history panel UI — Phase 4 of `async-sync-technical.md`