create table notification_preference
(
    recipient_id          uuid primary key,
    task_success_enabled  boolean not null default true,
    task_failure_enabled  boolean not null default true,
    version               bigint not null default 0,
    updated_at            timestamptz not null default current_timestamp,
    constraint notification_preference_version_ck check (version >= 0)
);
