CREATE TABLE planning_task (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, title VARCHAR(512) NOT NULL,
    description TEXT, status VARCHAR(24) NOT NULL DEFAULT 'INBOX', priority VARCHAR(24) NOT NULL DEFAULT 'NONE', important BOOLEAN NOT NULL DEFAULT FALSE, urgent BOOLEAN NOT NULL DEFAULT FALSE,
    scheduled_start TIMESTAMPTZ, scheduled_end TIMESTAMPTZ, deadline TIMESTAMPTZ, estimated_duration_minutes INTEGER, project_id UUID, section_id UUID, parent_task_id UUID, completed_at TIMESTAMPTZ,
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
CREATE TABLE planning_project_section (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), project_id UUID NOT NULL, name VARCHAR(256) NOT NULL, position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, UNIQUE (project_id, name), CHECK (position >= 0), CHECK (version >= 0), FOREIGN KEY (project_id) REFERENCES planning_project(id) ON DELETE CASCADE
);
CREATE INDEX idx_planning_project_section_project_position ON planning_project_section (project_id, position);
ALTER TABLE planning_task ADD CONSTRAINT fk_planning_task_section FOREIGN KEY (section_id) REFERENCES planning_project_section(id) ON DELETE SET NULL;

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

CREATE TABLE planning_comment (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), author_id UUID NOT NULL, target_type VARCHAR(16) NOT NULL, target_id UUID NOT NULL,
    content TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    deleted_at TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0, CHECK (target_type IN ('TASK','PROJECT','GOAL')), CHECK (version >= 0)
);
CREATE INDEX idx_planning_comment_target_created ON planning_comment (target_type, target_id, created_at);

CREATE TABLE planning_important_date (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, title VARCHAR(512) NOT NULL, description TEXT,
    occurs_at TIMESTAMPTZ NOT NULL, time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC', kind VARCHAR(64),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, version BIGINT NOT NULL DEFAULT 0,
    CHECK (status IN ('ACTIVE','ARCHIVED')), CHECK (version >= 0)
);
CREATE INDEX idx_planning_important_date_owner_occurs ON planning_important_date (owner_id, occurs_at);
