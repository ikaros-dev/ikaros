CREATE TABLE drive_space (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_user_id UUID NOT NULL, display_name VARCHAR(256) NOT NULL,
    root_node_id UUID, change_generation BIGINT NOT NULL DEFAULT 0, state VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, CHECK (change_generation >= 0), CHECK (version >= 0)
);

CREATE TABLE drive_node (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), drive_space_id UUID NOT NULL, parent_id UUID,
    node_type VARCHAR(16) NOT NULL, name VARCHAR(512) NOT NULL, normalized_name VARCHAR(512) NOT NULL,
    lifecycle VARCHAR(24) NOT NULL DEFAULT 'ACTIVE', current_revision_id UUID, created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    trashed_at TIMESTAMPTZ, node_version BIGINT NOT NULL DEFAULT 0, version BIGINT NOT NULL DEFAULT 0,
    CHECK (node_type IN ('FILE','FOLDER')), CHECK (lifecycle IN ('ACTIVE','TRASHED','PURGED')),
    CHECK (node_version >= 0), CHECK (version >= 0)
);
ALTER TABLE drive_space ADD CONSTRAINT fk_drive_space_root FOREIGN KEY (root_node_id) REFERENCES drive_node(id);
ALTER TABLE drive_node ADD CONSTRAINT fk_drive_node_space FOREIGN KEY (drive_space_id) REFERENCES drive_space(id);
ALTER TABLE drive_node ADD CONSTRAINT fk_drive_node_parent FOREIGN KEY (parent_id) REFERENCES drive_node(id);
CREATE UNIQUE INDEX uk_drive_node_active_name ON drive_node (drive_space_id, parent_id, normalized_name) WHERE lifecycle = 'ACTIVE';
CREATE INDEX idx_drive_node_children ON drive_node (drive_space_id, parent_id, lifecycle, normalized_name);

CREATE TABLE drive_file_revision (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), file_node_id UUID NOT NULL, revision_no BIGINT NOT NULL,
    attachment_id UUID NOT NULL, content_fingerprint VARCHAR(128), content_modified_at TIMESTAMPTZ,
    created_by UUID NOT NULL, operation_id VARCHAR(256), created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (file_node_id, revision_no), CHECK (revision_no > 0), CHECK (version >= 0),
    FOREIGN KEY (file_node_id) REFERENCES drive_node(id)
);
CREATE UNIQUE INDEX uk_drive_revision_operation ON drive_file_revision (file_node_id, operation_id) WHERE operation_id IS NOT NULL;

CREATE TABLE drive_quota (
    space_id UUID PRIMARY KEY, limit_bytes BIGINT NOT NULL, used_bytes BIGINT NOT NULL DEFAULT 0,
    reserved_bytes BIGINT NOT NULL DEFAULT 0, version BIGINT NOT NULL DEFAULT 0,
    CHECK (limit_bytes >= 0), CHECK (used_bytes >= 0), CHECK (reserved_bytes >= 0),
    FOREIGN KEY (space_id) REFERENCES drive_space(id)
);

CREATE TABLE drive_sync_binding (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), user_id UUID NOT NULL, device_id UUID NOT NULL,
    drive_space_id UUID NOT NULL, remote_root_node_id UUID NOT NULL, local_scope_id VARCHAR(512) NOT NULL,
    local_display_path VARCHAR(2000), source_kind VARCHAR(32) NOT NULL, mode VARCHAR(32) NOT NULL,
    delete_policy VARCHAR(32) NOT NULL DEFAULT 'KEEP_REMOTE', conflict_policy VARCHAR(32) NOT NULL DEFAULT 'PRESERVE_BOTH',
    enabled BOOLEAN NOT NULL DEFAULT TRUE, state VARCHAR(24) NOT NULL DEFAULT 'ACTIVE', cursor BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, CHECK (cursor >= 0),
    CHECK (source_kind IN ('DIRECTORY','CAMERA_ROLL','MEDIA_COLLECTION')),
    CHECK (mode IN ('BACKUP','TWO_WAY','UPLOAD_ONLY','DOWNLOAD_ONLY')),
    CHECK (delete_policy IN ('KEEP_REMOTE','TRASH_REMOTE','PROPAGATE')),
    CHECK (conflict_policy IN ('PRESERVE_BOTH','KEEP_REMOTE','KEEP_LOCAL')),
    FOREIGN KEY (drive_space_id) REFERENCES drive_space(id), FOREIGN KEY (remote_root_node_id) REFERENCES drive_node(id),
    UNIQUE (user_id, device_id, local_scope_id)
);
CREATE INDEX idx_drive_sync_binding_user ON drive_sync_binding (user_id, enabled, updated_at DESC);

CREATE TABLE drive_sync_conflict (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), binding_id UUID NOT NULL, node_id UUID NOT NULL,
    base_revision_id UUID, remote_revision_id UUID, local_fingerprint VARCHAR(256), state VARCHAR(24) NOT NULL DEFAULT 'OPEN',
    detected_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, resolved_at TIMESTAMPTZ, resolved_by UUID, version BIGINT NOT NULL DEFAULT 0,
    CHECK (state IN ('OPEN','RESOLVED','DISMISSED')), FOREIGN KEY (binding_id) REFERENCES drive_sync_binding(id),
    FOREIGN KEY (node_id) REFERENCES drive_node(id)
);
CREATE INDEX idx_drive_sync_conflict_binding ON drive_sync_conflict (binding_id, state, detected_at DESC);

CREATE TABLE drive_device (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), user_id UUID NOT NULL, installation_id VARCHAR(256) NOT NULL,
    display_name VARCHAR(256) NOT NULL, platform VARCHAR(64) NOT NULL, app_version VARCHAR(64),
    trust_state VARCHAR(24) NOT NULL DEFAULT 'ACTIVE', registered_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, revoked_at TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0,
    CHECK (trust_state IN ('ACTIVE','LIMITED','REVOKED')), UNIQUE (user_id, installation_id)
);
CREATE INDEX idx_drive_device_user ON drive_device (user_id, trust_state, last_seen_at DESC);
ALTER TABLE drive_sync_binding ADD CONSTRAINT fk_drive_sync_binding_device FOREIGN KEY (device_id) REFERENCES drive_device(id);

CREATE TABLE drive_sync_item_mapping (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), binding_id UUID NOT NULL, local_item_id VARCHAR(1024) NOT NULL,
    remote_node_id UUID NOT NULL, last_synced_revision_id UUID, last_synced_fingerprint VARCHAR(256),
    last_seen_remote_version BIGINT NOT NULL DEFAULT 0, state VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, version BIGINT NOT NULL DEFAULT 0,
    CHECK (last_seen_remote_version >= 0), CHECK (state IN ('ACTIVE','TOMBSTONED','CONFLICT')),
    FOREIGN KEY (binding_id) REFERENCES drive_sync_binding(id), FOREIGN KEY (remote_node_id) REFERENCES drive_node(id),
    UNIQUE (binding_id, local_item_id)
);
CREATE INDEX idx_drive_sync_mapping_binding ON drive_sync_item_mapping (binding_id, updated_at);

CREATE TABLE drive_sync_tombstone (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), drive_space_id UUID NOT NULL, node_id UUID NOT NULL,
    sequence BIGINT NOT NULL, node_version BIGINT NOT NULL, lifecycle VARCHAR(24) NOT NULL,
    previous_parent_id UUID, previous_name VARCHAR(512), deleted_at TIMESTAMPTZ NOT NULL,
    retention_deadline TIMESTAMPTZ, CHECK (sequence > 0), CHECK (node_version >= 0),
    CHECK (lifecycle IN ('TRASHED','PURGED')), FOREIGN KEY (drive_space_id) REFERENCES drive_space(id),
    FOREIGN KEY (node_id) REFERENCES drive_node(id), UNIQUE (drive_space_id, sequence)
);
CREATE INDEX idx_drive_sync_tombstone_node ON drive_sync_tombstone (drive_space_id, node_id, sequence DESC);

CREATE TABLE drive_camera_backup_mapping (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), binding_id UUID NOT NULL, source_item_id VARCHAR(1024) NOT NULL,
    state VARCHAR(48) NOT NULL, remote_node_id UUID, remote_revision_id UUID, content_fingerprint VARCHAR(256),
    error_message VARCHAR(2000), updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, version BIGINT NOT NULL DEFAULT 0,
    CHECK (state IN ('DISCOVERED','QUEUED','UPLOADING','UPLOAD_COMPLETE','BLOB_VERIFIED','DRIVE_COMMITTED','BACKUP_VERIFIED','PHOTO_PROJECTION_PENDING','PHOTO_PROJECTED','PHOTO_PROJECTION_FAILED','WAITING_FOR_LOCAL_ORIGINAL','PERMISSION_REQUIRED','SOURCE_UNAVAILABLE','IGNORED','ERROR','REMOVED_AFTER_VERIFIED_BACKUP')),
    FOREIGN KEY (binding_id) REFERENCES drive_sync_binding(id), FOREIGN KEY (remote_node_id) REFERENCES drive_node(id),
    FOREIGN KEY (remote_revision_id) REFERENCES drive_file_revision(id), UNIQUE (binding_id, source_item_id)
);
CREATE INDEX idx_drive_camera_backup_binding ON drive_camera_backup_mapping (binding_id, state, updated_at);

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

CREATE TABLE planning_task (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, title VARCHAR(512) NOT NULL,
    description TEXT, status VARCHAR(24) NOT NULL DEFAULT 'INBOX', priority VARCHAR(24) NOT NULL DEFAULT 'NONE',
    deadline TIMESTAMPTZ, project_id UUID, parent_task_id UUID, completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, CHECK (status IN ('INBOX','PLANNED','IN_PROGRESS','COMPLETED','BLOCKED','CANCELLED','ARCHIVED')),
    CHECK (priority IN ('NONE','LOW','MEDIUM','HIGH','URGENT')), CHECK (version >= 0),
    CHECK (parent_task_id IS NULL OR parent_task_id <> id), FOREIGN KEY (parent_task_id) REFERENCES planning_task(id)
);
CREATE INDEX idx_planning_task_owner_status ON planning_task (owner_id, status, updated_at DESC);

CREATE TABLE planning_project (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, name VARCHAR(512) NOT NULL,
    description TEXT, status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, CHECK (status IN ('ACTIVE','COMPLETED','ARCHIVED')), CHECK (version >= 0)
);
CREATE INDEX idx_planning_project_owner_updated ON planning_project (owner_id, updated_at DESC);
ALTER TABLE planning_task ADD CONSTRAINT fk_planning_task_project FOREIGN KEY (project_id) REFERENCES planning_project(id);

CREATE TABLE planning_tag (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, name VARCHAR(128) NOT NULL,
    color VARCHAR(32), created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    UNIQUE (owner_id, name)
);
CREATE TABLE planning_task_tag (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), task_id UUID NOT NULL, tag_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    UNIQUE (task_id, tag_id), FOREIGN KEY (task_id) REFERENCES planning_task(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES planning_tag(id) ON DELETE CASCADE
);
CREATE INDEX idx_planning_task_tag_tag ON planning_task_tag (tag_id, task_id);

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

CREATE TABLE drive_change_log (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), drive_space_id UUID NOT NULL, sequence BIGINT NOT NULL,
    node_id UUID NOT NULL, mutation_kind VARCHAR(48) NOT NULL, node_version BIGINT NOT NULL,
    revision_id UUID, occurred_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    UNIQUE (drive_space_id, sequence), CHECK (sequence > 0), CHECK (node_version >= 0),
    FOREIGN KEY (drive_space_id) REFERENCES drive_space(id), FOREIGN KEY (node_id) REFERENCES drive_node(id)
);
CREATE INDEX idx_drive_change_feed ON drive_change_log (drive_space_id, sequence);

CREATE TABLE drive_quota_reservation (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), drive_space_id UUID NOT NULL, upload_session_id UUID NOT NULL,
    reserved_bytes BIGINT NOT NULL, state VARCHAR(24) NOT NULL DEFAULT 'ACTIVE', expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, UNIQUE (drive_space_id, upload_session_id), CHECK (reserved_bytes > 0),
    CHECK (state IN ('ACTIVE','COMMITTED','RELEASED','EXPIRED')), FOREIGN KEY (drive_space_id) REFERENCES drive_space(id)
);
