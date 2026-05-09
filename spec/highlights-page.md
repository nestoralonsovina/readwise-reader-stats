# Highlights Page Implementation

## Overview

A dedicated page to browse, search, and filter highlights across the reading library. Provides two views: grouped by document (accordion) and unified chronological stream.

## Design Reference

- **By Document view**: `design/highlights-mockup.html`
- **All Highlights view**: `design/highlights-all-view-mockup.html`

## Data Requirements

### Existing API Endpoints

| Endpoint | Purpose |
|----------|---------|
| `GET /api/analytics/highlights` | Summary stats + top documents |
| `GET /api/documents/{id}` | Document detail with highlights |

### New API Endpoint Required

```
GET /api/library/highlights
```

Returns paginated highlights across all documents for the "All Highlights" view. Namespaced under `/api/library/` to allow future endpoints like `/api/library/documents`.

**Query Parameters:**
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `startDate` | string | - | ISO date filter start |
| `endDate` | string | - | ISO date filter end |
| `withNotes` | boolean | false | Filter to highlights with notes |
| `search` | string | - | Full-text search in highlight text |
| `cursor` | string | - | Pagination cursor |
| `limit` | number | 20 | Page size |

**Response:**
```typescript
interface HighlightsListResponse {
  highlights: HighlightWithDocument[];
  hasMore: boolean;
  nextCursor: string | null;
}

interface HighlightWithDocument {
  id: string;
  text: string;
  note: string | null;
  createdAt: string | null;
  document: {
    id: string;
    title: string | null;
    source: string;
    coverUrl: string | null;
    category: string | null;
  };
}
```

---

## Backend Implementation

### Context Placement

The new endpoint belongs in the **Library Context**:

| Consideration | Decision |
|---------------|----------|
| Domain ownership | Highlights are Library entities |
| Use case | Browsing library content, not computing metrics |
| Independence | Separate from dashboard analytics |
| REST design | `GET /api/library/highlights` namespaced for future library endpoints |

**Note:** While the query is complex (joins, pagination, search), it's fundamentally about retrieving Library data—not computing derived analytics like reading streaks or completion rates.

### File Structure

```
backend/src/main/kotlin/com/reader/analytics/
├── library/
│   ├── domain/
│   │   └── HighlightWithDocument.kt       # Domain model for query result
│   └── application/
│       └── HighlightStore.kt              # Add query method
├── api/
│   ├── LibraryController.kt               # New controller for /api/library/*
│   └── dto/
│       └── HighlightsListDto.kt           # Response DTOs
```

### Domain Model

Add to `library/domain/HighlightWithDocument.kt`:

```kotlin
package com.reader.analytics.library.domain

import java.time.Instant
import java.util.UUID

data class HighlightWithDocument(
    val id: UUID,
    val text: String,
    val note: String?,
    val highlightedAt: Instant?,
    val documentId: UUID,
    val documentTitle: String?,
    val documentUrl: String,
    val documentImageUrl: String?,
    val documentCategory: String?
)

data class HighlightsPage(
    val items: List<HighlightWithDocument>,
    val hasMore: Boolean,
    val nextCursor: UUID?
)
```

### HighlightStore Interface

Add method to `library/application/HighlightStore.kt`:

```kotlin
fun findWithDocuments(
    startDate: Instant?,
    endDate: Instant?,
    withNotes: Boolean,
    searchQuery: String?,
    cursor: UUID?,
    limit: Int
): HighlightsPage
```

### Repository Query

Add to `library/infrastructure/persistence/HighlightRepository.kt`:

```kotlin
@Query(
    value = """
        SELECT
            h.id,
            h.text,
            n.content as note,
            h.highlighted_at,
            d.id as document_id,
            d.title as document_title,
            d.url as document_url,
            d.image_url as document_image_url,
            d.category as document_category
        FROM highlights h
        JOIN documents d ON h.document_id = d.id
        LEFT JOIN notes n ON n.highlight_id = h.id
        WHERE (:startDate IS NULL OR h.highlighted_at >= :startDate)
          AND (:endDate IS NULL OR h.highlighted_at <= :endDate)
          AND (:withNotes = false OR n.id IS NOT NULL)
          AND (:searchQuery IS NULL OR h.text ILIKE '%' || :searchQuery || '%')
          AND (:cursor IS NULL OR h.id < :cursor)
        ORDER BY h.highlighted_at DESC NULLS LAST, h.id DESC
        LIMIT :limit
    """,
    nativeQuery = true
)
fun findHighlightsWithDocuments(
    @Param("startDate") startDate: Instant?,
    @Param("endDate") endDate: Instant?,
    @Param("withNotes") withNotes: Boolean,
    @Param("searchQuery") searchQuery: String?,
    @Param("cursor") cursor: UUID?,
    @Param("limit") limit: Int
): List<Array<Any?>>
```

**Query Design Notes:**

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| Cursor pagination | UUID-based, descending | Consistent with drill-down endpoints |
| Search | ILIKE | Simple, works for MVP; upgrade to `tsvector` if needed |
| Notes filter | LEFT JOIN + IS NOT NULL | Filter in SQL, not application |
| Ordering | `highlighted_at DESC, id DESC` | Stable sort for pagination |

### JpaHighlightStore Implementation

Add to `library/infrastructure/persistence/JpaHighlightStore.kt`:

```kotlin
override fun findWithDocuments(
    startDate: Instant?,
    endDate: Instant?,
    withNotes: Boolean,
    searchQuery: String?,
    cursor: UUID?,
    limit: Int
): HighlightsPage {
    val results = highlightRepository.findHighlightsWithDocuments(
        startDate = startDate,
        endDate = endDate,
        withNotes = withNotes,
        searchQuery = searchQuery?.takeIf { it.isNotBlank() },
        cursor = cursor,
        limit = limit + 1  // Fetch one extra to determine hasMore
    )

    val hasMore = results.size > limit
    val items = results.take(limit).map { row ->
        HighlightWithDocument(
            id = row[0] as UUID,
            text = row[1] as String,
            note = row[2] as String?,
            highlightedAt = (row[3] as java.sql.Timestamp?)?.toInstant(),
            documentId = row[4] as UUID,
            documentTitle = row[5] as String?,
            documentUrl = row[6] as String,
            documentImageUrl = row[7] as String?,
            documentCategory = row[8] as String?
        )
    }

    return HighlightsPage(
        items = items,
        hasMore = hasMore,
        nextCursor = if (hasMore) items.lastOrNull()?.id else null
    )
}
```

### Controller

Create new `api/LibraryController.kt`:

```kotlin
package com.reader.analytics.api

import com.reader.analytics.api.dto.*
import com.reader.analytics.library.application.HighlightStore
import com.reader.analytics.shared.UrlUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@RestController
@RequestMapping("/api/library")
@Tag(name = "Library")
class LibraryController(
    private val highlightStore: HighlightStore
) {

    @GetMapping("/highlights")
    @Operation(
        summary = "List highlights with document context",
        description = """
            Returns paginated highlights across all documents.

            Supports filtering by date range, notes presence, and full-text search.
            Results ordered by highlight creation date (newest first).

            Use cursor-based pagination for efficient scrolling through large result sets.
        """
    )
    @ApiResponse(
        responseCode = "200",
        description = "Highlights retrieved successfully",
        content = [Content(schema = Schema(implementation = HighlightsListResponse::class))]
    )
    fun listHighlights(
        @Parameter(description = "Start date filter (ISO format)", example = "2024-01-01")
        @RequestParam(required = false) startDate: String?,

        @Parameter(description = "End date filter (ISO format)", example = "2024-12-31")
        @RequestParam(required = false) endDate: String?,

        @Parameter(description = "Only return highlights with notes attached")
        @RequestParam(defaultValue = "false") withNotes: Boolean,

        @Parameter(description = "Search text within highlights")
        @RequestParam(required = false) search: String?,

        @Parameter(description = "Pagination cursor (highlight ID)")
        @RequestParam(required = false) cursor: String?,

        @Parameter(description = "Maximum results to return", example = "20")
        @RequestParam(defaultValue = "20") limit: Int
    ): HighlightsListResponse {
        val startInstant = startDate?.let {
            LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant()
        }
        val endInstant = endDate?.let {
            LocalDate.parse(it).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        }

        val cursorUuid = cursor?.let { UUID.fromString(it) }
        val result = highlightStore.findWithDocuments(
            startDate = startInstant,
            endDate = endInstant,
            withNotes = withNotes,
            searchQuery = search,
            cursor = cursorUuid,
            limit = limit.coerceIn(1, 100)
        )

        return HighlightsListResponse(
            highlights = result.items.map { hl ->
                HighlightWithDocumentDto(
                    id = hl.id.toString(),
                    text = hl.text,
                    note = hl.note,
                    createdAt = hl.highlightedAt?.toString(),
                    document = HighlightDocumentDto(
                        id = hl.documentId.toString(),
                        title = hl.documentTitle,
                        source = UrlUtils.extractDomain(hl.documentUrl),
                        coverUrl = hl.documentImageUrl,
                        category = hl.documentCategory
                    )
                )
            },
            hasMore = result.hasMore,
            nextCursor = result.nextCursor?.toString()
        )
    }
}
```

### DTO Classes

Add `api/dto/HighlightsListDto.kt`:

```kotlin
package com.reader.analytics.api.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Paginated list of highlights with document context")
data class HighlightsListResponse(
    @Schema(description = "List of highlights with their source documents")
    val highlights: List<HighlightWithDocumentDto>,

    @Schema(description = "Whether more results are available", example = "true")
    val hasMore: Boolean,

    @Schema(description = "Cursor for next page (null if no more results)")
    val nextCursor: String?
)

@Schema(description = "Highlight with its source document")
data class HighlightWithDocumentDto(
    @Schema(description = "Highlight UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    val id: String,

    @Schema(description = "Highlighted text content")
    val text: String,

    @Schema(description = "User note attached to this highlight")
    val note: String?,

    @Schema(description = "When the highlight was created (ISO-8601)")
    val createdAt: String?,

    @Schema(description = "Source document information")
    val document: HighlightDocumentDto
)

@Schema(description = "Minimal document info for highlight context")
data class HighlightDocumentDto(
    @Schema(description = "Document UUID", example = "550e8400-e29b-41d4-a716-446655440001")
    val id: String,

    @Schema(description = "Document title")
    val title: String?,

    @Schema(description = "Source domain", example = "example.com")
    val source: String,

    @Schema(description = "Document cover image URL")
    val coverUrl: String?,

    @Schema(description = "Content category", example = "article")
    val category: String?
)
```

### API Design

| Endpoint | Purpose | Context |
|----------|---------|---------|
| `GET /api/library/highlights` | Browse/list highlights | Library (new) |
| `GET /api/library/documents` | Browse/list documents | Library (future) |
| `GET /api/analytics/highlights` | Highlight statistics | Analytics (existing) |

**Rationale:** `/api/library/*` groups entity browsing endpoints. `/api/analytics/*` groups computed metrics. This separation keeps contexts focused and URLs predictable.

### Database Index

Add for query performance:

```sql
CREATE INDEX idx_highlights_highlighted_at_desc
ON highlights (highlighted_at DESC NULLS LAST, id DESC);

CREATE INDEX idx_notes_highlight_id
ON notes (highlight_id) WHERE highlight_id IS NOT NULL;
```

### Backend Testing

**Store Tests (JpaHighlightStoreTest):**
```kotlin
@Test
fun `findWithDocuments returns paginated results`()

@Test
fun `findWithDocuments filters by date range`()

@Test
fun `findWithDocuments filters to notes only`()

@Test
fun `findWithDocuments searches highlight text`()

@Test
fun `findWithDocuments respects cursor pagination`()
```

**Repository Tests (HighlightRepositoryTest):**
```kotlin
@Test
fun `findHighlightsWithDocuments joins document data`()

@Test
fun `findHighlightsWithDocuments left joins notes`()

@Test
fun `findHighlightsWithDocuments applies search filter`()
```

**Controller Tests (LibraryControllerTest):**
```kotlin
@Test
fun `listHighlights returns highlights with document context`()

@Test
fun `listHighlights enforces max limit`()

@Test
fun `listHighlights parses date parameters correctly`()
```

---

## Frontend Implementation

### File Structure

```
src/app/features/highlights/
├── highlights.component.ts          # Container component
├── highlights.routes.ts             # Route config
└── components/
    ├── highlights-header/
    │   └── highlights-header.component.ts
    ├── highlights-kpi-row/
    │   └── highlights-kpi-row.component.ts
    ├── highlights-toolbar/
    │   └── highlights-toolbar.component.ts
    ├── document-accordion/
    │   └── document-accordion.component.ts
    └── highlight-stream/
        └── highlight-stream.component.ts
```

### Route Configuration

```typescript
// highlights.routes.ts
import { Routes } from '@angular/router';

export const HIGHLIGHTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./highlights.component').then(m => m.HighlightsComponent),
  },
];
```

Register in `app.routes.ts`:
```typescript
{
  path: 'highlights',
  loadChildren: () =>
    import('./features/highlights/highlights.routes').then(m => m.HIGHLIGHTS_ROUTES),
}
```

---

## Component Specifications

### 1. HighlightsComponent (Container)

**Responsibility:** Orchestrates state, fetches data, manages view mode.

**Signals:**
```typescript
readonly period = signal<Period>(30);
readonly viewMode = signal<'byDocument' | 'allHighlights'>('byDocument');
readonly notesOnly = signal<boolean>(false);
readonly searchQuery = signal<string>('');
readonly highlightStats = signal<HighlightResponse | null>(null);
readonly highlights = signal<HighlightsListResponse | null>(null);
readonly expandedDocumentId = signal<string | null>(null);
readonly documentDetail = signal<DocumentDetailResponse | null>(null);
```

**Data Fetching:**
- On init and period change: fetch `/api/analytics/highlights`
- On view mode = 'allHighlights': fetch `/api/highlights`
- On document expand: fetch `/api/documents/{id}`

**Template Structure:**
```html
<app-highlights-header [period]="period()" (periodChange)="period.set($event)" />

<app-highlights-kpi-row [data]="highlightStats()" />

<app-highlights-toolbar
  [viewMode]="viewMode()"
  [notesOnly]="notesOnly()"
  [searchQuery]="searchQuery()"
  (viewModeChange)="viewMode.set($event)"
  (notesOnlyChange)="notesOnly.set($event)"
  (searchChange)="searchQuery.set($event)"
/>

@if (viewMode() === 'byDocument') {
  <app-document-accordion
    [documents]="highlightStats()?.topDocuments ?? []"
    [expandedId]="expandedDocumentId()"
    [documentDetail]="documentDetail()"
    (expand)="onDocumentExpand($event)"
  />
} @else {
  <app-highlight-stream
    [highlights]="highlights()"
    [notesOnly]="notesOnly()"
    (loadMore)="onLoadMore()"
  />
}
```

---

### 2. HighlightsHeaderComponent

**Spartan Components:**
- Period selector: `HlmToggleGroup`, `HlmToggleGroupItem`

**Inputs:**
```typescript
readonly period = input.required<Period>();
```

**Outputs:**
```typescript
readonly periodChange = output<Period>();
```

**Template:**
```html
<header class="border-b border-border bg-card">
  <div class="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
    <div>
      <h1 class="text-xl font-semibold">Highlights</h1>
      <p class="text-sm text-muted-foreground">Your annotations and saved passages</p>
    </div>

    <div hlmToggleGroup type="single" [value]="period()" (valueChange)="periodChange.emit($event)">
      <button hlmToggleGroupItem [value]="7">Week</button>
      <button hlmToggleGroupItem [value]="30">Month</button>
      <button hlmToggleGroupItem [value]="365">Year</button>
    </div>
  </div>
</header>
```

---

### 3. HighlightsKpiRowComponent

**Spartan Components:**
- Cards: `HlmCard`, `HlmCardHeader`, `HlmCardContent`
- Progress: `BrnProgressComponent`, `HlmProgressImports`

**Inputs:**
```typescript
readonly data = input<HighlightResponse | null>();
```

**KPI Cards (4 columns):**

| Card | Icon Color | Value | Subtext |
|------|------------|-------|---------|
| Total Highlights | amber | `summary.total` | "across X documents" |
| This Period | blue | `summary.thisPeriod` | % change badge, "vs X last period" |
| With Notes | emerald | `summary.withNotes` | percentage + progress bar |
| Avg / Document | violet | `summary.averagePerDocument` | "highlights per document" |

---

### 4. HighlightsToolbarComponent

**Spartan Components:**
- View toggle: `HlmToggleGroup`, `HlmToggleGroupItem`
- Checkbox: `HlmCheckbox`, `HlmLabel`
- Search: `HlmInput`
- Icons: `HlmIconImports`

**Inputs:**
```typescript
readonly viewMode = input.required<'byDocument' | 'allHighlights'>();
readonly notesOnly = input.required<boolean>();
readonly searchQuery = input.required<string>();
```

**Outputs:**
```typescript
readonly viewModeChange = output<'byDocument' | 'allHighlights'>();
readonly notesOnlyChange = output<boolean>();
readonly searchChange = output<string>();
```

**Template:**
```html
<div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
  <div class="flex items-center gap-4">
    <!-- View Toggle -->
    <div hlmToggleGroup type="single" [value]="viewMode()" (valueChange)="viewModeChange.emit($event)">
      <button hlmToggleGroupItem value="byDocument">
        <ng-icon hlm name="lucideArchive" size="sm" />
        By Document
      </button>
      <button hlmToggleGroupItem value="allHighlights">
        <ng-icon hlm name="lucideList" size="sm" />
        All Highlights
      </button>
    </div>

    <!-- Notes Filter -->
    <label hlmLabel class="flex items-center gap-2 cursor-pointer">
      <hlm-checkbox [checked]="notesOnly()" (checkedChange)="notesOnlyChange.emit($event)" />
      With notes only
    </label>
  </div>

  <!-- Search -->
  <div class="relative w-full sm:w-64">
    <ng-icon hlm name="lucideSearch" size="sm" class="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
    <input
      hlmInput
      type="text"
      placeholder="Search highlights..."
      class="pl-10"
      [value]="searchQuery()"
      (input)="onSearchInput($event)"
    />
  </div>
</div>
```

**Debounce search** with `debounceTime(300)` before emitting.

---

### 5. DocumentAccordionComponent

**Spartan Components:**
- Accordion: `BrnAccordion`, `HlmAccordionImports`
- Badge: `HlmBadge`
- Icons: `HlmIconImports`

**Inputs:**
```typescript
readonly documents = input.required<readonly TopDocumentDto[]>();
readonly expandedId = input<string | null>();
readonly documentDetail = input<DocumentDetailResponse | null>();
```

**Outputs:**
```typescript
readonly expand = output<string>();
```

**Template Structure:**
```html
<div hlmAccordion type="single" [value]="expandedId()">
  @for (doc of documents(); track doc.documentId) {
    <div hlmAccordionItem [value]="doc.documentId">
      <button hlmAccordionTrigger (click)="expand.emit(doc.documentId)">
        <!-- Document row: cover, title, source, highlight count badge, notes badge -->
      </button>
      <div hlmAccordionContent>
        @if (expandedId() === doc.documentId && documentDetail(); as detail) {
          <app-highlight-list
            [highlights]="detail.highlights"
            [notesOnly]="false"
          />
        } @else {
          <!-- Loading skeleton -->
          <div hlmSkeleton class="h-24 w-full" />
        }
      </div>
    </div>
  }
</div>
```

**Reuses existing:** `HighlightListComponent` and `HighlightItemComponent` from `features/library/components/`.

---

### 6. HighlightStreamComponent

**Spartan Components:**
- Card container: `HlmCard`
- Button: `HlmButton`
- Skeleton: `HlmSkeleton`

**Inputs:**
```typescript
readonly highlights = input<HighlightsListResponse | null>();
readonly notesOnly = input.required<boolean>();
```

**Outputs:**
```typescript
readonly loadMore = output<void>();
```

**Template:**
```html
<section hlmCard class="overflow-hidden">
  <div class="divide-y divide-border">
    @for (hl of filteredHighlights(); track hl.id) {
      <article class="p-5 hover:bg-muted/50">
        <div class="flex gap-4">
          <!-- Document thumbnail -->
          <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-chart-2 to-chart-4 flex-shrink-0"></div>

          <div class="flex-1 min-w-0">
            <!-- Document context -->
            <div class="flex items-center gap-2 mb-2">
              <a [routerLink]="['/library', hl.document.id]" class="text-sm font-medium hover:text-brand truncate">
                {{ hl.document.title ?? 'Untitled' }}
              </a>
              <span class="text-xs text-muted-foreground">{{ hl.document.source }}</span>
            </div>

            <!-- Highlight with quote bar -->
            <app-highlight-item [highlight]="hl" />
          </div>
        </div>
      </article>
    } @empty {
      <div class="p-8 text-center text-muted-foreground">
        No highlights found
      </div>
    }
  </div>

  <!-- Load more -->
  @if (highlights()?.hasMore) {
    <div class="p-4 border-t border-border">
      <button hlmBtn variant="outline" class="w-full" (click)="loadMore.emit()">
        Load more
      </button>
    </div>
  }
</section>
```

---

## API Models

Add to `core/models/api.models.ts`:

```typescript
// ============================================================================
// Highlights List Types (new endpoint)
// ============================================================================

export interface HighlightDocument {
  readonly id: string;
  readonly title: string | null;
  readonly source: string;
  readonly coverUrl: string | null;
  readonly category: string | null;
}

export interface HighlightWithDocument {
  readonly id: string;
  readonly text: string;
  readonly note: string | null;
  readonly createdAt: string | null;
  readonly document: HighlightDocument;
}

export interface HighlightsListResponse {
  readonly highlights: readonly HighlightWithDocument[];
  readonly hasMore: boolean;
  readonly nextCursor: string | null;
}

export interface HighlightsListParams {
  readonly startDate?: string;
  readonly endDate?: string;
  readonly withNotes?: boolean;
  readonly search?: string;
  readonly cursor?: string;
  readonly limit?: number;
}
```

---

## Service Updates

Create new `core/services/highlights.service.ts`:

```typescript
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HighlightsListParams, HighlightsListResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class HighlightsService {
  private readonly http = inject(HttpClient);

  getList(params: HighlightsListParams): Observable<HighlightsListResponse> {
    let httpParams = new HttpParams();

    if (params.startDate) httpParams = httpParams.set('startDate', params.startDate);
    if (params.endDate) httpParams = httpParams.set('endDate', params.endDate);
    if (params.withNotes) httpParams = httpParams.set('withNotes', 'true');
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.cursor) httpParams = httpParams.set('cursor', params.cursor);
    if (params.limit) httpParams = httpParams.set('limit', params.limit.toString());

    return this.http.get<HighlightsListResponse>('/api/library/highlights', { params: httpParams });
  }
}
```

---

## Navigation Updates

Update `shared/components/sidebar/sidebar.component.ts`:

1. Remove "Coming Soon" badge from Highlights nav item
2. Add `routerLink="/highlights"` and `routerLinkActive`

---

## Accessibility

| Requirement | Implementation |
|-------------|----------------|
| Keyboard navigation | Accordion items focusable, Enter/Space to expand |
| Screen reader | Accordion uses ARIA expanded state |
| Focus indicators | Spartan components include focus rings |
| Search debounce | 300ms delay reduces screen reader noise |
| Loading states | Skeleton with `aria-busy="true"` |

---

## Implementation Order

### Phase 1: Backend (Library Context)

1. **Domain model**: `HighlightWithDocument.kt` and `HighlightsPage` in `library/domain/`
2. **Port**: Add `findWithDocuments()` to `HighlightStore` interface
3. **Repository**: Add `findHighlightsWithDocuments()` native query to `HighlightRepository`
4. **Adapter**: Implement `findWithDocuments()` in `JpaHighlightStore`
5. **DTOs**: Create `HighlightsListDto.kt` with response types
6. **Controller**: Create `LibraryController` with `GET /api/library/highlights`
7. **Index**: Add database index for query performance
8. **Tests**: Store + repository + controller tests

### Phase 2: Frontend

9. **Models**: Add TypeScript interfaces to `api.models.ts`
10. **Service**: Create `HighlightsService` with `getList()`
11. **Route**: Register `/highlights` route in `app.routes.ts`
12. **Container**: `HighlightsComponent` with state management
13. **Header**: `HighlightsHeaderComponent` with period toggle
14. **KPI Row**: `HighlightsKpiRowComponent` with 4 cards
15. **Toolbar**: `HighlightsToolbarComponent` with view toggle, filter, search
16. **By Document**: `DocumentAccordionComponent` reusing existing highlight components
17. **All Highlights**: `HighlightStreamComponent` with cursor pagination
18. **Navigation**: Update sidebar to link to highlights page

---

## Testing

### Unit Tests

- `HighlightsComponent`: Signal state transitions, data fetching triggers
- `HighlightsToolbarComponent`: Debounced search, filter toggles
- `DocumentAccordionComponent`: Expand/collapse, loading states

### Integration Tests

- Period change refetches data
- View mode switch loads correct data
- Search filters results (with debounce)
- Pagination loads next page

---

## Future Enhancements

- Export highlights to Markdown/CSV
- Bulk actions (delete, tag)
- Highlight color filtering (when available in API)
- Sort options (date, document, most notes)
- Keyboard shortcuts for navigation
