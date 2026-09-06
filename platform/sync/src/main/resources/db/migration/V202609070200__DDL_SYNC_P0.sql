CREATE TABLE drive_device (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), user_id UUID NOT NULL, installation_id VARCHAR(256) NOT NULL,
    display_name VARCHAR(256) NOT NULL, platform VARCHAR(64) NOT NULL, app_version VARCHAR(64),
    trust_state VARCHAR(24) NOT NULL DEFAULT 'ACTIVE', registered_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, revoked_at TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0,
    CHECK (trust_state IN ('ACTIVE','LIMITED','REVOKED')), UNIQUE (user_id, installation_id)
);
CREATE INDEX idx_drive_device_user ON drive_device (user_id, trust_state, last_seen_at DESC);

CREATE TABLE offline_download_intent (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), user_id UUID NOT NULL, device_id UUID NOT NULL,
    resource_id UUID NOT NULL, attachment_id UUID, kind VARCHAR(24) NOT NULL DEFAULT 'DOWNLOAD',
    state VARCHAR(24) NOT NULL DEFAULT 'QUEUED', failure_reason VARCHAR(2000), manifest_version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, CHECK (kind IN ('DOWNLOAD','CACHE')),
    CHECK (state IN ('QUEUED','DOWNLOADING','PAUSED','VERIFYING','COMPLETED','FAILED','NEEDS_REPAIR','CANCELLED','REMOVED')),
    CHECK (manifest_version > 0), FOREIGN KEY (device_id) REFERENCES drive_device(id),
    UNIQUE (user_id, device_id, resource_id, attachment_id, kind)
);
CREATE INDEX idx_offline_download_device_state ON offline_download_intent (user_id, device_id, state, updated_at DESC);

CREATE TABLE offline_cache_entry (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), user_id UUID NOT NULL, device_id UUID NOT NULL,
    resource_id UUID NOT NULL, attachment_id UUID, size_bytes BIGINT NOT NULL DEFAULT 0,
    content_fingerprint VARCHAR(256), state VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    last_accessed_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, CHECK (size_bytes >= 0), CHECK (state IN ('ACTIVE','EVICTED')),
    FOREIGN KEY (device_id) REFERENCES drive_device(id)
);
CREATE INDEX idx_offline_cache_device_access ON offline_cache_entry (user_id, device_id, state, last_accessed_at DESC);

CREATE TABLE offline_download_manifest (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), intent_id UUID NOT NULL, manifest_version BIGINT NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, UNIQUE (intent_id, manifest_version),
    CHECK (manifest_version > 0), FOREIGN KEY (intent_id) REFERENCES offline_download_intent(id)
);
CREATE TABLE offline_download_manifest_item (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), manifest_id UUID NOT NULL, attachment_id UUID NOT NULL,
    size_bytes BIGINT NOT NULL, sha256 VARCHAR(128), required BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (size_bytes >= 0), FOREIGN KEY (manifest_id) REFERENCES offline_download_manifest(id),
    UNIQUE (manifest_id, attachment_id)
);
