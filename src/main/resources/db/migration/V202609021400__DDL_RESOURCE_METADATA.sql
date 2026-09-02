CREATE TABLE resource_metadata (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    resource_id UUID NOT NULL,
    field_key VARCHAR(128) NOT NULL,
    field_value TEXT NOT NULL,
    source VARCHAR(32) NOT NULL,
    source_reference VARCHAR(255),
    manually_locked BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT uk_resource_metadata_field UNIQUE (resource_id, field_key)
);

CREATE INDEX idx_resource_metadata_resource ON resource_metadata (resource_id, field_key);
