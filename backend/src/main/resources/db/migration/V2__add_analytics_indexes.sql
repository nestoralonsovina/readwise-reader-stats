-- Reading progress snapshots: most critical table for analytics
CREATE INDEX idx_rps_document_recorded
    ON reading_progress_snapshots(document_id, recorded_at DESC);

CREATE INDEX idx_rps_recorded_at
    ON reading_progress_snapshots(recorded_at);

-- Documents: pipeline and breakdown queries
CREATE INDEX idx_documents_location
    ON documents(location);

CREATE INDEX idx_documents_saved_at
    ON documents(saved_at);

CREATE INDEX idx_documents_category
    ON documents(category);

-- Highlights: document joins and date filters
CREATE INDEX idx_highlights_document_id
    ON highlights(document_id);

CREATE INDEX idx_highlights_highlighted_at
    ON highlights(highlighted_at);

-- Notes: parent lookups
CREATE INDEX idx_notes_highlight_id
    ON notes(highlight_id);

CREATE INDEX idx_notes_document_id
    ON notes(document_id);

-- Sync runs: active sync check and history
CREATE INDEX idx_sync_runs_status
    ON sync_runs(status);

CREATE INDEX idx_sync_runs_started_at
    ON sync_runs(started_at DESC);