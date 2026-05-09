-- Baseline: capture current schema as managed by Flyway going forward
-- This migration represents the schema as it exists before Flyway was introduced
--
-- Note: If running on an existing database, use flyway repair + baseline
-- to mark V1 as already applied. For fresh databases, this creates everything.

CREATE TABLE documents (
    id uuid NOT NULL,
    readwise_id varchar(255) NOT NULL,
    url varchar(2048) NOT NULL,
    title varchar(1024),
    author varchar(512),
    category varchar(255),
    location varchar(255),
    word_count integer,
    saved_at timestamp with time zone,
    updated_at timestamp with time zone,
    first_opened_at timestamp with time zone,
    last_opened_at timestamp with time zone,
    parent_id varchar(255),
    image_url varchar(2048),
    PRIMARY KEY (id)
);

CREATE TABLE tags (
    id uuid NOT NULL,
    name varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE document_tags (
    document_id uuid NOT NULL,
    tag_id uuid NOT NULL,
    PRIMARY KEY (document_id, tag_id),
    CONSTRAINT fk_document_tags_document FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT fk_document_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id)
);

CREATE TABLE highlights (
    id uuid NOT NULL,
    readwise_id varchar(255) NOT NULL,
    document_id uuid NOT NULL,
    text text NOT NULL,
    highlighted_at timestamp with time zone,
    PRIMARY KEY (id),
    CONSTRAINT fk_highlights_document FOREIGN KEY (document_id) REFERENCES documents (id)
);

CREATE TABLE notes (
    id uuid NOT NULL,
    readwise_id varchar(255) NOT NULL,
    document_id uuid,
    highlight_id uuid,
    content text NOT NULL,
    created_at timestamp with time zone,
    PRIMARY KEY (id),
    CONSTRAINT fk_notes_document FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT fk_notes_highlight FOREIGN KEY (highlight_id) REFERENCES highlights (id)
);

CREATE TABLE reading_progress_snapshots (
    id uuid NOT NULL,
    document_id varchar(255) NOT NULL,
    reading_progress double precision NOT NULL,
    word_count integer,
    first_opened_at timestamp with time zone,
    last_opened_at timestamp with time zone,
    recorded_at timestamp with time zone NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE location_changes (
    id uuid NOT NULL,
    document_id varchar(255) NOT NULL,
    from_location varchar(255),
    to_location varchar(255) NOT NULL,
    changed_at timestamp with time zone NOT NULL,
    category varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE sync_runs (
    id uuid NOT NULL,
    status varchar(255) NOT NULL,
    current_phase varchar(255),
    started_at timestamp with time zone NOT NULL,
    completed_at timestamp with time zone,
    total_phases integer NOT NULL,
    completed_phases integer NOT NULL,
    current_phase_progress integer NOT NULL,
    documents_processed integer NOT NULL,
    highlights_processed integer NOT NULL,
    notes_processed integer NOT NULL,
    rate_limit_hits integer NOT NULL,
    last_rate_limit_retry_seconds integer,
    last_rate_limit_attempt integer,
    error_message text,
    error_phase varchar(255),
    PRIMARY KEY (id)
);

CREATE TABLE sync_cursors (
    cursor_type varchar(255) NOT NULL,
    last_synced_at timestamp with time zone NOT NULL,
    next_page_cursor varchar(255),
    PRIMARY KEY (cursor_type)
);

CREATE TABLE sync_logs (
    id uuid NOT NULL,
    started_at timestamp with time zone NOT NULL,
    completed_at timestamp with time zone,
    status varchar(255) NOT NULL,
    documents_processed integer NOT NULL,
    highlights_processed integer NOT NULL,
    notes_processed integer NOT NULL,
    error_message varchar(255),
    PRIMARY KEY (id)
);

ALTER TABLE documents
    ADD CONSTRAINT uk_documents_readwise_id UNIQUE (readwise_id);

ALTER TABLE highlights
    ADD CONSTRAINT uk_highlights_readwise_id UNIQUE (readwise_id);

ALTER TABLE notes
    ADD CONSTRAINT uk_notes_readwise_id UNIQUE (readwise_id);

ALTER TABLE tags
    ADD CONSTRAINT uk_tags_name UNIQUE (name);