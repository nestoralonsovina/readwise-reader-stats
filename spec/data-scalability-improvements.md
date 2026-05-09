# Data Scalability Improvements

## Summary

Prepare the data layer for 500-1000 users with thousands of documents each. Addresses multi-tenancy, time-series growth, and query performance.

**Target Scale:** 1000 users × 3000 documents = 3M documents, 30M+ snapshots

---

## Problem Statement

Current architecture is single-tenant with unbounded time-series growth and missing indexes. Will not scale beyond current usage.

**Key Metrics:**
| Issue | Current | Target |
|-------|---------|--------|
| Multi-tenancy | None | Full user isolation |
| Snapshot growth | Unbounded | Capped with rollups |
| Dashboard query | ~500ms (1 user) | <200ms (1000 users) |
| Streak calculation | Full table scan | O(1) lookup |

---

## Phase 1: Multi-Tenancy Foundation

### 1.1 Add userId to Core Entities

```kotlin
// Document.kt
@Entity
@Table(name = "documents")
data class Document(
    // ... existing fields

    @Column(nullable = false)
    val userId: String,  // External user identifier
)

// ReadingProgressSnapshot.kt
@Entity
@Table(name = "reading_progress_snapshots")
data class ReadingProgressSnapshot(
    // ... existing fields

    @Column(nullable = false)
    val userId: String,
)

// LocationChange.kt - same pattern
// SyncCursor.kt - same pattern
// SyncLog.kt - same pattern
```

**Why String not UUID for userId:** External identity provider IDs may not be UUIDs. String is flexible.

### 1.2 Update All Repositories

```kotlin
interface DocumentRepository : JpaRepository<Document, UUID> {
    fun findByUserIdAndReadwiseId(userId: String, readwiseId: String): Document?
    fun findByUserId(userId: String): List<Document>
}

interface SnapshotRepository : JpaRepository<ReadingProgressSnapshot, UUID> {
    fun findTopByUserIdAndDocumentIdOrderByRecordedAtDesc(
        userId: String,
        documentId: String
    ): ReadingProgressSnapshot?
}
```

### 1.3 Add User Context

```kotlin
// core/application/UserContext.kt
interface UserContext {
    fun currentUserId(): String
}

// For single-tenant migration, use default user
@Component
class DefaultUserContext : UserContext {
    override fun currentUserId(): String = "default-user"
}
```

### 1.4 Database Migration

```sql
-- Add userId columns (nullable first for migration)
ALTER TABLE documents ADD COLUMN user_id VARCHAR(255);
ALTER TABLE reading_progress_snapshots ADD COLUMN user_id VARCHAR(255);
ALTER TABLE location_changes ADD COLUMN user_id VARCHAR(255);
ALTER TABLE sync_cursors ADD COLUMN user_id VARCHAR(255);
ALTER TABLE sync_logs ADD COLUMN user_id VARCHAR(255);

-- Backfill existing data
UPDATE documents SET user_id = 'default-user' WHERE user_id IS NULL;
UPDATE reading_progress_snapshots SET user_id = 'default-user' WHERE user_id IS NULL;
UPDATE location_changes SET user_id = 'default-user' WHERE user_id IS NULL;
UPDATE sync_cursors SET user_id = 'default-user' WHERE user_id IS NULL;
UPDATE sync_logs SET user_id = 'default-user' WHERE user_id IS NULL;

-- Make non-nullable
ALTER TABLE documents ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE reading_progress_snapshots ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE location_changes ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE sync_cursors ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE sync_logs ALTER COLUMN user_id SET NOT NULL;
```

---

## Phase 2: Index Strategy

### 2.1 Primary Query Indexes

```sql
-- ReadingProgressSnapshot: Most queried table
CREATE INDEX idx_rps_user_doc_recorded
    ON reading_progress_snapshots(user_id, document_id, recorded_at DESC);

CREATE INDEX idx_rps_user_recorded
    ON reading_progress_snapshots(user_id, recorded_at);

-- LocationChange
CREATE INDEX idx_lc_user_doc_changed
    ON location_changes(user_id, document_id, changed_at DESC);

-- Document
CREATE INDEX idx_doc_user_readwise
    ON documents(user_id, readwise_id);

CREATE INDEX idx_doc_user_location
    ON documents(user_id, location);

CREATE INDEX idx_doc_user_saved
    ON documents(user_id, saved_at);

-- Highlight
CREATE INDEX idx_hl_doc_highlighted
    ON highlights(document_id, highlighted_at);

-- Note
CREATE INDEX idx_note_parent
    ON notes(highlight_id) WHERE highlight_id IS NOT NULL;
```

### 2.2 Partial Indexes for Common Filters

```sql
-- Only active reading (not archived)
CREATE INDEX idx_doc_user_active
    ON documents(user_id, saved_at)
    WHERE location IN ('new', 'later', 'shortlist');

-- Completed documents
CREATE INDEX idx_rps_user_completed
    ON reading_progress_snapshots(user_id, recorded_at)
    WHERE reading_progress = 1.0;
```

---

## Phase 3: Snapshot Optimization

### 3.1 Current State Table

Add a materialized "current state" table to avoid repeated `DISTINCT ON` queries.

```kotlin
@Entity
@Table(name = "document_current_state")
data class DocumentCurrentState(
    @Id
    val documentId: UUID,  // FK to Document.id

    val userId: String,
    val readingProgress: Double,
    val wordCount: Int?,
    val lastProgressAt: Instant?,
    val location: String?,
    val lastLocationAt: Instant?
)
```

**Maintained by:** Trigger or application-level upsert on snapshot insert.

```sql
CREATE TABLE document_current_state (
    document_id UUID PRIMARY KEY REFERENCES documents(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    reading_progress DOUBLE PRECISION NOT NULL DEFAULT 0,
    word_count INTEGER,
    last_progress_at TIMESTAMP WITH TIME ZONE,
    location VARCHAR(50),
    last_location_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_dcs_user ON document_current_state(user_id);
CREATE INDEX idx_dcs_user_progress ON document_current_state(user_id, reading_progress);
```

### 3.2 Update Tracking Listener

```kotlin
@Component
class TrackingEventListener(
    private val trackingStore: TrackingStore,
    private val currentStateStore: CurrentStateStore,  // NEW
    private val clock: Clock = Clock.systemUTC()
) {
    @EventListener
    @Transactional
    fun onDocumentSynced(event: DocumentSyncedEvent) {
        trackProgressChange(event)
        trackLocationChange(event)
        updateCurrentState(event)  // NEW
    }

    private fun updateCurrentState(event: DocumentSyncedEvent) {
        currentStateStore.upsert(
            documentId = event.documentUuid,  // Need to resolve
            userId = event.userId,
            readingProgress = event.readingProgress ?: 0.0,
            wordCount = event.wordCount,
            lastProgressAt = event.updatedAt,
            location = event.location,
            lastLocationAt = event.updatedAt
        )
    }
}
```

### 3.3 Daily Rollup Table

For historical analytics, roll up snapshots to daily granularity.

```sql
CREATE TABLE daily_reading_stats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    words_read BIGINT NOT NULL DEFAULT 0,
    articles_progressed INTEGER NOT NULL DEFAULT 0,
    articles_completed INTEGER NOT NULL DEFAULT 0,
    reading_minutes INTEGER NOT NULL DEFAULT 0,  -- Future: session tracking

    UNIQUE(user_id, date)
);

CREATE INDEX idx_drs_user_date ON daily_reading_stats(user_id, date DESC);
```

**Rollup Job (Daily):**

```kotlin
@Scheduled(cron = "0 5 0 * * *")  // 00:05 daily
fun rollupYesterdaysProgress() {
    val yesterday = LocalDate.now().minusDays(1)
    analyticsRepository.rollupDailyStats(yesterday)
}
```

```sql
-- Rollup query
INSERT INTO daily_reading_stats (user_id, date, words_read, articles_progressed, articles_completed)
SELECT
    user_id,
    DATE(recorded_at) as date,
    SUM(CASE
        WHEN progress_delta > 0 AND word_count IS NOT NULL
        THEN (progress_delta * word_count)::bigint
        ELSE 0
    END) as words_read,
    COUNT(DISTINCT CASE WHEN progress_delta > 0 THEN document_id END) as articles_progressed,
    COUNT(DISTINCT CASE WHEN reading_progress = 1 AND progress_delta > 0 THEN document_id END) as articles_completed
FROM (
    SELECT
        user_id, document_id, reading_progress, word_count, recorded_at,
        reading_progress - LAG(reading_progress, 1, 0) OVER (
            PARTITION BY document_id ORDER BY recorded_at
        ) AS progress_delta
    FROM reading_progress_snapshots
    WHERE DATE(recorded_at) = :date
) deltas
WHERE progress_delta > 0
GROUP BY user_id, DATE(recorded_at)
ON CONFLICT (user_id, date) DO UPDATE SET
    words_read = EXCLUDED.words_read,
    articles_progressed = EXCLUDED.articles_progressed,
    articles_completed = EXCLUDED.articles_completed;
```

### 3.4 Retention Policy

Keep raw snapshots for 90 days, then delete.

```kotlin
@Scheduled(cron = "0 0 3 * * *")  // 03:00 daily
fun cleanupOldSnapshots() {
    val cutoff = Instant.now().minus(90, ChronoUnit.DAYS)
    snapshotRepository.deleteByRecordedAtBefore(cutoff)
}
```

---

## Phase 4: Query Optimizations

### 4.1 Refactor Analytics Repository

**Before (current):**
```kotlin
fun getReadingStatsByPeriod(...): List<RawDailyStats> {
    // Complex CTE with window functions on raw snapshots
}
```

**After:**
```kotlin
fun getReadingStatsByPeriod(
    userId: String,
    startDate: LocalDate,
    endDate: LocalDate,
    granularity: Granularity
): List<RawDailyStats> {
    // For recent data (< 90 days): compute from raw snapshots
    // For older data: read from daily_reading_stats

    return if (ChronoUnit.DAYS.between(startDate, LocalDate.now()) > 90) {
        getFromDailyRollups(userId, startDate, endDate, granularity)
    } else {
        computeFromSnapshots(userId, startDate, endDate, granularity)
    }
}
```

### 4.2 Pipeline Stats from Current State

**Before:**
```sql
WITH latest_progress AS (
    SELECT DISTINCT ON (document_id) ...
    FROM reading_progress_snapshots
    ORDER BY document_id, recorded_at DESC
)
SELECT COUNT(*) FILTER (WHERE ...) ...
```

**After:**
```sql
SELECT
    COUNT(*) FILTER (WHERE d.location IN ('new', 'later')) AS backlog_size,
    COUNT(*) FILTER (WHERE dcs.reading_progress > 0 AND dcs.reading_progress < 1) AS in_progress,
    COUNT(*) FILTER (WHERE dcs.reading_progress = 1) AS completed,
    COUNT(*) FILTER (WHERE d.location = 'archive') AS archived
FROM documents d
JOIN document_current_state dcs ON dcs.document_id = d.id
WHERE d.user_id = :userId
```

### 4.3 Streak Cache

Reading streaks are expensive to compute. Cache current streak.

```kotlin
@Entity
@Table(name = "user_streak_cache")
data class UserStreakCache(
    @Id
    val userId: String,

    val currentStreak: Int,
    val currentStreakStart: LocalDate?,
    val longestStreak: Int,
    val longestStreakStart: LocalDate?,
    val longestStreakEnd: LocalDate?,
    val lastActivityDate: LocalDate?,
    val updatedAt: Instant
)
```

**Update on activity:**
```kotlin
fun recordActivity(userId: String, date: LocalDate) {
    val cache = streakCacheRepository.findById(userId).orElse(newCache(userId))

    val updated = when {
        cache.lastActivityDate == date -> cache  // Same day, no change
        cache.lastActivityDate == date.minusDays(1) -> cache.incrementStreak(date)
        else -> cache.resetStreak(date)
    }

    streakCacheRepository.save(updated)
}
```

---

## Phase 5: Foreign Key Migration

### 5.1 Add Document FK to Snapshots

```sql
-- Add UUID column
ALTER TABLE reading_progress_snapshots
    ADD COLUMN document_uuid UUID;

-- Backfill from document.readwise_id
UPDATE reading_progress_snapshots rps
SET document_uuid = d.id
FROM documents d
WHERE d.readwise_id = rps.document_id;

-- Add FK constraint
ALTER TABLE reading_progress_snapshots
    ADD CONSTRAINT fk_rps_document
    FOREIGN KEY (document_uuid)
    REFERENCES documents(id)
    ON DELETE CASCADE;

-- Create index
CREATE INDEX idx_rps_doc_uuid ON reading_progress_snapshots(document_uuid);

-- Eventually: drop old string column
-- ALTER TABLE reading_progress_snapshots DROP COLUMN document_id;
```

### 5.2 Remove Denormalized Columns

Once current state table is in place:

```sql
ALTER TABLE reading_progress_snapshots DROP COLUMN word_count;
ALTER TABLE reading_progress_snapshots DROP COLUMN first_opened_at;
ALTER TABLE reading_progress_snapshots DROP COLUMN last_opened_at;
```

**Note:** Requires updating `TrackingEventListener` to not set these fields.

---

## Implementation Order

### Phase 1: Foundation (Week 1)
1. Add `userId` to all entities
2. Create `UserContext` interface with default implementation
3. Run migration to backfill existing data
4. Update all repositories to filter by userId

### Phase 2: Indexes (Week 1)
1. Add all indexes in single migration
2. Analyze query plans before/after
3. Tune index strategy based on actual query patterns

### Phase 3: Current State (Week 2)
1. Create `document_current_state` table
2. Add `CurrentStateStore` interface + implementation
3. Update `TrackingEventListener` to maintain current state
4. Backfill current state from existing snapshots
5. Migrate `getPipelineStats` to use current state

### Phase 4: Daily Rollups (Week 2-3)
1. Create `daily_reading_stats` table
2. Implement rollup job
3. Backfill historical data
4. Update `getReadingStatsByPeriod` to use rollups for old data

### Phase 5: Streak Cache (Week 3)
1. Create `user_streak_cache` table
2. Implement cache update on activity
3. Migrate `getReadingStreak` to use cache
4. Add cache invalidation/rebuild

### Phase 6: Cleanup (Week 4)
1. Add FK constraints with proper cascades
2. Remove denormalized columns from snapshots
3. Implement retention policy
4. Performance testing at scale

---

## Trade-offs

| Decision | Benefit | Cost |
|----------|---------|------|
| Current state table | O(1) latest progress lookup | Extra write per progress update |
| Daily rollups | Fast historical queries | Delayed accuracy (day-old) |
| Streak cache | O(1) streak lookup | Cache invalidation complexity |
| 90-day retention | Bounded storage | Lose granular old data |
| UUID FKs | Referential integrity | Migration effort |

---

## Success Metrics

| Metric | Current | Target | How to Measure |
|--------|---------|--------|----------------|
| Dashboard load | ~500ms | <200ms | API response time p95 |
| Streak calculation | O(n) scan | O(1) lookup | Query explain analyze |
| Snapshot table size | Unbounded | 90 days | Row count |
| User data isolation | None | Complete | Integration test |

---

## Open Questions

1. **Authentication Strategy:** How will users authenticate? Need to determine userId format.
2. **Data Migration:** Migrate existing data to "default-user" or require fresh start?
3. **Rollup Precision:** Daily granularity sufficient, or need hourly for peak hours?
4. **Streak Timezone:** Use UTC or user timezone for streak calculation?

---

## Files to Change

### Create (6 files)
| File | Purpose |
|------|---------|
| `core/application/UserContext.kt` | User context interface |
| `core/infrastructure/DefaultUserContext.kt` | Default implementation |
| `tracking/domain/DocumentCurrentState.kt` | Current state entity |
| `tracking/infrastructure/persistence/CurrentStateRepository.kt` | Repository |
| `analytics/domain/DailyReadingStats.kt` | Rollup entity |
| `analytics/domain/UserStreakCache.kt` | Streak cache entity |

### Modify (12+ files)
| File | Change |
|------|--------|
| `library/domain/Document.kt` | Add userId |
| `tracking/domain/ReadingProgressSnapshot.kt` | Add userId, remove denormalized fields |
| `tracking/domain/LocationChange.kt` | Add userId |
| `sync/domain/SyncCursor.kt` | Add userId |
| `sync/domain/SyncLog.kt` | Add userId |
| All repositories | Add userId filtering |
| `AnalyticsRepository.kt` | Use current state + rollups |
| `TrackingEventListener.kt` | Update current state |

### Database Migrations
| Migration | Description |
|-----------|-------------|
| `V1__add_user_id.sql` | Add userId columns |
| `V2__add_indexes.sql` | All query indexes |
| `V3__current_state.sql` | Current state table |
| `V4__daily_rollups.sql` | Daily stats table |
| `V5__streak_cache.sql` | Streak cache table |
