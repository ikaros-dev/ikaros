CREATE TABLE IF NOT EXISTS offline_cache_quota (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), user_id UUID NOT NULL, device_id UUID NOT NULL,
    quota_bytes BIGINT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, version BIGINT NOT NULL DEFAULT 0,
    CHECK (quota_bytes > 0), FOREIGN KEY (device_id) REFERENCES drive_device(id),
    UNIQUE (user_id, device_id)
);
