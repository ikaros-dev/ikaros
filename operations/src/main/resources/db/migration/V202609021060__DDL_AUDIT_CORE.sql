create table audit_event
(
    id          uuid primary key default uuid_v7(),
    actor_type  varchar(64) not null,
    actor_id    uuid,
    action      varchar(128) not null,
    target_type varchar(128) not null,
    target_id   uuid,
    details     text not null default '{}',
    occurred_at timestamptz not null default current_timestamp,
    version     bigint not null default 0
);

create index idx_audit_event_target on audit_event (target_type, target_id, occurred_at desc);
