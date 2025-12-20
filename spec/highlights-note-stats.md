# Highlights Note Statistics

## Summary

Add backend support for note-based highlight analytics. The UI mockup (`design/dashboard-mockup.html`) now shows annotation metrics instead of color distribution (which V3 API doesn't provide).

## Context

- **V3 Reader API only** — no Highlights v2 API
- Highlight entity has: `text`, `note`, `highlightedAt`, `documentReadwiseId`
- Color field intentionally omitted (V3 doesn't provide it)

## Requirements

### 1. New Metrics

| Metric | Description |
|--------|-------------|
| `highlightsWithNotes` | Count of highlights where `note IS NOT NULL AND note != ''` |
| `notePercentage` | `highlightsWithNotes / totalHighlights * 100` |
| `highlightsThisPeriod` | Already exists |
| `highlightsPreviousPeriod` | Count from previous equivalent period (for trend calculation) |

### 2. Enhanced Most Highlighted Documents

Add `hasNotes: Boolean` to each document in the "most highlighted" list — indicates if any highlight for that document has a note.

---

## Implementation

### File: `AnalyticsRepository.kt`

**Add query: `getHighlightsWithNotesCount()`**
```sql
SELECT COUNT(*)
FROM highlights
WHERE note IS NOT NULL AND note != ''
```

**Add query: `getHighlightCountForPeriod(startDate, endDate)`**
```sql
SELECT COUNT(*)
FROM highlights
WHERE highlighted_at >= :startDate AND highlighted_at < :endDate
```

**Modify query: `getMostHighlightedDocuments()`**

Add `hasNotes` field:
```sql
SELECT
    d.readwise_id AS document_id,
    d.title,
    d.category,
    COUNT(h.id) AS highlight_count,
    BOOL_OR(h.note IS NOT NULL AND h.note != '') AS has_notes
FROM documents d
JOIN highlights h ON h.document_readwise_id = d.readwise_id
GROUP BY d.id, d.readwise_id, d.title, d.category
ORDER BY highlight_count DESC
LIMIT :limit
```

### File: `HighlightStats.kt`

Update projection class:
```kotlin
data class HighlightStats(
    val totalHighlights: Int,
    val highlightsThisPeriod: Int,
    val highlightsPreviousPeriod: Int,        // NEW
    val highlightsWithNotes: Int,              // NEW
    val notePercentage: Double,                // NEW
    val averageHighlightsPerDocument: Double,
    val mostHighlightedDocuments: List<DocumentHighlightCount>
)
```

### File: `DocumentHighlightCount.kt`

Add field:
```kotlin
data class DocumentHighlightCount(
    val documentId: String,
    val title: String,
    val category: String?,
    val highlightCount: Int,
    val hasNotes: Boolean                      // NEW
)
```

### File: `JpaAnalyticsStore.kt`

Update `getHighlightStats()` to:
1. Call new repository methods
2. Calculate `notePercentage`
3. Calculate previous period based on date range

### File: `HighlightResponse.kt`

Update API response DTO:
```kotlin
data class HighlightSummaryDto(
    val total: Int,
    val thisPeriod: Int,
    val previousPeriod: Int,                   // NEW
    val periodChange: Int,                     // NEW (thisPeriod - previousPeriod)
    val periodChangePercent: Double,           // NEW
    val withNotes: Int,                        // NEW
    val notePercentage: Double,                // NEW
    val averagePerDocument: Double
)

data class TopDocumentDto(
    val documentId: String,
    val title: String,
    val category: String?,
    val highlightCount: Int,
    val hasNotes: Boolean                      // NEW
)
```

**Remove:** `colorDistribution` field (or keep as deprecated empty list for backwards compatibility)

### File: `AnalyticsController.kt`

Update the mapping in `getHighlights()` to include new fields.

---

## Testing

### Unit Tests (FakeAnalyticsStore)

1. `highlightsWithNotes returns correct count`
2. `notePercentage calculated correctly`
3. `previousPeriod uses correct date range`
4. `periodChange calculated correctly`
5. `hasNotes true when document has annotated highlights`
6. `hasNotes false when document has no notes`

### Repository Tests (@DataJpaTest)

1. `getHighlightsWithNotesCount excludes empty strings`
2. `getHighlightCountForPeriod respects date boundaries`
3. `getMostHighlightedDocuments includes hasNotes correctly`

---

## Files to Modify

```
backend/src/main/kotlin/com/reader/analytics/
├── analytics/
│   ├── domain/
│   │   ├── HighlightStats.kt              # Add new fields
│   │   └── DocumentHighlightCount.kt      # Add hasNotes
│   ├── application/
│   │   └── AnalyticsStore.kt              # Update interface if needed
│   └── infrastructure/persistence/
│       ├── AnalyticsRepository.kt         # New queries
│       └── JpaAnalyticsStore.kt           # Wire up new queries
└── api/dto/
    └── HighlightResponse.kt               # Update response DTOs
```

---

## API Response Example

```json
{
  "summary": {
    "total": 127,
    "thisPeriod": 23,
    "previousPeriod": 15,
    "periodChange": 8,
    "periodChangePercent": 53.3,
    "withNotes": 42,
    "notePercentage": 33.1,
    "averagePerDocument": 3.2
  },
  "topDocuments": [
    {
      "documentId": "abc123",
      "title": "How to Build a Second Brain",
      "category": "article",
      "highlightCount": 24,
      "hasNotes": true
    }
  ]
}
```

---

## Out of Scope

- Recent highlights feed (future feature)
- Color distribution (V3 API doesn't provide)
- Highlight text in API response (privacy/size considerations)
