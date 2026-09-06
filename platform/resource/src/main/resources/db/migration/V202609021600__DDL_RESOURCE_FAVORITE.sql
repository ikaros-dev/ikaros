CREATE TABLE resource_favorite (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    owner_id UUID NOT NULL,
    resource_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_resource_favorite_owner_resource UNIQUE (owner_id, resource_id)
);

CREATE INDEX idx_resource_favorite_owner_created ON resource_favorite (owner_id, created_at DESC);
