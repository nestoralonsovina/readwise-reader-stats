# Project Hygiene: Agent Docs and Spec Scaffold

## Summary

The project's AI-facing documentation (AGENTS.md, backend/CLAUDE.md, readwise-analytics/CLAUDE.md) is stale and incomplete after the async sync refactor and drill-down/document-detail features. There is also no spec template or project scaffold for creating new features consistently. This spec brings agent docs up to date and introduces a repeatable template for new specs.

---

## Problem Statement

### 1. Stale Agent Documentation

All three AI-facing docs describe the system as it was before the async sync and drill-down implementations. This causes AI assistants to generate code against outdated patterns.

**Root AGENTS.md issues:**
- Architecture table lists `SyncLog` instead of `SyncRun` 
- Only documents `POST /sync` — omits `GET /sync/{syncId}`, `GET /sync/{syncId}/stream`, `GET /sync/active`, and planned `DELETE /sync/{syncId}`
- Omits drill-down endpoints (`GET /api/analytics/drill-down/*`)
- Omits document detail endpoint (`GET /api/documents/{id}`)
- No mention of SSE, async sync, or the SyncRun entity

**Backend CLAUDE.md issues:**
- Lists `SyncService` as "orchestrates sync" — it's dead code; `SyncOrchestrator` is the orchestrator
- Sync Context table omits `SyncRun`, `SyncPhase`, `SyncRunStatus`, `SyncProgressEvent`, `SyncProgressEmitter`, `RateLimitRetryHandler`
- No mention of `DocumentService`, `DrillDownService`, `HighlightStore` with notes
- Omits `LogStore` (even though it still exists and should be documented for the planned cleanup)
- Testing section says "Tests before implementation" but doesn't mention `@DataJpaTest` pattern or the fakes in `testFixtures`

**Frontend CLAUDE.md issues:**
- Directory structure is outdated — missing `features/sync/`, `features/library/`, `core/models/sync.models.ts`
- Services list only shows `analytics.service.ts`, `sync.service.ts`, `theme.service.ts` — missing `document.service.ts`, `drill-down.service.ts`, `chart-colors.service.ts`
- Service description says `sync.service.ts` does `POST /sync` — it now also handles SSE streaming and cancel
- Missing feature modules: `library/` (document detail, highlight list), `sync/` (sync panel, phase stepper, activity log, rate limit banner, sync footer)
- Missing shared components: `document-row/`, `reading-timeline/`, `stat-drill-down-sheet/`
- Missing `palette-selector/` (empty directory — should be noted or removed)
- No mention of Spartan Sheet (used for drill-down slide-overs)

### 2. No Spec Template

Specs are hand-written in different styles. There's no starting template ensuring consistent structure:
- Some specs skip sections (e.g., `highlights-note-stats.md` has no "Out of Scope")
- Phase numbering varies between specs
- No `CONTRIBUTING.md` or `AGENTS.md` section describing how to write a new spec
- No template file to `cp spec/template.md spec/my-feature.md`

### 3. Empty Directory

`shared/components/palette-selector/` in the frontend exists as an empty directory with no files.

---

## Implementation

### Phase 1: Update Root AGENTS.md

Rewrite `/AGENTS.md` to reflect the current state of the project.

**Architecture table — update:**

```markdown
| Context | Purpose | Key Entities |
|---------|---------|--------------|
| Sync | Readwise API integration, async orchestration | SyncRun, SyncPhase, SyncCursor, SyncProgressEvent |
| Library | Document storage | Document, Highlight, Note, Tag |
| Tracking | Progress time-series | ReadingProgressSnapshot, LocationChange |
| Analytics | Query-time metrics | Projections via PostgreSQL |
```

**API Endpoints — expand:**

```markdown
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
```

**Add section — Design Specs:**

```markdown
## Design Specs

Specs are in `spec/` as named markdown files. Each spec follows the template in `spec/template.md`. See `spec/template.md` for the expected structure.
```

**Add section — Testing:**

```markdown
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
```

### Phase 2: Update Backend CLAUDE.md

Rewrite `/backend/CLAUDE.md`.

**Sync Context table — replace entirely:**

```markdown
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
```

**Library Context — update:**

```markdown
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
```

**Analytics Context — update:**

```markdown
### Analytics Context
Query-time computation via native PostgreSQL.

| Component | Purpose |
|-----------|---------|
| `AnalyticsService` | Facade for all metrics |
| `AnalyticsRepository` | Native SQL with window functions |
| `JpaAnalyticsStore` | Adapter implementing `AnalyticsStore` |
```

**Testing section — expand:**

```markdown
## Testing

- **Unit tests**: Fakes for stores (`FakeAnalyticsStore`, `FakeDocumentStore`, etc.) in `src/testFixtures`
- **Repository tests**: `@DataJpaTest` with H2 for JPA queries
- **Integration tests**: `@SpringBootTest` for event listeners and full flow
- **TDD**: Write tests before implementation
- Run: `./gradlew test` or `./gradlew test --tests "ClassName.method"`
```

**Key Patterns — update:**

```markdown
**Async sync**: `SyncOrchestrator.startSync()` creates `SyncRun`, spawns `SyncExecutorImpl` on virtual thread. Progress streamed via SSE (`SyncProgressEmitter`). Cancel via `SyncOrchestrator.cancel()`.

**Event-driven**: Sync publishes domain events, other contexts subscribe. No coupling.

**Port/Adapter**: Application layer defines interfaces (`SyncRunStore`, `DocumentStore`, `TrackingStore`, `AnalyticsStore`), infrastructure implements.

**Query-time analytics**: No stored aggregates. PostgreSQL window functions (`LAG`, `ROW_NUMBER`, `DISTINCT ON`) for calculations.

**Cursor pagination**: Drill-down endpoints use keyset pagination (not offset) for stable paging through large result sets.
```

### Phase 3: Update Frontend CLAUDE.md

Rewrite `/readwise-analytics/CLAUDE.md`.

**Directory structure — update to match current tree:**

```markdown
## Directory Structure

```
src/app/
├── core/
│   ├── layout/
│   │   └── shell.component.ts          # Main layout with sidebar
│   ├── models/
│   │   ├── api.models.ts              # Dashboard, drill-down, document detail types
│   │   └── sync.models.ts             # SSE event types and sync state
│   └── services/
│       ├── analytics.service.ts        # GET /api/analytics/*
│       ├── chart-colors.service.ts     # Chart color palette (light/dark)
│       ├── document.service.ts         # GET /api/documents/:id
│       ├── drill-down.service.ts       # GET /api/analytics/drill-down/*
│       ├── sync.service.ts            # POST /sync, SSE stream, cancel
│       └── theme.service.ts           # Dark mode toggle
├── features/
│   ├── dashboard/
│   │   ├── dashboard.component.ts      # Container with signal state
│   │   └── components/
│   │       ├── dashboard-header/
│   │       ├── dashboard-footer/
│   │       ├── reading-activity-chart/  # ApexCharts area+line
│   │       ├── pipeline-card/           # Progress bars
│   │       ├── highlights-card/         # Donut chart
│   │       └── most-highlighted/        # Article list
│   ├── library/
│   │   ├── document-detail.component.ts # Document detail page
│   │   └── components/
│   │       ├── highlight-list.component.ts
│   │       └── highlight-item.component.ts
│   └── sync/
│       ├── sync-panel.component.ts      # Spartan Sheet slide-over
│       └── components/
│           ├── phase-stepper.component.ts
│           ├── activity-log.component.ts
│           ├── rate-limit-banner.component.ts
│           └── sync-footer.component.ts
└── shared/
    ├── components/
    │   ├── sidebar/                     # Navigation with route links
    │   ├── kpi-card/                    # Metric card with sparkline
    │   ├── streak-bar/                  # 14-day streak visualization
    │   ├── period-toggle/               # Week/Month/Year selector
    │   ├── reading-timeline/            # Document reading progress timeline
    │   ├── document-row/                # Row for drill-down lists
    │   ├── stat-drill-down-sheet/       # Spartan Sheet slide-over
    │   └── coming-soon-badge/           # Feature availability indicator
    └── pipes/
        └── format-number.pipe.ts        # 15400 → "15.4K"
```
```

**Services section — update:**

```markdown
## Services

| Service | Endpoints |
|---------|-----------|
| `AnalyticsService` | `GET /api/analytics/dashboard`, `reading/stats`, `reading/streak`, `reading/peak-hours`, `pipeline`, `highlights` |
| `DrillDownService` | `GET /api/analytics/drill-down/words-read`, `drill-down/completed`, `drill-down/backlog` |
| `DocumentService` | `GET /api/documents/:id` |
| `SyncService` | `POST /sync`, SSE `/sync/:id/stream`, `GET /sync/active`, `DELETE /sync/:id` (cancel) |
| `ChartColorsService` | Local palette generation (no API) |
| `ThemeService` | Local dark mode (no API) |
```

**Add section — SSE Pattern:**

```markdown
## SSE Pattern

`SyncService` connects to `/sync/{id}/stream` and dispatches typed events:

```typescript
const eventMap: Record<SyncEventType, (data: any) => void> = {
  phase_started: (d) => this.onPhaseStarted(d),
  progress: (d) => this.onProgress(d),
  phase_completed: (d) => this.onPhaseCompleted(d),
  rate_limited: (d) => this.onRateLimited(d),
  completed: (d) => this.onCompleted(d),
  error: (d) => this.onError(d),
  cancelled: (d) => this.onCancelled(d),
};
```

State managed with Angular Signals. Reconnection handled via `lastEventId`.
```

### Phase 4: Create Spec Template

**File:** `spec/template.md`

```markdown
# [Feature Name]

## Summary

1-3 sentences describing what this spec covers and why.

---

## Problem Statement

Describe the current gap, bug, or missing capability. Use tables or numbered lists with **bold issue titles**.

---

## Technical Decisions

| Aspect | Choice | Rationale |
|--------|--------|-----------|
| ... | ... | ... |

---

## Architecture

ASCII diagram showing component relationships and data flow.

---

## Data Model

Entity changes, new tables, new columns. Include Kotlin data classes or SQL DDL.

---

## API Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| ... | ... | ... | ... |

Request/response JSON examples.

---

## Frontend Implementation

Component tree, service changes, routing changes. Include Spartan UI component mappings.

| Element | Spartan Import |
|---------|---------------|
| ... | ... |

---

## Backend Implementation

New/modified files organized by bounded context. Include method signatures and key logic.

---

## Implementation Phases

### Phase 1: [Name]
- [ ] Task 1
- [ ] Task 2

### Phase 2: [Name]
- [ ] Task 1
- [ ] Task 2

---

## Testing

### Unit Tests
1. `test name` — expected behavior

### Integration Tests
1. `test name` — expected behavior

---

## Files to Create / Modify

```
path/to/
├── new-file.kt      # Purpose
└── modified-file.ts  # What changed
```

| File | Purpose |
|------|---------|
| ... | ... |

---

## Trade-offs

| Decision | Trade-off |
|----------|-----------|
| ... | ... |

---

## Out of Scope

- Items explicitly excluded from this spec
```

### Phase 5: Remove Empty Directory

Delete `readwise-analytics/src/app/shared/components/palette-selector/` — empty directory with no component files.

---

## Files to Modify

| File | Change |
|------|--------|
| `/AGENTS.md` | Full rewrite — architecture table, endpoints, testing section, spec reference |
| `/backend/CLAUDE.md` | Full rewrite — sync context, services, patterns, testing |
| `/readwise-analytics/CLAUDE.md` | Full rewrite — directory structure, services, SSE pattern |
| `/spec/template.md` | New file — spec template |
| `readwise-analytics/src/app/shared/components/palette-selector/` | Delete empty directory |

---

## Out of Scope

- Deployment configuration (Netlify, Docker, etc.)
- New content or blog posts
- Changes to actual application code or features
- `.github/` issue/PR templates (separate concern)