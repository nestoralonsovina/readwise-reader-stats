-- Main documents table
CREATE TABLE documents
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    readwise_id      TEXT UNIQUE NOT NULL,
    user_id          UUID REFERENCES auth.users (id),
    title            TEXT        NOT NULL,
    author           TEXT,
    source           TEXT,
    category         TEXT,
    url              TEXT,
    word_count       INTEGER,
    created_at       TIMESTAMPTZ      DEFAULT NOW(),
    updated_at       TIMESTAMPTZ      DEFAULT NOW(),
    last_synced_at   TIMESTAMPTZ,

    -- Denormalized current state
    reading_progress DECIMAL(3, 2)    DEFAULT 0,
    first_opened_at  TIMESTAMPTZ,
    last_opened_at   TIMESTAMPTZ,
    archived         BOOLEAN          DEFAULT FALSE
);

CREATE INDEX idx_documents_user_id ON documents(user_id);
CREATE INDEX idx_documents_category ON documents(category);
CREATE INDEX idx_documents_last_synced ON documents(last_synced_at);
