CREATE TABLE ingestion_source (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    owner_id UUID NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    display_name VARCHAR(256) NOT NULL,
    root_reference VARCHAR(1024) NOT NULL,
    credential_reference VARCHAR(512),
    scan_policy_json TEXT NOT NULL DEFAULT '{}',
    status VARCHAR(24) NOT NULL DEFAULT 'ENABLED',
    last_successful_scan TIMESTAMPTZ,
    health_status VARCHAR(24) NOT NULL DEFAULT 'UNKNOWN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_ingestion_source_credential CHECK (credential_reference IS NULL OR credential_reference LIKE 'secret://%')
);

CREATE INDEX idx_ingestion_source_owner_updated ON ingestion_source (owner_id, updated_at DESC);
