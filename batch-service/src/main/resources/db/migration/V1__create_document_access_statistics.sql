-- Database migration script for document_access_statistics table
-- This table stores daily access counts per document from external systems

CREATE TABLE IF NOT EXISTS document_access_statistics (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    access_date DATE NOT NULL,
    access_count INTEGER NOT NULL DEFAULT 0,
    source_file VARCHAR(255),
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_document_access UNIQUE (document_id, access_date)
);

CREATE INDEX IF NOT EXISTS idx_document_access_date ON document_access_statistics(document_id, access_date);
CREATE INDEX IF NOT EXISTS idx_access_date ON document_access_statistics(access_date);
