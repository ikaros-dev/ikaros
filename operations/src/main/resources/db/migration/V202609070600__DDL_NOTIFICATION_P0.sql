create table notification
(
    id              uuid primary key default uuid_v7(),
    event_id        uuid not null unique,
    recipient_id    uuid not null,
    source          varchar(64) not null,
    event_type      varchar(256) not null,
    title           varchar(512) not null,
    body            text not null,
    priority        varchar(32) not null default 'NORMAL',
    status          varchar(32) not null default 'UNREAD',
    task_id         uuid,
    resource_id     uuid,
    created_at      timestamptz not null default current_timestamp,
    read_at         timestamptz,
    archived_at     timestamptz,
    version         bigint not null default 0,
    constraint notification_priority_ck check (priority in ('NORMAL', 'HIGH')),
    constraint notification_status_ck check (status in ('UNREAD', 'READ', 'ARCHIVED')),
    constraint notification_version_ck check (version >= 0)
);

create index notification_recipient_created_idx
    on notification (recipient_id, created_at desc, id desc);
create index notification_recipient_status_idx
    on notification (recipient_id, status, created_at desc, id desc);
