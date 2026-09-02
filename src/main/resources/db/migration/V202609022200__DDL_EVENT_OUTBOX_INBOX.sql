create table event_outbox
(
    id              uuid primary key default uuid_v7(),
    event_type      varchar(256) not null,
    schema_version  integer not null,
    aggregate_type  varchar(128) not null,
    aggregate_id    uuid,
    payload_json    text not null,
    occurred_at     timestamptz not null default current_timestamp,
    attempt_count   integer not null default 0,
    last_attempt_at timestamptz,
    dispatched_at   timestamptz,
    constraint event_outbox_version_ck check (schema_version >= 1),
    constraint event_outbox_attempt_ck check (attempt_count >= 0)
);

create index event_outbox_pending_idx on event_outbox (occurred_at, id)
    where dispatched_at is null;

create table event_inbox
(
    id           uuid primary key default uuid_v7(),
    consumer_id  varchar(256) not null,
    event_id     uuid not null,
    processed_at timestamptz not null default current_timestamp,
    constraint event_inbox_consumer_event_uq unique (consumer_id, event_id)
);
