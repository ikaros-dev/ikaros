CREATE TABLE metadata_sync_source (
 id UUID PRIMARY KEY DEFAULT uuid_v7(),
 owner_id UUID NOT NULL,
 provider_key VARCHAR(128) NOT NULL,
 display_name VARCHAR(256) NOT NULL,
 credential_reference VARCHAR(512),
 refresh_schedule VARCHAR(32) NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
 created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT ck_metadata_sync_source_schedule CHECK (refresh_schedule IN ('MANUAL', 'HOURLY', 'DAILY')),
 CONSTRAINT ck_metadata_sync_source_status CHECK (status IN ('ENABLED', 'DISABLED')),
 CONSTRAINT uq_metadata_sync_source_owner_provider UNIQUE (owner_id, provider_key)
);
CREATE INDEX idx_metadata_sync_source_owner_updated ON metadata_sync_source (owner_id, updated_at DESC);
