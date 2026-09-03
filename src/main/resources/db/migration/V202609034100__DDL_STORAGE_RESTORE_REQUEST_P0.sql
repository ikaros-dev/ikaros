create table storage_restore_request
(
    id                   uuid primary key default uuid_v7(),
    actor_id             uuid not null,
    scope                varchar(32) not null,
    scope_id             uuid not null,
    status               varchar(32) not null default 'REQUESTED',
    total_items          integer not null default 0,
    completed_items      integer not null default 0,
    total_bytes          bigint not null default 0,
    error_summary        varchar(2000),
    idempotency_key      varchar(256),
    background_task_id   uuid,
    created_at           timestamptz not null default current_timestamp,
    updated_at           timestamptz not null default current_timestamp,
    version              bigint not null default 0,
    check (scope in ('ATTACHMENT', 'EPISODE', 'SEASON', 'RESOURCE_SET')),
    check (status in ('REQUESTED', 'IN_PROGRESS', 'COMPLETED', 'PARTIAL_FAILURE', 'FAILED', 'CANCELLED')),
    check (total_items >= 0 and completed_items >= 0 and completed_items <= total_items),
    check (total_bytes >= 0),
    unique (actor_id, scope, scope_id, idempotency_key)
);
create index idx_storage_restore_request_actor on storage_restore_request (actor_id, created_at desc);
create index idx_storage_restore_request_task on storage_restore_request (background_task_id);
