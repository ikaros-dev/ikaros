CREATE TABLE resource_tag (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    owner_id UUID NOT NULL,
    resource_id UUID NOT NULL,
    name VARCHAR(128) NOT NULL,
    color VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_resource_tag_owner_resource_name UNIQUE (owner_id, resource_id, name)
);

CREATE INDEX idx_resource_tag_owner_created ON resource_tag (owner_id, created_at DESC);
