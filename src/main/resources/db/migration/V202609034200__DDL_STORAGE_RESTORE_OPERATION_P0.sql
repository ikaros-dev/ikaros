create table storage_restore_operation
(
    id                      uuid primary key default uuid_v7(),
    placement_id            uuid not null,
    provider_restore_class  varchar(64) not null,
    restore_generation      bigint not null default 0,
    status                  varchar(32) not null default 'REQUESTED',
    background_task_id      uuid,
    provider_operation_id   varchar(512),
    restore_expires_at      timestamptz,
    error_summary           varchar(2000),
    created_at              timestamptz not null default current_timestamp,
    updated_at              timestamptz not null default current_timestamp,
    version                 bigint not null default 0,
    check (status in ('REQUESTED', 'IN_PROGRESS', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    check (restore_generation >= 0),
    foreign key (placement_id) references blob_placement(id),
    unique (placement_id, provider_restore_class, restore_generation)
);
create index idx_storage_restore_operation_active on storage_restore_operation (placement_id, status);

create table storage_restore_request_item
(
    id              uuid primary key default uuid_v7(),
    request_id      uuid not null,
    placement_id    uuid not null,
    operation_id    uuid not null,
    status          varchar(32) not null default 'WAITING',
    error_summary   varchar(2000),
    created_at      timestamptz not null default current_timestamp,
    updated_at      timestamptz not null default current_timestamp,
    version         bigint not null default 0,
    check (status in ('WAITING', 'READY', 'READY_TEMPORARILY', 'FAILED', 'CANCELLED')),
    foreign key (request_id) references storage_restore_request(id) on delete cascade,
    foreign key (placement_id) references blob_placement(id),
    foreign key (operation_id) references storage_restore_operation(id),
    unique (request_id, placement_id)
);
create index idx_storage_restore_item_operation on storage_restore_request_item (operation_id, status);
