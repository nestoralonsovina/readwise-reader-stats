-- Sync status tracking
CREATE TABLE sync_status
(
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID REFERENCES auth.users (id) UNIQUE,
    last_full_sync        TIMESTAMPTZ,
    last_incremental_sync TIMESTAMPTZ,
    next_cursor           TEXT,
    sync_in_progress      BOOLEAN          DEFAULT FALSE,
    documents_synced      INTEGER          DEFAULT 0,
    highlights_synced     INTEGER          DEFAULT 0,
    failed_attempts       INTEGER          DEFAULT 0,
    last_error            TEXT,
    created_at            TIMESTAMPTZ      DEFAULT NOW(),
    updated_at            TIMESTAMPTZ      DEFAULT NOW()
);