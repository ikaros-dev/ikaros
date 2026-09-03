CREATE TABLE ingestion_metadata_candidate (
 id UUID PRIMARY KEY DEFAULT uuid_v7(), resource_id UUID NOT NULL, field_key VARCHAR(128) NOT NULL,
 field_value TEXT NOT NULL, source VARCHAR(32) NOT NULL, source_reference VARCHAR(512), confidence INTEGER NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'PENDING', created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
 resolved_at TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT ck_ingestion_metadata_confidence CHECK (confidence BETWEEN 0 AND 100)
);
CREATE INDEX idx_ingestion_metadata_candidate_resource ON ingestion_metadata_candidate (resource_id, created_at DESC);
