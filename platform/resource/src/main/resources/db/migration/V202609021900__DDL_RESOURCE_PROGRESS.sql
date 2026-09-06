CREATE TABLE resource_progress (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    owner_id UUID NOT NULL,
    resource_id UUID NOT NULL,
    progress_type VARCHAR(32) NOT NULL,
    position_value BIGINT NOT NULL DEFAULT 0,
    total_value BIGINT,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_resource_progress_owner_resource_type UNIQUE (owner_id, resource_id, progress_type),
    CONSTRAINT ck_resource_progress_position_nonnegative CHECK (position_value >= 0),
    CONSTRAINT ck_resource_progress_total_positive CHECK (total_value IS NULL OR total_value > 0)
);

CREATE INDEX idx_resource_progress_owner_updated ON resource_progress (owner_id, updated_at DESC);
