CREATE TABLE ingestion_discovered_item (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    source_id UUID NOT NULL,
    scan_run_id UUID NOT NULL,
    relative_key VARCHAR(2048) NOT NULL,
    size_bytes BIGINT NOT NULL,
    modified_at TIMESTAMPTZ,
    etag VARCHAR(512),
    media_type VARCHAR(256),
    availability VARCHAR(24) NOT NULL,
    scan_generation BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_ingestion_item_size CHECK (size_bytes >= 0),
    CONSTRAINT ck_ingestion_item_generation CHECK (scan_generation >= 0),
    CONSTRAINT uk_ingestion_item_scan_key UNIQUE (scan_run_id, relative_key)
);

CREATE INDEX idx_ingestion_item_source_key ON ingestion_discovered_item (source_id, relative_key);
