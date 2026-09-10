CREATE TABLE IF NOT EXISTS metadata_sync_status (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    owner_id UUID NOT NULL,
    sync_source_id UUID NOT NULL REFERENCES metadata_sync_source(id),
    resource_id UUID NOT NULL,
    field_key VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    candidate_id UUID,
    checked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT metadata_sync_status_status_ck CHECK (status IN ('UNCHANGED', 'CANDIDATE_CREATED'))
);
CREATE INDEX IF NOT EXISTS metadata_sync_status_recent_idx
    ON metadata_sync_status (owner_id, sync_source_id, checked_at DESC);
