# Async Sync with Real-Time Feedback - Technical Possibilities

## Summary

Technical specification for transforming sync from synchronous/blocking to async with real-time progress streaming. This document covers what's technically possible; UI mockup will follow separately.

## Current State

| Aspect | Implementation |
|--------|----------------|
| Sync trigger | `POST /sync` (manual, user-initiated) |
| Execution | Synchronous, blocking (client waits for completion) |
| Rate limiting | 20 req/min with exponential backoff + jitter |
| Progress reporting | None - returns final status only |
| Event handling | Synchronous via `ApplicationEventPublisher` |
| Sync history | `SyncLog` entity exists, not exposed via API |

**Pain points:**
- Large libraries block for minutes (rate limit bottleneck)
- No visibility into sync progress or rate limiter state
- No feedback when hitting API limits

---

## Sync Pipeline Phases

Current pipeline (in order for referential integrity):

```
1. Documents  → DocumentSyncedEvent → Library persists Document
2. Highlights → HighlightSyncedEvent → Library persists Highlight (links to Document)
3. Notes      → NoteSyncedEvent → Library persists Note (links to Document/Highlight)
```

**Future phases** (legacy Readwise v2 API):
```
4. Books      → BookSyncedEvent (source: kindle, instapaper, pocket, etc.)
5. Highlights → LegacyHighlightSyncedEvent (links to Book)
```

Each phase can report progress independently.

---

## Technical Options

### 1. Async Execution

| Option | Pros | Cons | Effort |
|--------|------|------|--------|
| **Spring @Async + CompletableFuture** | Simple, Spring-native, well-documented | Requires thread pool tuning | Low |
| **Virtual Threads (Java 21)** | Lightweight, no pool sizing, modern | Less familiar patterns | Low |
| **Spring Scheduler (background)** | Fire-and-forget, automatic | Less control over single runs | Low |

**Recommendation:** Virtual Threads (Java 21) - we're already on Java 21, minimal config, scales naturally.

### 2. Real-Time Feedback Transport

| Option | Pros | Cons | Effort |
|--------|------|------|--------|
| **Server-Sent Events (SSE)** | Simple, unidirectional, auto-reconnect, HTTP-based | One-way only (fine for logs) | Low |
| **WebSocket** | Bidirectional, real-time | More complex, connection management | Medium |
| **Polling** | Simplest, works everywhere | Not truly real-time, wasteful | Low |

**Recommendation:** SSE - perfect for streaming logs/progress. Unidirectional is exactly what we need.

### 3. Progress State Storage

| Option | Pros | Cons | Effort |
|--------|------|------|--------|
| **Database (extend SyncLog)** | Persistent, queryable, survives restarts | Write overhead per update | Low |
| **In-memory (ConcurrentHashMap)** | Fast, no I/O | Lost on restart, single instance only | Very Low |
| **Redis** | Fast, shared across instances | New dependency, operational overhead | Medium |

**Recommendation:** Database (extend SyncLog) for durability. Batch updates every N items to reduce writes.

---

## Proposed Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend                                 │
├─────────────────────────────────────────────────────────────────┤
│  POST /sync         → Returns immediately with syncId           │
│  GET /sync/{id}/stream (SSE) → Streams progress events          │
│  GET /sync/{id}     → Returns final status (polling fallback)   │
│  GET /sync/history  → Returns past sync runs                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SyncOrchestrator                           │
├─────────────────────────────────────────────────────────────────┤
│  - Receives sync request                                        │
│  - Creates SyncRun (PENDING)                                    │
│  - Spawns async task (Virtual Thread)                           │
│  - Returns syncId immediately                                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SyncExecutor (async)                         │
├─────────────────────────────────────────────────────────────────┤
│  For each phase:                                                │
│    1. Update SyncRun status (SYNCING_DOCUMENTS, etc.)           │
│    2. Emit SyncProgressEvent                                    │
│    3. Fetch from Readwise API (with rate limit handling)        │
│    4. Emit progress every N items                               │
│    5. Publish domain events (DocumentSyncedEvent, etc.)         │
│    6. Update phase completion                                   │
│  On completion: Update SyncRun (COMPLETED/FAILED)               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  SyncProgressEmitter                            │
├─────────────────────────────────────────────────────────────────┤
│  - Maintains Map<syncId, List<SseEmitter>>                      │
│  - Listens to SyncProgressEvent                                 │
│  - Pushes to all connected SSE clients for that syncId          │
│  - Persists progress snapshots to SyncRun                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Model Extensions

### SyncRun Entity (replaces/extends SyncLog)

```kotlin
@Entity
data class SyncRun(
    @Id val id: UUID,
    val status: SyncRunStatus,           // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    val currentPhase: SyncPhase?,        // DOCUMENTS, HIGHLIGHTS, NOTES, BOOKS, etc.
    val startedAt: Instant,
    val completedAt: Instant?,

    // Progress tracking
    val totalPhases: Int,                // e.g., 3 for v3 API
    val completedPhases: Int,
    val currentPhaseProgress: Int,       // items processed in current phase
    val currentPhaseTotal: Int?,         // null if unknown (API doesn't provide total)

    // Counts (final)
    val documentsProcessed: Int,
    val highlightsProcessed: Int,
    val notesProcessed: Int,

    // Rate limit visibility
    val rateLimitHits: Int,              // times we hit 429
    val lastRateLimitAt: Instant?,

    // Error handling
    val errorMessage: String?,
    val errorPhase: SyncPhase?
)
```

### SyncPhase Enum

```kotlin
enum class SyncPhase {
    // v3 API (Reader)
    DOCUMENTS,
    HIGHLIGHTS,
    NOTES,

    // v2 API (Legacy Readwise) - future
    BOOKS,
    LEGACY_HIGHLIGHTS
}
```

### SyncRunStatus Enum

```kotlin
enum class SyncRunStatus {
    PENDING,      // Created, not started
    RUNNING,      // Currently executing
    COMPLETED,    // All phases done
    FAILED,       // Error occurred
    CANCELLED     // User cancelled (future)
}
```

---

## SSE Event Types

Events streamed to the client:

```typescript
// Phase started
{ type: "phase_started", phase: "DOCUMENTS", timestamp: "..." }

// Progress update (emitted every 10 items or on rate limit)
{ type: "progress", phase: "DOCUMENTS", processed: 50, total: null, timestamp: "..." }

// Rate limit hit
{ type: "rate_limited", retryAfter: 37, attempt: 2, maxAttempts: 3, timestamp: "..." }

// Phase completed
{ type: "phase_completed", phase: "DOCUMENTS", count: 156, timestamp: "..." }

// Sync completed
{ type: "completed", summary: { documents: 156, highlights: 42, notes: 8 }, duration: "PT2M34S" }

// Error
{ type: "error", phase: "HIGHLIGHTS", message: "...", timestamp: "..." }
```

---

## API Endpoints

### POST /sync
Triggers async sync, returns immediately.

**Response:**
```json
{
  "syncId": "uuid",
  "status": "PENDING",
  "streamUrl": "/sync/{syncId}/stream"
}
```

### GET /sync/{syncId}/stream (SSE)
Opens SSE connection, streams progress events until completion.

**Headers:**
```
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

### GET /sync/{syncId}
Returns current/final status (polling fallback).

**Response:**
```json
{
  "syncId": "uuid",
  "status": "RUNNING",
  "currentPhase": "HIGHLIGHTS",
  "progress": {
    "completedPhases": 1,
    "totalPhases": 3,
    "currentPhaseProgress": 23
  },
  "counts": {
    "documents": 156,
    "highlights": 23,
    "notes": 0
  }
}
```

### GET /sync/history
Returns past sync runs.

**Query params:** `?limit=10&offset=0`

**Response:**
```json
{
  "runs": [
    {
      "syncId": "uuid",
      "status": "COMPLETED",
      "startedAt": "...",
      "completedAt": "...",
      "duration": "PT2M34S",
      "counts": { ... }
    }
  ],
  "total": 47
}
```

### GET /sync/active
Returns currently running sync (if any). Prevents duplicate syncs.

**Response:**
```json
{
  "active": true,
  "syncId": "uuid",
  "startedAt": "...",
  "currentPhase": "DOCUMENTS"
}
```

---

## Rate Limit Visibility

Current rate limit handler already parses delay from Readwise response:
```
"Expected available in 37 seconds"
```

We can emit this to SSE:
```json
{ "type": "rate_limited", "retryAfter": 37, "attempt": 2, "maxAttempts": 3 }
```

Frontend can show: "Rate limited. Retrying in 37s (attempt 2/3)"

---

## Constraints

1. **Single active sync** - Only one sync can run at a time (mutex/check before starting)
2. **SSE connection limits** - Browser typically allows 6 concurrent SSE connections per domain
3. **Rate limit is external** - 20 req/min is Readwise-imposed, cannot be bypassed
4. **Total unknown** - Readwise API doesn't return total count upfront, only cursor-based pagination

---

## Implementation Phases

### Phase 1: Async Foundation
- Add `@EnableAsync` with Virtual Threads
- Create `SyncRun` entity (extend SyncLog)
- Make `POST /sync` return immediately
- Add `GET /sync/{syncId}` for polling

### Phase 2: SSE Streaming
- Add `GET /sync/{syncId}/stream` SSE endpoint
- Create `SyncProgressEmitter` component
- Emit progress events from `SyncService`
- Handle SSE client disconnection

### Phase 3: Enhanced Progress
- Emit rate limit events
- Add phase-level progress tracking
- Add `GET /sync/history` endpoint
- Add `GET /sync/active` endpoint

### Phase 4: Legacy API Support (Future)
- Add Books sync phase (v2 API)
- Add Legacy Highlights sync phase
- Add source filtering parameter

---

## Open Questions for UI Mockup

1. **Log verbosity** - Technical logs (every API call) vs summary logs (phase transitions only)?
2. **Progress visualization** - Progress bar per phase? Overall progress bar? Just logs?
3. **Rate limit display** - Countdown timer? Just a message? Toast notification?
4. **History view** - Full page? Sidebar? Modal?
5. **Error display** - Inline in log stream? Modal? Toast?
6. **Cancel support** - Should user be able to cancel mid-sync?
