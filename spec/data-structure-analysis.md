# Data Structure Analysis: Library & Analytics Contexts

## Executive Summary

Analysis of current data models for scalability to **500-1000 users** with **~3,000 documents + highlights each**. Found critical issues around missing multi-tenancy, unbounded time-series growth, missing indexes, and inefficient join patterns.

**Projected Scale:**
| Entity | Per User | Total (1000 users) |
|--------|----------|-------------------|
| Documents | ~3,000 | 3M |
| Highlights | ~15,000 | 15M |
| Notes | ~3,000 | 3M |
| ReadingProgressSnapshot | ~30,000* | 30M |
| LocationChange | ~6,000 | 6M |

*Assuming 10 progress updates per document on average

---

## Current Entity Model

```
┌─────────────────────────────────────────────────────────────────┐
│                         Library Context                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Document ←──────── Highlight ←──────── Note                    │
│     │                    │                 │                     │
│     │ readwiseId (String)│ readwiseId     │ readwiseId          │
│     │ ManyToMany: tags   │ ManyToOne: doc │ ManyToOne: doc/hl   │
│     └────────────────────┴─────────────────┴────────────────────┤
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│                        Tracking Context                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ReadingProgressSnapshot          LocationChange                 │
│     documentId: String               documentId: String          │
│     readingProgress: Double          fromLocation: String?       │
│     wordCount: Int?                  toLocation: String          │
│     firstOpenedAt: Instant?          changedAt: Instant          │
│     lastOpenedAt: Instant?           category: String?           │
│     recordedAt: Instant                                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Issue Analysis

### 1. Missing Multi-Tenancy (Critical)

**Problem:** No `userId` field on any entity. Currently single-tenant.

**Impact:**
- Cannot scale to 1000 users without full redesign
- No data isolation between users
- Queries return all users' data

**Affected Entities:**
- `Document` - needs `userId`
- `Highlight` - inherits via Document FK (already correct)
- `Note` - inherits via Document/Highlight FK (already correct)
- `ReadingProgressSnapshot` - needs `userId`
- `LocationChange` - needs `userId`
- `SyncCursor` - needs `userId`
- `SyncLog` - needs `userId`

### 2. Unbounded Time-Series Growth (Critical)

**Problem:** `ReadingProgressSnapshot` creates a new row on every sync when progress changes. No retention policy or rollup.

**Current Behavior:**
```
Sync 1: doc-1 progress 0.10 → new row
Sync 2: doc-1 progress 0.15 → new row
Sync 3: doc-1 progress 0.25 → new row
... (10 syncs) ...
```

**Projected Growth (1000 users):**
- Year 1: 30M rows
- Year 2: 60M rows
- Year 3: 90M rows

**Impact:**
- Query performance degrades O(n) with data growth
- Storage costs grow linearly
- Window functions (`LAG`, `DISTINCT ON`) become increasingly expensive

### 3. Missing Database Indexes (High)

**Problem:** No indexes defined on JPA entities. All analytics queries perform full table scans.

**Missing Indexes:**
```sql
-- ReadingProgressSnapshot (most critical)
CREATE INDEX idx_rps_document_recorded ON reading_progress_snapshots(document_id, recorded_at);
CREATE INDEX idx_rps_recorded_at ON reading_progress_snapshots(recorded_at);

-- LocationChange
CREATE INDEX idx_lc_document_changed ON location_changes(document_id, changed_at);

-- Document
CREATE INDEX idx_doc_readwise_id ON documents(readwise_id);
CREATE INDEX idx_doc_location ON documents(location);
CREATE INDEX idx_doc_saved_at ON documents(saved_at);

-- Highlight
CREATE INDEX idx_hl_highlighted_at ON highlights(highlighted_at);
```

**Impact at 30M rows:**
- `getReadingStatsByPeriod()`: Full scan + sort
- `getReadingStreak()`: Full scan + multiple window functions
- `getPipelineStats()`: Full scan with `DISTINCT ON`

### 4. String-Based Foreign Keys (High)

**Problem:** Tracking entities use `documentId: String` instead of FK to Document.

**Current:**
```kotlin
// ReadingProgressSnapshot
val documentId: String,  // References Document.readwiseId as string

// Analytics queries
JOIN documents d ON d.readwise_id = rps.document_id  // String comparison
```

**Issues:**
- No referential integrity - orphaned snapshots possible
- String comparison slower than UUID join
- No cascade delete when document removed

### 5. Data Denormalization in Snapshots (Medium)

**Problem:** `ReadingProgressSnapshot` duplicates data from `Document`:

```kotlin
data class ReadingProgressSnapshot(
    val wordCount: Int?,        // Duplicated from Document.wordCount
    val firstOpenedAt: Instant?, // Duplicated from Document.firstOpenedAt
    val lastOpenedAt: Instant?,  // Duplicated from Document.lastOpenedAt
)
```

**Issues:**
- 30M rows × 3 extra columns = unnecessary storage
- Potential inconsistency between snapshot and document values
- `wordCount` never changes per document - pure waste

**Why it exists:** Analytics queries need wordCount for "words read" calculation. Currently avoids JOIN by denormalizing.

### 6. No Pre-Aggregated Metrics (Medium)

**Problem:** All analytics computed at query time via complex CTEs and window functions.

**Current Pattern:**
```sql
WITH progress_deltas AS (
    SELECT document_id, reading_progress,
           reading_progress - LAG(reading_progress) OVER (
               PARTITION BY document_id ORDER BY recorded_at
           ) AS progress_delta
    FROM reading_progress_snapshots
    WHERE recorded_at >= ? AND recorded_at < ?
)
SELECT SUM(progress_delta * word_count) AS words_read
FROM progress_deltas
WHERE progress_delta > 0
```

**Issues:**
- Same expensive computation runs on every dashboard load
- No caching layer
- Window functions don't parallelize well

### 7. Inconsistent Progress Tracking (Medium)

**Problem:** Reading progress stored in snapshots, but location stored on Document.

| Field | Stored On | Why? |
|-------|-----------|------|
| `readingProgress` | ReadingProgressSnapshot | "Need history" |
| `location` | Document | "Just current state" |
| `firstOpenedAt` | Both | Inconsistent |
| `lastOpenedAt` | Both | Inconsistent |

**Result:** To get current document state, must JOIN Document with latest snapshot.

### 8. Eager Loading Tags (Low)

**Problem:** Document has `FetchType.EAGER` for tags.

```kotlin
@ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.EAGER)
val tags: MutableSet<Tag> = mutableSetOf()
```

**Impact:**
- Every Document query loads all tags
- N+1 potential when iterating documents
- Most analytics queries don't need tags

---

## Query Performance Analysis

### Most Expensive Queries

| Query | Current Cost | Issue |
|-------|-------------|-------|
| `getReadingStatsByPeriod` | O(n) full scan | No index on recorded_at, window function |
| `getReadingStreak` | O(n) full scan | Multiple window functions, no index |
| `getPipelineStats` | O(n) DISTINCT ON | Expensive deduplication |
| `getCategoryBreakdown` | O(n) + O(n) | Double full scan (documents + snapshots) |

### "Latest Progress" Anti-Pattern

This CTE appears in 6+ queries:
```sql
WITH latest_progress AS (
    SELECT DISTINCT ON (document_id)
        document_id, reading_progress
    FROM reading_progress_snapshots
    ORDER BY document_id, recorded_at DESC
)
```

**At 30M rows:** This CTE alone could take seconds.

---

## Schema Consistency Issues

### 1. Nullable `id` Pattern

```kotlin
val id: UUID? = null,  // Always null until persisted
```

**Issue:** Forces null checks everywhere. Consider generated strategy changes or wrapper types.

### 2. Mixed Mutability

```kotlin
// Document - immutable (data class, val)
data class Document(val id: UUID?, val title: String?)

// Highlight - mutable (class, var)
class Highlight(var id: UUID?, var document: Document)
```

**Issue:** Inconsistent patterns make refactoring harder.

### 3. Note Ownership Validation

```kotlin
init {
    require((document != null) xor (highlight != null)) {
        "Note must have exactly one parent"
    }
}
```

**Issue:** Runtime validation for what could be type-level constraint. Two separate entities (`DocumentNote`, `HighlightNote`) would be safer.

---

## Recommendations Summary

| Priority | Issue | Recommendation |
|----------|-------|----------------|
| P0 | Missing multi-tenancy | Add userId to all entities, partition indexes |
| P0 | Unbounded snapshot growth | Implement rollup (daily aggregates) + retention |
| P1 | Missing indexes | Add composite indexes on query patterns |
| P1 | String-based FKs | Migrate to UUID FKs with proper constraints |
| P2 | Query-time aggregation | Add `daily_reading_stats` materialized view or table |
| P2 | Denormalized snapshots | Remove wordCount/timestamps from snapshots |
| P3 | Eager tag loading | Switch to LAZY, use JOIN FETCH when needed |

See `spec/data-scalability-improvements.md` for implementation spec.
