# Async Sync with Real-Time Feedback

## Summary

Transform sync from synchronous/blocking to async with real-time progress streaming via SSE. User sees a slide-over panel with phase stepper, activity log, and rate limit feedback.

**Mockup:** `design/sync-progress-mockup.html`

---

## Current State

| Aspect | Implementation |
|--------|----------------|
| Sync trigger | `POST /sync` (manual, blocking) |
| Progress reporting | None - returns final status only |
| Rate limit feedback | None - user waits blindly |
| Sync history | `SyncLog` entity exists, not exposed via API |

---

## Sync Pipeline Phases

```
Phase 1: Documents  → DocumentSyncedEvent → Library persists Document
Phase 2: Highlights → HighlightSyncedEvent → Library persists Highlight
Phase 3: Notes      → NoteSyncedEvent → Library persists Note
```

Future (v2 API): Books, Legacy Highlights

---

## Technical Decisions

| Aspect | Choice | Rationale |
|--------|--------|-----------|
| Async execution | Virtual Threads (Java 21) | Already on Java 21, no pool tuning needed |
| Real-time transport | Server-Sent Events (SSE) | Unidirectional, auto-reconnect, HTTP-based |
| State storage | Database (extend SyncLog → SyncRun) | Persistent, queryable, survives restarts |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend                                 │
├─────────────────────────────────────────────────────────────────┤
│  POST /sync              → Returns syncId immediately           │
│  GET /sync/{id}/stream   → SSE stream of progress events        │
│  GET /sync/{id}          → Polling fallback                     │
│  GET /sync/history       → Past sync runs                       │
│  GET /sync/active        → Currently running sync               │
│  DELETE /sync/{id}       → Cancel running sync                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SyncOrchestrator                           │
├─────────────────────────────────────────────────────────────────┤
│  - Checks for active sync (mutex)                               │
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
│    1. Emit phase_started event                                  │
│    2. Fetch from Readwise API (rate limit handling)             │
│    3. Emit progress event every 10 items                        │
│    4. Emit rate_limited event when 429 received                 │
│    5. Emit phase_completed event                                │
│  On completion: Emit completed/error event                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  SyncProgressEmitter                            │
├─────────────────────────────────────────────────────────────────┤
│  - Maintains Map<syncId, List<SseEmitter>>                      │
│  - Pushes events to connected clients                           │
│  - Persists events to SyncRun for reconnection replay           │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Model

### SyncRun Entity

```kotlin
@Entity
@Table(name = "sync_runs")
data class SyncRun(
    @Id val id: UUID,

    @Enumerated(EnumType.STRING)
    val status: SyncRunStatus,

    @Enumerated(EnumType.STRING)
    val currentPhase: SyncPhase?,

    val startedAt: Instant,
    val completedAt: Instant?,

    // Phase tracking
    val totalPhases: Int = 3,
    val completedPhases: Int = 0,
    val currentPhaseProgress: Int = 0,

    // Final counts
    val documentsProcessed: Int = 0,
    val highlightsProcessed: Int = 0,
    val notesProcessed: Int = 0,

    // Rate limit tracking
    val rateLimitHits: Int = 0,
    val lastRateLimitRetrySeconds: Int? = null,
    val lastRateLimitAttempt: Int? = null,

    // Error handling
    val errorMessage: String? = null,
    val errorPhase: SyncPhase? = null
)
```

### Enums

```kotlin
enum class SyncRunStatus {
    PENDING,    // Created, not started
    RUNNING,    // Currently executing
    COMPLETED,  // All phases done
    FAILED,     // Error occurred
    CANCELLED   // User cancelled
}

enum class SyncPhase {
    DOCUMENTS,
    HIGHLIGHTS,
    NOTES
}
```

---

## SSE Events

All events include `timestamp` (ISO 8601). Frontend maps events to UI updates.

### Event Types

```typescript
// Sync started
{ type: "started", syncId: "uuid", timestamp: "..." }

// Phase started
{ type: "phase_started", phase: "DOCUMENTS", phaseNumber: 1, totalPhases: 3, timestamp: "..." }

// Progress update (every 10 items)
{ type: "progress", phase: "DOCUMENTS", processed: 50, timestamp: "..." }

// Rate limit hit - triggers banner display
{ type: "rate_limited", retryAfter: 37, attempt: 2, maxAttempts: 3, timestamp: "..." }

// Rate limit cleared - hides banner
{ type: "rate_limit_cleared", timestamp: "..." }

// Phase completed
{ type: "phase_completed", phase: "DOCUMENTS", count: 156, timestamp: "..." }

// Sync completed
{ type: "completed",
  summary: { documents: 156, highlights: 42, notes: 8 },
  duration: "PT2M34S",
  timestamp: "..."
}

// Sync failed
{ type: "error", phase: "HIGHLIGHTS", message: "Max retries exceeded", timestamp: "..." }

// Sync cancelled
{ type: "cancelled", phase: "HIGHLIGHTS", timestamp: "..." }
```

### Event → UI Mapping

| Event | UI Update |
|-------|-----------|
| `started` | Open panel, show "Running" badge |
| `phase_started` | Update stepper: activate phase icon (spinner) |
| `progress` | Update phase count ("42 of ~100"), update overall progress % |
| `rate_limited` | Show rate limit banner with countdown |
| `rate_limit_cleared` | Hide rate limit banner |
| `phase_completed` | Update stepper: checkmark icon, green connector |
| `completed` | All checkmarks, 100% progress, show footer success |
| `error` | Red X on failed phase, show footer error |
| `cancelled` | Gray out remaining phases, show cancelled state |

---

## API Endpoints

### POST /sync
Trigger async sync. Returns immediately.

**Response (201 Created):**
```json
{
  "syncId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "streamUrl": "/sync/550e8400-e29b-41d4-a716-446655440000/stream"
}
```

**Error (409 Conflict):** Sync already running
```json
{
  "error": "SYNC_IN_PROGRESS",
  "activeSyncId": "...",
  "message": "A sync is already in progress"
}
```

### GET /sync/{syncId}/stream
SSE stream. Replays missed events on reconnect.

**Headers:**
```
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

**Query param:** `?lastEventId=5` (for reconnection)

### GET /sync/{syncId}
Polling fallback. Returns current state.

**Response:**
```json
{
  "syncId": "...",
  "status": "RUNNING",
  "currentPhase": "HIGHLIGHTS",
  "startedAt": "2024-12-20T10:23:05Z",
  "completedAt": null,
  "progress": {
    "completedPhases": 1,
    "totalPhases": 3,
    "currentPhaseProgress": 42,
    "overallPercent": 45
  },
  "counts": {
    "documents": 156,
    "highlights": 42,
    "notes": 0
  },
  "rateLimit": {
    "isLimited": true,
    "retryAfter": 37,
    "attempt": 2,
    "maxAttempts": 3
  },
  "duration": "PT2M34S"
}
```

### GET /sync/active
Check for running sync.

**Response (sync running):**
```json
{
  "active": true,
  "syncId": "...",
  "startedAt": "...",
  "currentPhase": "DOCUMENTS",
  "streamUrl": "/sync/.../stream"
}
```

**Response (no sync):**
```json
{
  "active": false
}
```

### DELETE /sync/{syncId}
Cancel running sync.

**Response (200 OK):**
```json
{
  "syncId": "...",
  "status": "CANCELLED",
  "cancelledAt": "..."
}
```

### GET /sync/history
Past sync runs for history panel.

**Query params:** `?limit=20&offset=0`

**Response:**
```json
{
  "runs": [
    {
      "syncId": "...",
      "status": "COMPLETED",
      "startedAt": "2024-12-20T10:23:05Z",
      "completedAt": "2024-12-20T10:25:39Z",
      "duration": "PT2M34S",
      "counts": {
        "documents": 156,
        "highlights": 42,
        "notes": 8
      },
      "errorMessage": null
    },
    {
      "syncId": "...",
      "status": "FAILED",
      "startedAt": "2024-12-19T23:30:00Z",
      "completedAt": "2024-12-19T23:30:45Z",
      "duration": "PT0M45S",
      "counts": {
        "documents": 156,
        "highlights": 0,
        "notes": 0
      },
      "errorMessage": "Rate limit exceeded (max retries)"
    }
  ],
  "summary": {
    "total": 47,
    "successful": 45,
    "failed": 2
  },
  "pagination": {
    "limit": 20,
    "offset": 0,
    "hasMore": true
  }
}
```

---

## UI Components

### Sync Button (Header)

| State | Display |
|-------|---------|
| Idle | "Sync" + refresh icon |
| Running | "Syncing..." + spinning icon |

Click opens slide-over panel (or starts sync if idle).

### Slide-Over Panel

**Width:** `max-w-lg` (~512px)
**Position:** Right edge, full height
**Backdrop:** Semi-transparent overlay

#### Header
- Title: "Syncing with Readwise" / "Sync Complete" / "Sync Failed"
- Status badge: Running (blue), Rate Limited (amber), Completed (green), Failed (red)
- Close button (X)

#### Phase Stepper
Horizontal 3-step indicator with connectors:

| Phase State | Icon | Color | Label Color |
|-------------|------|-------|-------------|
| Pending | Number | Gray | Gray |
| Running | Spinner (animated) | Accent | Accent |
| Completed | Checkmark | Emerald | Emerald |
| Failed | X | Red | Red |
| Skipped | Number | Gray | Gray ("Skipped") |

**Connector colors:**
- Before current phase: Emerald (solid)
- After current phase: Gray

**Phase counts:**
- Pending: "Pending"
- Running: "42 of ~100" (estimate based on previous sync)
- Completed: "156 synced"
- Failed: "Failed"
- Skipped: "Skipped"

#### Overall Progress Bar
- Animated striped pattern when running
- Solid when completed
- Red when failed
- Percentage label: "45%"

#### Rate Limit Banner
**Shown when:** `rate_limited` event received
**Hidden when:** `rate_limit_cleared` event received

Content:
- Clock icon
- "Rate limit reached"
- "Retrying in **37**s (attempt 2/3)"

Countdown updates every second (client-side).

#### Activity Log
Scrollable log stream with:
- **Verbose toggle:** Checkbox to show/hide progress events
- **Entry format:** `HH:MM:SS` | Icon | Message

| Log Type | Icon | Color |
|----------|------|-------|
| info | Info circle | Gray |
| phase | Lightning bolt | Blue |
| progress | Chevron right | Gray (hidden if !verbose) |
| success | Checkmark | Emerald |
| warning | Warning triangle | Amber |
| error | X | Red |
| complete | Checkmark | Emerald (bold) |

#### Footer

| State | Left | Right |
|-------|------|-------|
| Running | "Started 2m 34s ago" | Cancel Sync button |
| Completed | Success icon + "Duration: 2m 34s" | Done button |
| Failed | Error icon + "Check error above" | Retry Sync button |
| Cancelled | "Sync cancelled" | Close button |

### History Panel

Separate slide-over with:

**Summary stats:**
- Total syncs
- Successful (green)
- Failed (red)

**Grouped list:**
- Today
- Yesterday
- Last 7 Days

**Entry format:**
- Status icon (checkmark/X)
- Status text + duration
- Item counts: "156 docs • 42 highlights • 8 notes"
- Timestamp

Click entry → opens sync detail panel (reuses main panel in completed/failed state)

---

## Frontend Implementation

### Angular Service

```typescript
@Injectable({ providedIn: 'root' })
export class SyncService {
  private eventSource: EventSource | null = null;

  readonly syncState = signal<SyncState>({ status: 'idle' });
  readonly logs = signal<LogEntry[]>([]);

  startSync(): Observable<SyncStartResponse> {
    return this.http.post<SyncStartResponse>('/sync', {}).pipe(
      tap(response => {
        this.connectToStream(response.syncId);
        this.syncState.set({ status: 'running', syncId: response.syncId });
      })
    );
  }

  private connectToStream(syncId: string, lastEventId?: number): void {
    const url = lastEventId
      ? `/sync/${syncId}/stream?lastEventId=${lastEventId}`
      : `/sync/${syncId}/stream`;

    this.eventSource = new EventSource(url);

    this.eventSource.onmessage = (event) => {
      const data = JSON.parse(event.data);
      this.handleEvent(data);
    };

    this.eventSource.onerror = () => {
      // Auto-reconnect with lastEventId
      setTimeout(() => this.connectToStream(syncId, this.lastEventId), 1000);
    };
  }

  cancelSync(syncId: string): Observable<void> {
    return this.http.delete<void>(`/sync/${syncId}`);
  }

  getHistory(limit = 20, offset = 0): Observable<SyncHistoryResponse> {
    return this.http.get<SyncHistoryResponse>('/sync/history', {
      params: { limit, offset }
    });
  }

  getActive(): Observable<ActiveSyncResponse> {
    return this.http.get<ActiveSyncResponse>('/sync/active');
  }
}
```

### State Management

```typescript
interface SyncState {
  status: 'idle' | 'running' | 'completed' | 'failed' | 'cancelled';
  syncId?: string;
  currentPhase?: SyncPhase;
  completedPhases: number;
  totalPhases: number;
  overallPercent: number;
  counts: { documents: number; highlights: number; notes: number };
  rateLimit?: { retryAfter: number; attempt: number; maxAttempts: number };
  duration?: string;
  errorMessage?: string;
}

interface LogEntry {
  id: number;
  type: 'info' | 'phase' | 'progress' | 'success' | 'warning' | 'error' | 'complete';
  timestamp: string;
  message: string;
}
```

---

## Backend Implementation

### Files to Create

```
backend/src/main/kotlin/com/reader/analytics/
├── sync/
│   ├── domain/
│   │   ├── SyncRun.kt              # Entity (replaces SyncLog for new syncs)
│   │   ├── SyncRunStatus.kt        # Enum
│   │   ├── SyncPhase.kt            # Enum
│   │   └── events/
│   │       └── SyncProgressEvent.kt # Internal event for SSE emission
│   ├── application/
│   │   ├── SyncOrchestrator.kt     # Starts async sync, checks mutex
│   │   ├── SyncExecutor.kt         # Async execution logic
│   │   ├── SyncProgressEmitter.kt  # SSE emitter management
│   │   └── SyncRunStore.kt         # Repository interface
│   └── infrastructure/
│       └── persistence/
│           ├── SyncRunRepository.kt
│           └── JpaSyncRunStore.kt
└── api/
    ├── SyncController.kt           # Update with new endpoints
    └── dto/
        ├── SyncStartResponse.kt
        ├── SyncStatusResponse.kt
        ├── SyncHistoryResponse.kt
        └── ActiveSyncResponse.kt
```

### Files to Modify

```
backend/src/main/kotlin/com/reader/analytics/
├── sync/
│   ├── application/
│   │   └── SyncService.kt          # Extract execution logic to SyncExecutor
│   └── infrastructure/
│       └── readwise/
│           └── RateLimitRetryHandler.kt  # Emit events on retry
└── config/
    └── AsyncConfig.kt              # New: @EnableAsync + Virtual Thread executor
```

---

## Database Migration

```sql
-- V4__create_sync_runs.sql

CREATE TABLE sync_runs (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    current_phase VARCHAR(20),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    total_phases INT NOT NULL DEFAULT 3,
    completed_phases INT NOT NULL DEFAULT 0,
    current_phase_progress INT NOT NULL DEFAULT 0,
    documents_processed INT NOT NULL DEFAULT 0,
    highlights_processed INT NOT NULL DEFAULT 0,
    notes_processed INT NOT NULL DEFAULT 0,
    rate_limit_hits INT NOT NULL DEFAULT 0,
    last_rate_limit_retry_seconds INT,
    last_rate_limit_attempt INT,
    error_message TEXT,
    error_phase VARCHAR(20)
);

CREATE INDEX idx_sync_runs_status ON sync_runs(status);
CREATE INDEX idx_sync_runs_started_at ON sync_runs(started_at DESC);
```

---

## Constraints

1. **Single active sync** - 409 Conflict if sync already running
2. **SSE connection limits** - Browser allows ~6 per domain
3. **Rate limit is external** - 20 req/min from Readwise, cannot bypass
4. **Total unknown** - Readwise API uses cursor pagination, no total count

---

## Implementation Phases

### Phase 1: Async Foundation
- [ ] Add `AsyncConfig` with Virtual Thread executor
- [ ] Create `SyncRun` entity + migration
- [ ] Create `SyncOrchestrator` (mutex check, spawn async)
- [ ] Create `SyncExecutor` (move logic from SyncService)
- [ ] Update `POST /sync` to return immediately
- [ ] Add `GET /sync/{syncId}` polling endpoint
- [ ] Add `GET /sync/active` endpoint

### Phase 2: SSE Streaming
- [ ] Add `SyncProgressEmitter` component
- [ ] Add `GET /sync/{syncId}/stream` SSE endpoint
- [ ] Emit events from `SyncExecutor` at key points
- [ ] Handle SSE client disconnection/reconnection
- [ ] Add `lastEventId` support for replay

### Phase 3: UI Implementation
- [ ] Create `SyncPanelComponent` (slide-over)
- [ ] Create `PhaseStepperComponent`
- [ ] Create `ActivityLogComponent`
- [ ] Update `SyncService` with SSE handling
- [ ] Wire up sync button states

### Phase 4: Enhanced Features
- [ ] Add `DELETE /sync/{syncId}` cancel endpoint
- [ ] Add `GET /sync/history` endpoint
- [ ] Create `SyncHistoryPanelComponent`
- [ ] Emit rate limit events from `RateLimitRetryHandler`
- [ ] Add rate limit banner with client-side countdown

### Phase 5: Polish
- [ ] Verbose toggle for progress logs
- [ ] Duration timer (client-side)
- [ ] Reconnection handling
- [ ] Error boundary / fallback UI
