# Notes Support Specification

## Summary

Add support for notes as a distinct entity while preserving domain separation. Fix the bug where notes are filtered out during sync.

## Problem Statement

1. **Notes are lost**: Items with `category='note'` are filtered out during sync
2. **Unused field**: `Highlight.note` is never populated (V3 API returns notes as separate items, not as a field)
3. **Incomplete hierarchy**: Article → Highlight → Note chain isn't captured

## Current Architecture (Preserve This)

```
Sync Context                    Library Context
├── DocumentSyncedEvent    →    Document entity
├── HighlightSyncedEvent   →    Highlight entity
└── (notes filtered out)        (no Note entity)
```

**Why keep it:**
- Type-safe events with distinct semantics
- Focused entities without nullable field pollution
- Tracking context only cares about Documents (no filtering needed)
- Analytics queries are simple JOINs, not self-referential

## Target Architecture

```
Sync Context                    Library Context
├── DocumentSyncedEvent    →    Document (articles, pdfs, epubs)
├── HighlightSyncedEvent   →    Highlight (text selections)
└── NoteSyncedEvent        →    Note (annotations on documents/highlights)
```

**Hierarchy preserved via parentId:**
```
Document (parentId=null)
  ├── Highlight (parentId → Document.readwiseId)
  │     └── Note (parentId → Highlight.readwiseId)
  └── Note (parentId → Document.readwiseId)
```

---

## Implementation Phases

### Phase 1: Add Note Entity

**File:** `library/domain/Note.kt`

```kotlin
@Entity
@Table(name = "notes")
data class Note(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(unique = true, nullable = false)
    val readwiseId: String,

    @Column(nullable = false)
    val parentId: String,  // points to document or highlight readwiseId

    @Column(columnDefinition = "TEXT", nullable = false)
    val content: String,

    val createdAt: Instant? = null
)
```

**Rationale:** Note is semantically distinct from Highlight. Highlights are text selections; Notes are user-authored annotations. Different behavior, different fields.

### Phase 2: Add NoteSyncedEvent

**File:** `sync/domain/events/NoteSyncedEvent.kt`

```kotlin
data class NoteSyncedEvent(
    val id: String,
    val parentId: String,
    val content: String,
    val createdAt: Instant?
)
```

### Phase 3: Update Sync Service

**File:** `sync/application/SyncService.kt`

Replace partition logic with three-way classification:

```kotlin
// Before
val (highlightDtos, documentDtos) = allItems.partition { it.isHighlight() }

// After
val documents = allItems.filter { it.isDocument() }
val highlights = allItems.filter { it.isHighlight() }
val notes = allItems.filter { it.isNote() }

documents.forEach { eventPublisher.publishEvent(it.toDocumentEvent()) }
highlights.forEach { eventPublisher.publishEvent(it.toHighlightEvent()) }
notes.forEach { eventPublisher.publishEvent(it.toNoteEvent()) }
```

**File:** `sync/infrastructure/readwise/dto/DocumentDto.kt`

Add extension function:
```kotlin
fun DocumentDto.isNote(): Boolean = category == "note"
fun DocumentDto.isDocument(): Boolean = category != "highlight" && category != "note"

fun DocumentDto.toNoteEvent() = NoteSyncedEvent(
    id = id,
    parentId = parentId ?: error("Note $id missing parentId"),
    content = content ?: "",
    createdAt = createdAt
)
```

### Phase 4: Add NoteEventListener

**File:** `library/application/NoteEventListener.kt`

```kotlin
@Component
class NoteEventListener(private val store: DocumentStore) {

    @EventListener
    fun handle(event: NoteSyncedEvent) {
        store.saveNote(
            Note(
                readwiseId = event.id,
                parentId = event.parentId,
                content = event.content,
                createdAt = event.createdAt
            )
        )
    }
}
```

### Phase 5: Update DocumentStore Interface

**File:** `library/application/DocumentStore.kt`

```kotlin
interface DocumentStore {
    // existing methods...
    fun saveNote(note: Note)
    fun findNoteByReadwiseId(readwiseId: String): Note?
}
```

**File:** `library/infrastructure/persistence/JpaDocumentStore.kt`

Add implementation using new `NoteRepository`.

### Phase 6: Add NoteRepository

**File:** `library/infrastructure/persistence/NoteRepository.kt`

```kotlin
interface NoteRepository : JpaRepository<Note, UUID> {
    fun findByReadwiseId(readwiseId: String): Note?
}
```

### Phase 7: Update Analytics Queries

**File:** `analytics/infrastructure/persistence/AnalyticsRepository.kt`

Update `getHighlightStats` to count notes properly:

```kotlin
// Before: counts Highlight.note field (always null)
val withNotesSql = """
    SELECT COUNT(*) FROM highlights
    WHERE note IS NOT NULL AND note != ''
""".trimIndent()

// After: counts Note entities linked to highlights
val withNotesSql = """
    SELECT COUNT(DISTINCT h.readwise_id)
    FROM highlights h
    WHERE EXISTS (
        SELECT 1 FROM notes n WHERE n.parent_id = h.readwise_id
    )
""".trimIndent()
```

Update `getMostHighlightedDocuments`:

```kotlin
// Before: uses Highlight.note field
BOOL_OR(h.note IS NOT NULL AND h.note != '') AS has_notes

// After: checks for Note entities
EXISTS (
    SELECT 1 FROM notes n
    WHERE n.parent_id = h.readwise_id
       OR n.parent_id = d.readwise_id
) AS has_notes
```

### Phase 8: Remove Unused Field

**File:** `library/domain/Highlight.kt`

Remove unused `note` field:

```kotlin
// Before
val note: String? = null,

// After: remove field entirely
```

**File:** `sync/domain/events/HighlightSyncedEvent.kt`

Remove unused `note` field:

```kotlin
// Before
val note: String?,

// After: remove field
```

### Phase 9: Update SyncLog

**File:** `sync/domain/SyncLog.kt`

Add notes tracking:

```kotlin
data class SyncLog(
    // existing fields...
    val notesProcessed: Int = 0
)
```

### Phase 10: Clean Up Diagnostic Logging

**File:** `sync/application/SyncService.kt`

Remove temporary investigation logging (lines 39-53).

---

## Database Migration

```sql
-- Create notes table
CREATE TABLE notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    readwise_id VARCHAR(255) UNIQUE NOT NULL,
    parent_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notes_parent_id ON notes(parent_id);

-- Remove unused column from highlights (optional, can defer)
ALTER TABLE highlights DROP COLUMN IF EXISTS note;

-- Add notes_processed to sync_logs
ALTER TABLE sync_logs ADD COLUMN notes_processed INTEGER DEFAULT 0;
```

---

## Files Summary

### Create (4 files)

| File | Purpose |
|------|---------|
| `library/domain/Note.kt` | Note entity |
| `library/infrastructure/persistence/NoteRepository.kt` | JPA repository |
| `library/application/NoteEventListener.kt` | Event handler |
| `sync/domain/events/NoteSyncedEvent.kt` | Sync event |

### Modify (7 files)

| File | Change |
|------|--------|
| `sync/application/SyncService.kt` | Three-way partition, publish note events |
| `sync/infrastructure/readwise/dto/DocumentDto.kt` | Add `isNote()`, `toNoteEvent()` |
| `library/application/DocumentStore.kt` | Add `saveNote()` |
| `library/infrastructure/persistence/JpaDocumentStore.kt` | Implement note persistence |
| `analytics/infrastructure/persistence/AnalyticsRepository.kt` | Fix hasNotes queries |
| `sync/domain/SyncLog.kt` | Add notesProcessed |
| `library/domain/Highlight.kt` | Remove unused note field |

### Tests to Add

| File | Coverage |
|------|----------|
| `NoteEventListenerTest.kt` | Note persistence via events |
| `NoteRepositoryTest.kt` | CRUD operations |
| Update `SyncServiceTest.kt` | NoteSyncedEvent assertions |

---

## Trade-off Analysis

### This Approach

| Pros | Cons |
|------|------|
| Type-safe events per domain concept | Three event types instead of one |
| Focused entities, no nullable pollution | Three tables instead of one |
| Tracking context unchanged | Slightly more code |
| Simple JOINs in analytics | |
| Notes become first-class citizens | |

### Unified Model (Original Spec)

| Pros | Cons |
|------|------|
| One table | God entity with scattered nullability |
| One event type | Lost type safety, runtime category checks |
| API-aligned | Infrastructure driving domain |
| | Complex self-referential queries |
| | Tracking context needs filtering |

---

## Why Not Merge?

The original spec's justification was "API returns them as documents." But:

1. **API structure ≠ domain model**: Readwise's API design choices don't dictate your analytics domain
2. **Different semantics**: Documents have reading progress; highlights have text; notes have content. Forcing these into one entity creates nullable field pollution
3. **Query complexity**: Self-JOINs with category filters are more complex than simple table JOINs
4. **Type safety**: `when (category)` everywhere vs type-safe event handlers

The real problems (notes filtered, sync complexity) are fixed without sacrificing domain clarity.
