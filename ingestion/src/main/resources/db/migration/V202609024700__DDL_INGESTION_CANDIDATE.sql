CREATE TABLE ingestion_candidate (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    scan_run_id UUID NOT NULL,
    source_id UUID NOT NULL,
    suggested_resource_type VARCHAR(64) NOT NULL,
    title_hint VARCHAR(512),
    external_id_hint VARCHAR(512),
    confidence INTEGER NOT NULL,
    fingerprint VARCHAR(512) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_ingestion_candidate_confidence CHECK (confidence BETWEEN 0 AND 100),
    CONSTRAINT uk_ingestion_candidate_fingerprint UNIQUE (scan_run_id, fingerprint)
);

CREATE INDEX idx_ingestion_candidate_scan_created ON ingestion_candidate (scan_run_id, created_at);
