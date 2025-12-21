# Dashboard Drill-Down Feature

## Summary

Add drill-down capability to dashboard stat cards and document navigation, allowing users to explore the underlying data behind aggregate metrics.

## Problem Statement

The current dashboard displays aggregate statistics (words read, articles completed, backlog size) but provides no way to explore which documents contribute to these numbers. Users see "47,832 words read" but cannot answer "which articles did I read?" or "what's sitting in my backlog the longest?"

**Current state:** Static dashboard with display-only metrics
**Target state:** Interactive dashboard where every stat links to its underlying data

## User Stories

1. **Stat Exploration:** As a user, I want to click on "Words Read" to see which documents contributed those words this month
2. **Completion Review:** As a user, I want to click on "Completed" to see my recently finished articles with completion dates
3. **Backlog Management:** As a user, I want to click on "Backlog" to see my oldest unread items so I can prioritize
4. **Document Deep Dive:** As a user, I want to view a document's highlights and notes in full context
5. **Navigation Flow:** As a user, I want seamless navigation: Dashboard → Stat drill-down → Document detail → Back to dashboard

---

## UI Components

### 1. Slide-Over Panel

A right-side panel that slides in when clicking a stat card, showing the documents that contribute to that metric.

**Trigger:** Click on KPI cards (Words Read, Completed, Backlog)

**Layout:**
```
┌─────────────────────────────────────┐
│ [Title]                        [X]  │  ← Header with close button
├─────────────────────────────────────┤
│ 47,832        +12.4%                │  ← Summary stats
│ Total words   vs last period        │
├─────────────────────────────────────┤
│ 12 documents contributed            │  ← Count label
├─────────────────────────────────────┤
│ ┌─────┐ Title                 8,432 │
│ │ img │ source.com             Done │  ← Document row
│ └─────┘                             │
├─────────────────────────────────────┤
│ ┌─────┐ Title                 6,218 │
│ │ img │ source.com              72% │  ← Progress indicator
│ └─────┘ ████████░░░░               │
└─────────────────────────────────────┘
```

**Content varies by stat type:**

| Stat Card | Panel Title | Value Column | Sort Order |
|-----------|-------------|--------------|------------|
| Words Read | Words Read | Word count | By words (desc) |
| Completed | Articles Completed | Completion date | By date (desc) |
| Backlog | Reading Backlog | Days waiting | By age (desc) |

**Behavior (handled by Spartan Sheet):**
- Width: 400px (`max-w-md`)
- Slides in from right with built-in animation
- Backdrop: semi-transparent overlay (click to close)
- Close: X button, Escape key, or backdrop click
- Scroll: Document list scrolls independently
- Body scroll locked when open
- Focus trap managed automatically

### 2. Document Detail Page

Full-page view showing a document with all its highlights, notes, and metadata.

**Route:** `/library/:documentId`

**Layout:**
```
┌─────────────────────────────────────────────────────────────┐
│ Dashboard > Library > [Document Title]      [Open in Reader]│  ← Breadcrumb + action
├─────────────────────────────────────────────────────────────┤
│ ┌──────┐                                                    │
│ │      │  Document Title                                    │
│ │ cover│  by Author                                         │
│ │      │  source.com • Archive • Article                    │
│ └──────┘                                                    │
│          Reading Progress ████████████████████░░ 85%        │
│          #tag1  #tag2  #tag3                                │
├─────────────────────────────────────────────────────────────┤
│  4,832      24           8           18m                    │
│  Words    Highlights  With Notes  Reading Time              │  ← Stats row
├─────────────────────────────────────────────────────────────┤
│  Reading Timeline                                           │
│  Saved: Nov 12 │ First Opened: Nov 14 │ Completed: Nov 18   │
├─────────────────────────────────────────────────────────────┤
│  Highlights                                      [With notes]│
├─────────────────────────────────────────────────────────────┤
│  ┃ "Highlighted text passage here..."                       │
│  ┃                                                  Nov 14  │
│  ├──────────────────────────────────────────────────────────┤
│  │ 💬 User's note about this highlight                      │
│  └──────────────────────────────────────────────────────────┘
│                                                             │
│  ┃ "Another highlighted passage..."                         │
│  ┃                                                  Nov 15  │
└─────────────────────────────────────────────────────────────┘
```

**Sections:**
1. **Header:** Cover image, title, author, source, location badge, category
2. **Progress:** Reading progress bar with percentage
3. **Tags:** Document tags as chips
4. **Stats Row:** Words, highlights, notes count, reading time
5. **Timeline:** Saved, first opened, last read, completed dates
6. **Highlights List:** All highlights with notes and metadata

**Highlight Features:**
- Note indicator and expandable note content
- Date metadata
- Filter "with notes only"

---

## Mockup References

| File | Description |
|------|-------------|
| `design/dashboard-mockup.html` | Dashboard with clickable stat cards and slide-over panel |
| `design/document-detail-mockup.html` | Full document detail page with highlights |

Open mockups via: `cd design && python3 -m http.server 8888`
Then visit: http://localhost:8888/dashboard-mockup.html

---

## Interaction Patterns

### Stat Card Click → Slide-Over
```
User clicks "Words Read" card (brnSheetTrigger)
  → Sheet opens automatically (Spartan handles animation, backdrop, scroll lock)
  → Dashboard loads drill-down data
```

### Document Row Click → Detail Page
```
User clicks document row in slide-over
  → Navigate to /library/:documentId
  → Sheet closes automatically
  → Detail page renders
```

### Close Slide-Over
```
User clicks X button OR backdrop OR presses Escape
  → Sheet closes automatically (Spartan handles all close behaviors)
```

### Keyboard Navigation
Handled by Spartan Sheet:
- `Escape` - Close slide-over
- `Tab` - Navigate within panel (focus trap)

---

## Data Requirements

### New API Endpoints

#### 1. GET /api/analytics/drill-down/words-read
Documents contributing to words read for a period.

**Request:**
```
GET /api/analytics/drill-down/words-read?startDate=2024-12-01&endDate=2024-12-31
```

**Response:**
```json
{
  "summary": {
    "totalWords": 47832,
    "changePercent": 12.4
  },
  "documents": [
    {
      "id": "uuid",
      "title": "How to Build a Second Brain",
      "author": "Tiago Forte",
      "source": "fortelabs.com",
      "coverUrl": "https://...",
      "wordsRead": 8432,
      "readingProgress": 100,
      "category": "article"
    }
  ]
}
```

#### 2. GET /api/analytics/drill-down/completed
Articles completed in a period.

**Response:**
```json
{
  "summary": {
    "totalCompleted": 23,
    "changePercent": 8.2
  },
  "documents": [
    {
      "id": "uuid",
      "title": "...",
      "source": "...",
      "completedAt": "2024-12-18T14:30:00Z"
    }
  ]
}
```

#### 3. GET /api/analytics/drill-down/backlog
Unread documents sorted by age.

**Response:**
```json
{
  "summary": {
    "totalBacklog": 156,
    "changePercent": 5.1
  },
  "documents": [
    {
      "id": "uuid",
      "title": "...",
      "source": "...",
      "savedAt": "2024-11-05T10:00:00Z",
      "daysWaiting": 45
    }
  ]
}
```

#### 4. GET /api/documents/:id
Full document with highlights.

**Response:**
```json
{
  "id": "uuid",
  "readwiseId": "rw-123",
  "title": "How to Build a Second Brain",
  "author": "Tiago Forte",
  "source": "fortelabs.com",
  "sourceUrl": "https://...",
  "coverUrl": "https://...",
  "category": "article",
  "location": "archive",
  "wordCount": 4832,
  "readingProgress": 100,
  "savedAt": "2024-11-12T10:00:00Z",
  "firstOpenedAt": "2024-11-14T09:15:00Z",
  "lastOpenedAt": "2024-11-18T16:30:00Z",
  "tags": ["productivity", "pkm", "note-taking"],
  "highlights": [
    {
      "id": "uuid",
      "text": "Your Second Brain is a digital archive...",
      "note": "This is the core thesis of the book.",
      "color": "yellow",
      "location": 142,
      "createdAt": "2024-11-14T09:20:00Z"
    }
  ],
  "stats": {
    "highlightCount": 24,
    "notesCount": 8,
    "estimatedReadingTime": 18
  }
}
```

---

## Component Architecture

### New Components

| Component | Location | Spartan Components |
|-----------|----------|-------------------|
| `stat-drill-down-panel` | `shared/components/` | Sheet header/footer, Button, Icon |
| `document-row` | `shared/components/` | Badge (category), Icon |
| `document-detail` | `features/library/` | Progress, Badge (tags), Button, Card |
| `highlight-list` | `features/library/components/` | Checkbox (filter), Separator |
| `highlight-item` | `features/library/components/` | Card (optional) |
| `reading-timeline` | `shared/components/` | Custom layout (no equivalent) |

### Components to Modify

| Component | Change |
|-----------|--------|
| `kpi-card` | Add `(click)` output event |
| `most-highlighted` | Link rows to document detail |
| `dashboard.component` | Handle drill-down panel state |

### Services

| Service | Purpose |
|---------|---------|
| `DrillDownService` | Fetch drill-down data by stat type |
| `DocumentService` | Fetch single document with highlights |

---

## State Management

### Dashboard Component
```typescript
// Data signals only - Sheet manages its own visibility
readonly drillDownType = signal<'words' | 'completed' | 'backlog' | null>(null);
readonly drillDownData = signal<DrillDownData | null>(null);

// Load data when sheet opens (triggered by KPI card click)
loadDrillDownData(type: 'words' | 'completed' | 'backlog') {
  this.drillDownType.set(type);
  // Fetch data from service...
}
```

**Note:** No `drillDownOpen` signal needed. Spartan Sheet manages visibility internally via `brnSheetTrigger`.

### Document Detail Component
```typescript
readonly documentId = input.required<string>();
readonly document = signal<DocumentDetail | null>(null);
readonly loading = signal(true);

// Highlight filter
readonly notesOnly = signal(false);

readonly filteredHighlights = computed(() => {
  const doc = this.document();
  if (!doc) return [];

  if (this.notesOnly()) {
    return doc.highlights.filter(h => h.note !== null);
  }
  return doc.highlights;
});
```

---

## Styling Notes

### Spartan Components

All UI primitives use Spartan components:

| Element | Spartan Import |
|---------|---------------|
| Slide-over panel | `HlmSheetImports`, `BrnSheetImports` |
| Progress bars | `HlmProgressImports` |
| Buttons | `HlmButtonImports` |
| Tags/badges | `HlmBadgeImports` |
| Checkboxes | `HlmCheckboxImports`, `HlmLabelImports` |
| Icons | `HlmIconImports`, `provideIcons()` |
| Cards (optional) | `HlmCardImports` |
| Separators | `HlmSeparatorImports` |

### Icons (ng-icons/lucide)

| Purpose | Icon |
|---------|------|
| External link | `lucideExternalLink` |
| Word count | `lucideBookOpen` |
| Highlights | `lucideHighlighter` |
| Notes | `lucideStickyNote` |
| Reading time | `lucideClock` |
| Back navigation | `lucideArrowLeft` |
| Category: article | `lucideFileText` |
| Category: book | `lucideBook` |
| Category: pdf | `lucideFileType` |

### Transitions
Sheet animations handled by Spartan primitives. No custom transitions needed.

---

## Open Questions

1. ~~**Spartan UI Sheet Component:** Does `@spartan-ng/ui-sheet-brain` exist and support right-side slide-overs? If not, build custom.~~ **RESOLVED: Using Spartan Sheet.**

2. **Router vs Overlay for Detail:** Should document detail be:
   - Full route (`/library/:id`) - better for deep linking, bookmarking
   - Overlay/modal - stays in dashboard context
   - Recommendation: Full route for deep content

3. **Pagination:** Should drill-down lists paginate or load all? Consider:
   - Words read: likely 10-30 documents, load all
   - Backlog: could be 100+, may need pagination

4. **Highlight Inline Preview:** Show X characters of context around highlight, or just the highlighted text?

5. **Cover Image Fallback:** What to show when document has no cover? Gradient placeholder (as in mockup) or source favicon?

---

## Technical Specification

### Open Questions - Resolved

#### 1. Spartan UI Sheet Component
**Decision:** Use Spartan Sheet (`hlm-sheet`) with right-side slide-over.

**Rationale:** Spartan Sheet handles overlay, positioning, animations, keyboard (escape), and scroll locking. Matches the sync panel pattern. Eliminates custom panel logic entirely.

#### 2. Router vs Overlay for Document Detail
**Decision:** Full route at `/library/:documentId`.

**Rationale:**
- Enables deep linking and bookmarking
- Browser back button works naturally
- Simpler state management (no nested overlays)
- Matches sidebar "Library" navigation intent

#### 3. Pagination Strategy
**Decision:** Cursor-based pagination with "Load More" for all drill-down lists.

| Endpoint | Page Size | Rationale |
|----------|-----------|-----------|
| words-read | 20 | Power readers may have 50+ documents |
| completed | 20 | Consistent UX |
| backlog | 20 | Could be 100+ items |

**API Format:**
```json
{
  "documents": [...],
  "hasMore": true,
  "nextCursor": "uuid-of-last-item"
}
```

#### 4. Highlight Inline Preview
**Decision:** Show highlighted text only (no surrounding context).

**Rationale:**
- Readwise API doesn't provide surrounding context
- Extracting context would require storing full document content (scope creep)
- User can click "Open in Reader" for full context

#### 5. Cover Image Fallback
**Decision:** Category-based gradient placeholder.

| Category | Gradient |
|----------|----------|
| article | Blue → Indigo |
| book | Amber → Orange |
| pdf | Red → Rose |
| tweet | Cyan → Blue |
| default | Gray → Slate |

---

### Data Model Changes

#### Document Entity Additions

Add timestamp fields sourced from Readwise API:

```kotlin
// Add to Document entity
val firstOpenedAt: Instant? = null,  // From Readwise API
val lastOpenedAt: Instant? = null    // From Readwise API
```

**Migration:** Add nullable columns, populate from next Readwise sync.

#### Highlight Entity

No changes needed. Current fields are sufficient:
- `text` — highlighted content
- `highlightedAt` — creation timestamp
- Notes linked via separate `Note` entity

Color coding and location features are **out of scope** (data not available from Readwise API).

---

### API Endpoint Specifications

#### Drill-Down Endpoints

All drill-down endpoints follow consistent patterns:

**Common Query Parameters:**
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| startDate | ISO date | No | Period start (default: 30 days ago) |
| endDate | ISO date | No | Period end (default: today) |
| cursor | UUID | No | Pagination cursor |
| limit | int | No | Page size (default: 20, max: 50) |

**Common Response Structure:**
```typescript
interface DrillDownResponse<T> {
  readonly summary: {
    readonly total: number;
    readonly changePercent: number | null;
  };
  readonly documents: readonly T[];
  readonly hasMore: boolean;
  readonly nextCursor: string | null;
}
```

#### GET /api/analytics/drill-down/words-read

**Document DTO:**
```typescript
interface WordsReadDocument {
  readonly id: string;
  readonly title: string | null;
  readonly author: string | null;
  readonly source: string;           // Extracted from URL
  readonly coverUrl: string | null;
  readonly category: string;
  readonly wordsRead: number;        // progress × wordCount
  readonly readingProgress: number;  // 0-100
}
```

**SQL Query Pattern:**
```sql
SELECT d.*,
       ROUND(d.reading_progress * d.word_count) as words_read
FROM documents d
WHERE d.reading_progress > 0
  AND d.updated_at BETWEEN :start AND :end
ORDER BY words_read DESC
LIMIT :limit OFFSET :offset
```

#### GET /api/analytics/drill-down/completed

**Document DTO:**
```typescript
interface CompletedDocument {
  readonly id: string;
  readonly title: string | null;
  readonly author: string | null;
  readonly source: string;
  readonly coverUrl: string | null;
  readonly category: string;
  readonly completedAt: string;  // ISO timestamp
}
```

**Completion Detection:**
Documents where `reading_progress = 1.0` (100%). Use `updated_at` as completion timestamp until explicit tracking is added.

#### GET /api/analytics/drill-down/backlog

**Document DTO:**
```typescript
interface BacklogDocument {
  readonly id: string;
  readonly title: string | null;
  readonly author: string | null;
  readonly source: string;
  readonly coverUrl: string | null;
  readonly category: string;
  readonly savedAt: string;
  readonly daysWaiting: number;  // Computed: now - savedAt
}
```

**Backlog Definition:**
Documents where `reading_progress < 0.1` AND `location IN ('new', 'later', 'shortlist')`.

#### GET /api/documents/:id

**Response DTO:**
```typescript
interface DocumentDetailResponse {
  readonly id: string;
  readonly readwiseId: string;
  readonly title: string | null;
  readonly author: string | null;
  readonly sourceUrl: string;
  readonly source: string;           // Extracted domain
  readonly coverUrl: string | null;
  readonly category: string;
  readonly location: string;
  readonly wordCount: number | null;
  readonly readingProgress: number;
  readonly savedAt: string;
  readonly firstOpenedAt: string | null;  // From tracking
  readonly lastOpenedAt: string | null;   // From tracking
  readonly tags: readonly string[];
  readonly highlights: readonly HighlightDto[];
  readonly stats: DocumentStats;
}

interface HighlightDto {
  readonly id: string;
  readonly text: string;
  readonly note: string | null;  // From linked Note entity
  readonly createdAt: string;
}

interface DocumentStats {
  readonly highlightCount: number;
  readonly notesCount: number;
  readonly estimatedReadingTime: number;  // wordCount / 200 WPM
}
```

---

### Frontend Architecture

#### New TypeScript Interfaces

Add to `api.models.ts`:

```typescript
// Drill-down types
export type DrillDownType = 'words' | 'completed' | 'backlog';

export interface DrillDownSummary {
  readonly total: number;
  readonly changePercent: number | null;
}

export interface DrillDownDocument {
  readonly id: string;
  readonly title: string | null;
  readonly author: string | null;
  readonly source: string;
  readonly coverUrl: string | null;
  readonly category: string;
}

export interface WordsReadDocument extends DrillDownDocument {
  readonly wordsRead: number;
  readonly readingProgress: number;
}

export interface CompletedDocument extends DrillDownDocument {
  readonly completedAt: string;
}

export interface BacklogDocument extends DrillDownDocument {
  readonly savedAt: string;
  readonly daysWaiting: number;
}

export interface DrillDownResponse<T extends DrillDownDocument> {
  readonly summary: DrillDownSummary;
  readonly documents: readonly T[];
  readonly hasMore: boolean;
  readonly nextCursor: string | null;
}

// Document detail types
export interface HighlightDto {
  readonly id: string;
  readonly text: string;
  readonly note: string | null;
  readonly createdAt: string;
}

export interface DocumentStats {
  readonly highlightCount: number;
  readonly notesCount: number;
  readonly estimatedReadingTime: number;
}

export interface DocumentDetailResponse {
  readonly id: string;
  readonly readwiseId: string;
  readonly title: string | null;
  readonly author: string | null;
  readonly sourceUrl: string;
  readonly source: string;
  readonly coverUrl: string | null;
  readonly category: string;
  readonly location: string;
  readonly wordCount: number | null;
  readonly readingProgress: number;
  readonly savedAt: string;
  readonly firstOpenedAt: string | null;
  readonly lastOpenedAt: string | null;
  readonly tags: readonly string[];
  readonly highlights: readonly HighlightDto[];
  readonly stats: DocumentStats;
}
```

#### Service Layer

**DrillDownService** (`core/services/drill-down.service.ts`):
```typescript
@Injectable({ providedIn: 'root' })
export class DrillDownService {
  private readonly http = inject(HttpClient);

  getWordsRead(params: DrillDownParams): Observable<DrillDownResponse<WordsReadDocument>>;
  getCompleted(params: DrillDownParams): Observable<DrillDownResponse<CompletedDocument>>;
  getBacklog(params: DrillDownParams): Observable<DrillDownResponse<BacklogDocument>>;
}
```

**DocumentService** (`core/services/document.service.ts`):
```typescript
@Injectable({ providedIn: 'root' })
export class DocumentService {
  private readonly http = inject(HttpClient);

  getDocument(id: string): Observable<DocumentDetailResponse>;
}
```

#### Routing Configuration

Update `app.routes.ts`:
```typescript
{
  path: 'library/:id',
  loadComponent: () =>
    import('./features/library/document-detail.component').then(
      (m) => m.DocumentDetailComponent
    ),
}
```

#### Component Hierarchy

```
dashboard.component.ts
├── kpi-card.component.ts           # Add (cardClick) output
├── stat-drill-down-panel.component.ts  # NEW: slide-over container
│   └── document-row.component.ts   # NEW: clickable document item
└── ...existing components

library/
├── document-detail.component.ts    # NEW: full page view
│   ├── document-header.component.ts
│   ├── reading-timeline.component.ts
│   └── highlight-list.component.ts
│       └── highlight-item.component.ts
```

---

### Slide-Over Implementation

Uses Spartan Sheet pattern (matching sync panel):

**Dashboard template (wraps KPI cards):**
```typescript
// Each KPI card wrapped in Sheet trigger
<hlm-sheet side="right">
  <app-kpi-card
    brnSheetTrigger
    [label]="'Words Read'"
    [value]="stats.wordsRead"
    (click)="loadDrillDownData('words')"
  />
  <hlm-sheet-content class="w-full max-w-md sm:max-w-md">
    <app-stat-drill-down-panel
      [type]="'words'"
      [data]="drillDownData()"
      (documentSelect)="onDocumentSelect($event)"
    />
  </hlm-sheet-content>
</hlm-sheet>
```

**Panel component (content only, no overlay logic):**
```typescript
@Component({
  selector: 'app-stat-drill-down-panel',
  imports: [
    ...HlmSheetImports,
    ...HlmButtonImports,
    ...HlmIconImports,
    DocumentRowComponent,
  ],
  providers: [provideIcons({ lucideX })],
  template: `
    <hlm-sheet-header class="border-b border-border px-6 py-4">
      <h2 hlmSheetTitle class="text-lg font-semibold">{{ title() }}</h2>
    </hlm-sheet-header>

    <!-- Summary -->
    <div class="border-b border-border px-6 py-4">
      <div class="text-2xl font-bold">{{ summary()?.total | number }}</div>
      @if (summary()?.changePercent; as change) {
        <span class="text-sm text-muted-foreground">
          {{ change > 0 ? '+' : '' }}{{ change }}% vs last period
        </span>
      }
    </div>

    <!-- Document list -->
    <div class="flex-1 overflow-y-auto">
      @for (doc of documents(); track doc.id) {
        <app-document-row
          [document]="doc"
          [type]="type()"
          (click)="documentSelect.emit(doc.id)"
        />
      }
    </div>

    <hlm-sheet-footer class="border-t border-border px-6 py-4">
      @if (hasMore()) {
        <button hlmBtn variant="outline" class="w-full" (click)="loadMore.emit()">
          Load more
        </button>
      }
    </hlm-sheet-footer>
  `
})
export class StatDrillDownPanelComponent {
  readonly type = input.required<DrillDownType>();
  readonly summary = input<DrillDownSummary | null>();
  readonly documents = input<DrillDownDocument[]>([]);
  readonly hasMore = input(false);

  readonly loadMore = output<void>();
  readonly documentSelect = output<string>();

  readonly title = computed(() => {
    switch (this.type()) {
      case 'words': return 'Words Read';
      case 'completed': return 'Articles Completed';
      case 'backlog': return 'Reading Backlog';
    }
  });
}
```

**Key simplifications:**
- No `isOpen` input (Sheet manages visibility)
- No `close` output (Sheet handles via X button, escape, backdrop)
- No `@HostListener` for escape key
- No manual backdrop or positioning

---

### Error Handling

**Drill-down endpoints:** Return empty list with `total: 0` if no data.

**Document detail:** Return 404 with structured error:
```json
{
  "error": "DOCUMENT_NOT_FOUND",
  "message": "Document with ID 'xxx' not found",
  "documentId": "xxx"
}
```

**Frontend:** Show inline error state, not blocking modal.

---

### Testing Strategy

#### Backend
- Unit tests: Service methods with fakes
- Repository tests: Native SQL queries with `@DataJpaTest`
- API tests: Controller with `@WebMvcTest`

#### Frontend
- Component tests: Vitest + Testing Library
- Service tests: Mock HttpClient
- E2E: Manual testing via mockups (defer Playwright)

---

### Implementation Order

1. **Backend: Document entity** - Add `firstOpenedAt`, `lastOpenedAt` columns
2. **Backend: Sync enhancement** - Populate new timestamp fields from Readwise API
3. **Backend: Drill-down endpoints** - Three new endpoints
4. **Backend: Document detail endpoint** - Single document with highlights
5. **Frontend: Services** - DrillDownService, DocumentService
6. **Frontend: KPI card click** - Add output event
7. **Frontend: Slide-over panel** - Following SyncPanel pattern
8. **Frontend: Document detail page** - New route and component
9. **Frontend: Highlight list** - Notes filter and display

---

## Out of Scope

- Editing highlights or notes (read-only view)
- Document content/reader view (link to Readwise Reader)
- Bulk actions on backlog items
- Export highlights
- Search within highlights
- Highlight color coding (not available from Readwise API)
- Highlight location/position metadata (not available from Readwise API)
