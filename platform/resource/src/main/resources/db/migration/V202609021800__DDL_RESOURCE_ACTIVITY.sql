CREATE TABLE resource_activity (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    owner_id UUID NOT NULL,
    resource_id UUID NOT NULL,
    activity_type VARCHAR(32) NOT NULL,
    details VARCHAR(2000) NOT NULL DEFAULT '{}',
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_resource_activity_owner_occurred ON resource_activity (owner_id, occurred_at DESC);
CREATE INDEX idx_resource_activity_resource_occurred ON resource_activity (resource_id, occurred_at DESC);
