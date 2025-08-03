-- Sync queue for rate-limited processing
CREATE TABLE sync_queue
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID REFERENCES auth.users (id),
    sync_type     TEXT NOT NULL,                      -- 'documents', 'highlights', 'document_detail'
    resource_id   TEXT,                               -- Optional: specific document ID
    priority      INTEGER          DEFAULT 5,         -- 1-10, lower is higher priority
    attempts      INTEGER          DEFAULT 0,
    max_attempts  INTEGER          DEFAULT 3,
    status        TEXT             DEFAULT 'pending', -- 'pending', 'processing', 'completed', 'failed'
    scheduled_for TIMESTAMPTZ      DEFAULT NOW(),
    processed_at  TIMESTAMPTZ,
    error_message TEXT,
    created_at    TIMESTAMPTZ      DEFAULT NOW()
);

CREATE INDEX idx_queue_status_scheduled ON sync_queue(status, scheduled_for);
CREATE INDEX idx_queue_user_status ON sync_queue(user_id, status);
