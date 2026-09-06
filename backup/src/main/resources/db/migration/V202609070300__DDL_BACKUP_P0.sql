CREATE TABLE backup_restore_point (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), format_version VARCHAR(64) NOT NULL, source_instance_id VARCHAR(256) NOT NULL,
    schema_version VARCHAR(64) NOT NULL, manifest_digest VARCHAR(256) NOT NULL, state VARCHAR(24) NOT NULL DEFAULT 'PREPARING',
    verification_level VARCHAR(32) NOT NULL, verification_status VARCHAR(24) NOT NULL DEFAULT 'NOT_VERIFIED',
    failure_reason VARCHAR(2000), checked_objects BIGINT NOT NULL DEFAULT 0, failed_objects BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, published_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0, CHECK (state IN ('PREPARING','VERIFYING','PUBLISHED','FAILED','RETIRED')),
    CHECK (verification_level IN ('MANIFEST_ONLY','STRUCTURAL','CONTENT_SAMPLE','CONTENT_FULL','RESTORE_DRILL')),
    CHECK (verification_status IN ('NOT_VERIFIED','PASSED','FAILED')), CHECK (checked_objects >= 0), CHECK (failed_objects >= 0)
);
CREATE INDEX idx_backup_restore_point_created ON backup_restore_point (state, created_at DESC);
