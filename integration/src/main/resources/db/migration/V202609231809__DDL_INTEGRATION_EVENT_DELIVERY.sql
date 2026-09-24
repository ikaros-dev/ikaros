create table event_delivery
(
    id                         uuid primary key default uuid_v7(),
    consumer_id                varchar(256) not null,
    event_id                   uuid not null,
    status                     varchar(16) not null default 'PENDING',
    attempt_count              integer not null default 0,
    next_attempt_at            timestamptz not null default current_timestamp,
    last_attempt_at            timestamptz,
    last_error_classification  varchar(128),
    created_at                 timestamptz not null default current_timestamp,
    updated_at                 timestamptz not null default current_timestamp,
    constraint event_delivery_consumer_event_uq unique (consumer_id, event_id),
    constraint event_delivery_event_fk foreign key (event_id)
        references event_outbox (id) on delete cascade,
    constraint event_delivery_status_ck
        check (status in ('PENDING', 'RETRY', 'DELIVERED', 'DEAD')),
    constraint event_delivery_attempt_ck check (attempt_count >= 0)
);

create index event_delivery_ready_idx
    on event_delivery (consumer_id, next_attempt_at, event_id)
    where status in ('PENDING', 'RETRY');
