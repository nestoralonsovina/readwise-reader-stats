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

**Behavior:**
- Width: 400px (max-w-md)
- Slides in from right with 300ms ease-out transition
- Backdrop: semi-transparent overlay (click to close)
- Close: X button, Escape key, or backdrop click
- Scroll: Document list scrolls independently
- Body scroll locked when open

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
│  Highlights                              [Color] [With notes]│
├─────────────────────────────────────────────────────────────┤
│  ┃ "Highlighted text passage here..."           │ Nov 14    │
│  ┃                                              │ Loc 142   │
│  ├──────────────────────────────────────────────┤           │
│  │ 💬 User's note about this highlight          │           │
│  └──────────────────────────────────────────────┘           │
│                                                             │
│  ┃ "Another highlighted passage..."             │ Nov 15    │
│  ┃                                              │ Loc 298   │
└─────────────────────────────────────────────────────────────┘
```

**Sections:**
1. **Header:** Cover image, title, author, source, location badge, category
2. **Progress:** Reading progress bar with percentage
3. **Tags:** Document tags as chips
4. **Stats Row:** Words, highlights, notes count, reading time
5. **Timeline:** Saved, first opened, last read, completed dates
6. **Highlights List:** All highlights with color coding, notes, and metadata

**Highlight Features:**
- Color-coded left border (yellow, blue, green, pink, purple)
- Note indicator and expandable note content
- Date and location metadata
- Filter by color
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
User clicks "Words Read" card
  → Panel slides in from right (300ms ease-out)
  → Backdrop fades in (300ms)
  → Body scroll locked
  → Focus trapped in panel
```

### Document Row Click → Detail Page
```
User clicks document row in slide-over
  → Navigate to /library/:documentId
  → Slide-over closes
  → Detail page renders
```

### Close Slide-Over
```
User clicks X button OR backdrop OR presses Escape
  → Panel slides out (300ms ease-out)
  → Backdrop fades out
  → Body scroll restored
```

### Keyboard Navigation
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

| Component | Location | Purpose |
|-----------|----------|---------|
| `stat-drill-down-panel` | `shared/components/` | Slide-over panel container |
| `document-row` | `shared/components/` | Row in drill-down list |
| `document-detail` | `features/library/` | Full document detail page |
| `highlight-list` | `features/library/components/` | Highlights with filters |
| `highlight-item` | `features/library/components/` | Single highlight with note |
| `reading-timeline` | `shared/components/` | Saved → Opened → Completed |

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
// Slide-over state
readonly drillDownOpen = signal(false);
readonly drillDownType = signal<'words' | 'completed' | 'backlog' | null>(null);
readonly drillDownData = signal<DrillDownData | null>(null);

// Open panel
openDrillDown(type: 'words' | 'completed' | 'backlog') {
  this.drillDownType.set(type);
  this.drillDownOpen.set(true);
  this.loadDrillDownData(type);
}

// Close panel
closeDrillDown() {
  this.drillDownOpen.set(false);
  this.drillDownType.set(null);
}
```

### Document Detail Component
```typescript
readonly documentId = input.required<string>();
readonly document = signal<DocumentDetail | null>(null);
readonly loading = signal(true);

// Highlight filters
readonly colorFilter = signal<HighlightColor | 'all'>('all');
readonly notesOnly = signal(false);

readonly filteredHighlights = computed(() => {
  const doc = this.document();
  if (!doc) return [];

  return doc.highlights.filter(h => {
    if (this.colorFilter() !== 'all' && h.color !== this.colorFilter()) return false;
    if (this.notesOnly() && !h.note) return false;
    return true;
  });
});
```

---

## Styling Notes

### Highlight Colors
```css
.highlight-yellow { background: rgba(250, 204, 21, 0.3); border-left-color: #facc15; }
.highlight-blue   { background: rgba(59, 130, 246, 0.2); border-left-color: #3b82f6; }
.highlight-green  { background: rgba(34, 197, 94, 0.2);  border-left-color: #22c55e; }
.highlight-pink   { background: rgba(236, 72, 153, 0.2); border-left-color: #ec4899; }
.highlight-purple { background: rgba(168, 85, 247, 0.2); border-left-color: #a855f7; }
```

### Dark Mode Variants
All highlight colors reduce opacity to 0.15 in dark mode.

### Transitions
- Panel slide: `transform 300ms ease-out`
- Backdrop fade: `opacity 300ms ease-out`
- Body scroll lock when panel open

---

## Open Questions

1. **Spartan UI Sheet Component:** Does `@spartan-ng/ui-sheet-brain` exist and support right-side slide-overs? If not, build custom.

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

## Out of Scope

- Editing highlights or notes (read-only view)
- Document content/reader view (link to Readwise Reader)
- Bulk actions on backlog items
- Export highlights
- Search within highlights
