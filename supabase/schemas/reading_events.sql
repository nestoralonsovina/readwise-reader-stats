-- Time-series table for reading events
CREATE TABLE reading_events
(
    time                     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_id                  UUID        NOT NULL,
    document_id              UUID REFERENCES documents (id),
    event_type               TEXT        NOT NULL, -- 'open', 'progress', 'highlight', 'archive'

    -- Event-specific data
    reading_progress         DECIMAL(3, 2),
    session_duration_seconds INTEGER,
    words_read               INTEGER,

    -- Metadata
    device_type              TEXT,
    location                 TEXT,

    PRIMARY KEY (time, user_id, document_id)
);