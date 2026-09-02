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
    scheduled_start TIMESTAMPTZ, scheduled_end TIMESTAMPTZ, deadline TIMESTAMPTZ, estimated_duration_minutes INTEGER, project_id UUID, parent_task_id UUID, completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, CHECK (status IN ('INBOX','PLANNED','IN_PROGRESS','COMPLETED','BLOCKED','CANCELLED','ARCHIVED')),
    CHECK (priority IN ('NONE','LOW','MEDIUM','HIGH','URGENT')), CHECK (scheduled_end IS NULL OR scheduled_start IS NULL OR scheduled_end > scheduled_start), CHECK (estimated_duration_minutes IS NULL OR estimated_duration_minutes > 0), CHECK (version >= 0),
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

CREATE TABLE planning_recurrence (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, task_id UUID NOT NULL,
    rule VARCHAR(512) NOT NULL, mode VARCHAR(32) NOT NULL DEFAULT 'FIXED_SCHEDULE', time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    next_run_at TIMESTAMPTZ, active BOOLEAN NOT NULL DEFAULT TRUE, last_run_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, UNIQUE (owner_id, task_id),
    CHECK (mode IN ('FIXED_SCHEDULE','COMPLETION_BASED')), CHECK (version >= 0),
    FOREIGN KEY (task_id) REFERENCES planning_task(id) ON DELETE CASCADE
);
CREATE INDEX idx_planning_recurrence_due ON planning_recurrence (active, next_run_at);

CREATE TABLE planning_time_block (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, title VARCHAR(512) NOT NULL,
    task_id UUID, start_at TIMESTAMPTZ NOT NULL, end_at TIMESTAMPTZ NOT NULL,
    kind VARCHAR(16) NOT NULL DEFAULT 'FIXED', status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE', time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, CHECK (end_at > start_at), CHECK (kind IN ('FIXED','FLEXIBLE')),
    CHECK (status IN ('ACTIVE','CANCELLED')), CHECK (version >= 0), FOREIGN KEY (task_id) REFERENCES planning_task(id) ON DELETE SET NULL
);
CREATE INDEX idx_planning_time_block_owner_time ON planning_time_block (owner_id, start_at, end_at);

CREATE TABLE planning_reminder (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, target_type VARCHAR(32) NOT NULL, target_id UUID NOT NULL,
    trigger_at TIMESTAMPTZ NOT NULL, time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC', channel VARCHAR(32) NOT NULL DEFAULT 'IN_APP',
    status VARCHAR(24) NOT NULL DEFAULT 'SCHEDULED', snoozed_until TIMESTAMPTZ, fired_at TIMESTAMPTZ, acknowledged_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, CHECK (target_type IN ('TASK','TIME_BLOCK','PROJECT','GOAL','IMPORTANT_DATE')),
    CHECK (status IN ('SCHEDULED','SNOOZED','FIRED','ACKNOWLEDGED','CANCELLED')), CHECK (version >= 0)
);
CREATE INDEX idx_planning_reminder_due ON planning_reminder (status, trigger_at);
CREATE INDEX idx_planning_reminder_owner ON planning_reminder (owner_id, trigger_at);

CREATE TABLE planning_goal (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, title VARCHAR(512) NOT NULL, description TEXT,
    type VARCHAR(24) NOT NULL DEFAULT 'OUTCOME', status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE', progress DOUBLE PRECISION NOT NULL DEFAULT 0,
    start_at TIMESTAMPTZ, deadline TIMESTAMPTZ, completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, CHECK (type IN ('OUTCOME','HABIT','PROJECT','PERSONAL','WORK')),
    CHECK (status IN ('ACTIVE','COMPLETED','ABANDONED','ARCHIVED')), CHECK (progress >= 0 AND progress <= 1),
    CHECK (deadline IS NULL OR start_at IS NULL OR deadline > start_at), CHECK (version >= 0)
);
CREATE TABLE planning_goal_task (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), goal_id UUID NOT NULL, task_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, UNIQUE (goal_id, task_id),
    FOREIGN KEY (goal_id) REFERENCES planning_goal(id) ON DELETE CASCADE, FOREIGN KEY (task_id) REFERENCES planning_task(id) ON DELETE CASCADE
);
CREATE INDEX idx_planning_goal_owner_status ON planning_goal (owner_id, status, updated_at DESC);
CREATE INDEX idx_planning_goal_task_task ON planning_goal_task (task_id, goal_id);

CREATE TABLE planning_task_dependency (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), task_id UUID NOT NULL, depends_on_task_id UUID NOT NULL,
    type VARCHAR(16) NOT NULL DEFAULT 'BLOCKED_BY', created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    UNIQUE (task_id, depends_on_task_id), CHECK (task_id <> depends_on_task_id), CHECK (type IN ('BLOCKS','BLOCKED_BY')),
    FOREIGN KEY (task_id) REFERENCES planning_task(id) ON DELETE CASCADE,
    FOREIGN KEY (depends_on_task_id) REFERENCES planning_task(id) ON DELETE CASCADE
);
CREATE INDEX idx_planning_task_dependency_reverse ON planning_task_dependency (depends_on_task_id, task_id);

CREATE TABLE planning_time_entry (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, task_id UUID NOT NULL, duration_minutes INTEGER NOT NULL,
    started_at TIMESTAMPTZ, ended_at TIMESTAMPTZ, source VARCHAR(16) NOT NULL DEFAULT 'MANUAL', note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, CHECK (duration_minutes > 0), CHECK (ended_at IS NULL OR started_at IS NULL OR ended_at > started_at),
    CHECK (source IN ('MANUAL','FOCUS','IMPORTED')), FOREIGN KEY (task_id) REFERENCES planning_task(id) ON DELETE CASCADE
);
CREATE INDEX idx_planning_time_entry_task_created ON planning_time_entry (owner_id, task_id, created_at DESC);

CREATE TABLE planning_focus_session (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, task_id UUID,
    mode VARCHAR(16) NOT NULL DEFAULT 'FREEFORM', status VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
    planned_minutes INTEGER, actual_minutes INTEGER, started_at TIMESTAMPTZ NOT NULL, ended_at TIMESTAMPTZ, note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, version BIGINT NOT NULL DEFAULT 0,
    CHECK (mode IN ('FREEFORM','POMODORO')), CHECK (status IN ('RUNNING','COMPLETED','CANCELLED')),
    CHECK (planned_minutes IS NULL OR planned_minutes > 0), CHECK (actual_minutes IS NULL OR actual_minutes > 0), CHECK (version >= 0),
    FOREIGN KEY (task_id) REFERENCES planning_task(id) ON DELETE SET NULL
);
CREATE INDEX idx_planning_focus_session_owner_started ON planning_focus_session (owner_id, started_at DESC);

CREATE TABLE planning_habit (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, name VARCHAR(256) NOT NULL, description TEXT,
    metric VARCHAR(16) NOT NULL DEFAULT 'BOOLEAN', target_value DOUBLE PRECISION, schedule VARCHAR(256) NOT NULL,
    time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC', start_at TIMESTAMPTZ, status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, CHECK (metric IN ('BOOLEAN','COUNT','DURATION','NUMERIC')), CHECK (target_value IS NULL OR target_value > 0),
    CHECK (status IN ('ACTIVE','ARCHIVED')), CHECK (version >= 0)
);
CREATE TABLE planning_habit_check_in (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, habit_id UUID NOT NULL, value DOUBLE PRECISION NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, note TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    FOREIGN KEY (habit_id) REFERENCES planning_habit(id) ON DELETE CASCADE
);
CREATE INDEX idx_planning_habit_owner_created ON planning_habit (owner_id, created_at DESC);
CREATE INDEX idx_planning_habit_check_in_habit_occurred ON planning_habit_check_in (owner_id, habit_id, occurred_at DESC);

CREATE TABLE planning_milestone (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, title VARCHAR(512) NOT NULL, description TEXT,
    goal_id UUID NOT NULL, project_id UUID, due_at TIMESTAMPTZ, status VARCHAR(16) NOT NULL DEFAULT 'OPEN', achieved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, CHECK (status IN ('OPEN','ACHIEVED','MISSED','ARCHIVED')), CHECK (version >= 0),
    FOREIGN KEY (goal_id) REFERENCES planning_goal(id) ON DELETE CASCADE, FOREIGN KEY (project_id) REFERENCES planning_project(id) ON DELETE SET NULL
);
CREATE INDEX idx_planning_milestone_owner_due ON planning_milestone (owner_id, due_at);
CREATE INDEX idx_planning_milestone_goal_due ON planning_milestone (goal_id, due_at);

CREATE TABLE planning_review (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, period VARCHAR(16) NOT NULL,
    period_start TIMESTAMPTZ NOT NULL, period_end TIMESTAMPTZ NOT NULL, note TEXT NOT NULL,
    wins TEXT, challenges TEXT, next_focus TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (owner_id, period, period_start), CHECK (period IN ('DAILY','WEEKLY','MONTHLY','QUARTERLY')),
    CHECK (period_end > period_start), CHECK (version >= 0)
);
CREATE INDEX idx_planning_review_owner_period ON planning_review (owner_id, period, period_start DESC);

CREATE TABLE planning_okr_cycle (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, name VARCHAR(128) NOT NULL,
    start_at TIMESTAMPTZ NOT NULL, end_at TIMESTAMPTZ NOT NULL, status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, CHECK (end_at > start_at), CHECK (status IN ('DRAFT','ACTIVE','COMPLETED','ARCHIVED')), CHECK (version >= 0)
);
CREATE TABLE planning_okr_objective (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, cycle_id UUID NOT NULL, title VARCHAR(512) NOT NULL,
    description TEXT, goal_id UUID, status VARCHAR(16) NOT NULL DEFAULT 'DRAFT', created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, version BIGINT NOT NULL DEFAULT 0,
    CHECK (status IN ('DRAFT','ACTIVE','COMPLETED','ARCHIVED')), CHECK (version >= 0), FOREIGN KEY (cycle_id) REFERENCES planning_okr_cycle(id) ON DELETE CASCADE,
    FOREIGN KEY (goal_id) REFERENCES planning_goal(id) ON DELETE SET NULL
);
CREATE TABLE planning_okr_key_result (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, objective_id UUID NOT NULL, title VARCHAR(512) NOT NULL,
    metric_type VARCHAR(16) NOT NULL, start_value DOUBLE PRECISION NOT NULL, target_value DOUBLE PRECISION NOT NULL,
    current_value DOUBLE PRECISION NOT NULL, status VARCHAR(16) NOT NULL DEFAULT 'DRAFT', created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, version BIGINT NOT NULL DEFAULT 0,
    CHECK (metric_type IN ('NUMERIC','PERCENTAGE','BOOLEAN','MILESTONE')), CHECK (start_value <> target_value), CHECK (status IN ('DRAFT','ACTIVE','COMPLETED','ARCHIVED')), CHECK (version >= 0),
    FOREIGN KEY (objective_id) REFERENCES planning_okr_objective(id) ON DELETE CASCADE
);
CREATE INDEX idx_planning_okr_cycle_owner_start ON planning_okr_cycle (owner_id, start_at DESC);
CREATE INDEX idx_planning_okr_objective_cycle ON planning_okr_objective (owner_id, cycle_id, created_at DESC);
CREATE INDEX idx_planning_okr_key_result_objective ON planning_okr_key_result (owner_id, objective_id, created_at DESC);

CREATE TABLE planning_okr_check_in (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, key_result_id UUID NOT NULL,
    current_value DOUBLE PRECISION NOT NULL, progress DOUBLE PRECISION NOT NULL, confidence VARCHAR(16) NOT NULL,
    note TEXT, blocker TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    CHECK (progress >= 0 AND progress <= 1), CHECK (confidence IN ('ON_TRACK','AT_RISK','OFF_TRACK')),
    FOREIGN KEY (key_result_id) REFERENCES planning_okr_key_result(id) ON DELETE CASCADE
);
CREATE INDEX idx_planning_okr_check_in_key_result ON planning_okr_check_in (owner_id, key_result_id, created_at DESC);

CREATE TABLE planning_project_member (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), project_id UUID NOT NULL, user_id UUID NOT NULL,
    role VARCHAR(24) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    UNIQUE (project_id, user_id), CHECK (role IN ('VIEW','COMMENT','EDIT_TASK','ASSIGN','MANAGE_PROJECT')),
    FOREIGN KEY (project_id) REFERENCES planning_project(id) ON DELETE CASCADE
);
CREATE INDEX idx_planning_project_member_user ON planning_project_member (user_id, project_id);

CREATE TABLE planning_task_assignment (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), task_id UUID NOT NULL, assignee_id UUID NOT NULL,
    assigned_by UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    UNIQUE (task_id, assignee_id), FOREIGN KEY (task_id) REFERENCES planning_task(id) ON DELETE CASCADE
);
CREATE INDEX idx_planning_task_assignment_assignee ON planning_task_assignment (assignee_id, task_id);

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
