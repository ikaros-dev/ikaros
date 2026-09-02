CREATE TABLE ingestion_scan_run (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    source_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    trigger VARCHAR(64) NOT NULL,
    actor_id UUID,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    checkpoint VARCHAR(2048),
    discovered_count BIGINT NOT NULL DEFAULT 0,
    changed_count BIGINT NOT NULL DEFAULT 0,
    skipped_count BIGINT NOT NULL DEFAULT 0,
    error_summary VARCHAR(2000),
    background_task_id UUID,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_ingestion_scan_counts CHECK (discovered_count >= 0 AND changed_count >= 0 AND skipped_count >= 0)
);

CREATE INDEX idx_ingestion_scan_owner_created ON ingestion_scan_run (owner_id, created_at DESC);
CREATE INDEX idx_ingestion_scan_source_created ON ingestion_scan_run (source_id, created_at DESC);
