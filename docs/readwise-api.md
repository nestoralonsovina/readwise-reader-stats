# Readwise API Reference

## Reader API (v3)

Documents and articles from the Reader app.

- **Documentation:** https://readwise.io/reader_api
- **Base URL:** `https://readwise.io/api/v3/`
- **Rate Limit:** 20 req/min (50 for create/update)

| Endpoint | Method | Path |
|----------|--------|------|
| Create Document | POST | `/save/` |
| List Documents | GET | `/list/` |
| Update Document | PATCH | `/update/{id}/` |
| Delete Document | DELETE | `/delete/{id}/` |
| List Tags | GET | `/tags/` |

### List Documents Parameters

| Parameter | Description |
|-----------|-------------|
| `updatedAfter` | ISO timestamp for incremental sync |
| `location` | Filter: `new`, `later`, `shortlist`, `archive`, `feed` |
| `category` | Filter: `article`, `email`, `rss`, `highlight`, `note`, `pdf`, `epub`, `tweet`, `video` |
| `pageCursor` | Pagination cursor from previous response |

### Document Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique Readwise ID |
| `url` | string | Original URL |
| `title` | string | Document title |
| `author` | string | Author name |
| `source` | string | Source (e.g., "reader-web") |
| `category` | string | Document type |
| `location` | string | Queue location |
| `reading_progress` | float | 0.0 to 1.0 |
| `word_count` | int | Estimated word count |
| `first_opened_at` | string | ISO timestamp |
| `last_opened_at` | string | ISO timestamp |
| `saved_at` | string | ISO timestamp |
| `updated_at` | string | ISO timestamp |
| `parent_id` | string | For highlights/notes: parent document ID |
| `content` | string | For notes: the note text |
| `image_url` | string | Cover image URL |
| `tags` | object | Tag name → tag object map |

## Highlights API (v2)

Highlights from Kindle, books, articles, and other sources.

- **Documentation:** https://readwise.io/api_deets
- **Base URL:** `https://readwise.io/api/v2/`
- **Rate Limit:** 240 req/min (20 for list endpoints)

| Endpoint | Method | Path |
|----------|--------|------|
| Create Highlights | POST | `/highlights/` |
| List Highlights | GET | `/highlights/` |
| Export Highlights | GET | `/export/` |
| List Books | GET | `/books/` |
| Daily Review | GET | `/review/` |

**Note:** This project primarily uses Reader API (v3). Highlights API is documented for reference.

## Authentication

All endpoints require header:
```
Authorization: Token {ACCESS_TOKEN}
```

## Rate Limiting

When rate limited (HTTP 429), response body contains:
```
Expected available in X seconds
```

The `RateLimitRetryHandler` parses this and waits accordingly, with exponential backoff fallback.

## API Testing

Bruno collection available at `bruno/readwise/` with requests for all endpoints.
