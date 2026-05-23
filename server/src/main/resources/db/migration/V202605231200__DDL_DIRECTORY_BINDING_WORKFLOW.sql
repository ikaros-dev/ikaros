create table if not exists "directory_binding_workflow"
(
    "id"             uuid primary key default uuidv7(),
    "task_id"        uuid,
    "directory_id"   uuid,
    "directory_name" varchar(255),
    "subject_id"     uuid,
    "platform"       varchar(255),
    "platform_id"    varchar(255),
    "status"         varchar(255),
    "current_step"   varchar(255),
    "step_statuses"  varchar(2000),
    "create_time"    timestamp(6),
    "end_time"       timestamp(6),
    "fail_message"   varchar(2000)
)
;
