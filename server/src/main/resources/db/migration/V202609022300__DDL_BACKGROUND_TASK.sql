create table background_task
(
    id                   uuid primary key default uuid_v7(),
    task_type            varchar(256) not null,
    status               varchar(32) not null default 'PENDING',
    actor_type           varchar(64),
    actor_id             uuid,
    subject_type         varchar(128),
    subject_id           uuid,
    idempotency_key      varchar(512),
    correlation_id       uuid,
    payload              jsonb not null default '{}'::jsonb,
    result_summary       jsonb,
    available_at         timestamptz not null default current_timestamp,
    timeout_at           timestamptz,
    cancel_requested_at  timestamptz,
    version              bigint not null default 0,
    created_at           timestamptz not null default current_timestamp,
    updated_at           timestamptz not null default current_timestamp,
    completed_at         timestamptz,
    constraint background_task_status_ck
        check (status in ('PENDING','RUNNING','SUCCEEDED','FAILED','CANCELLED','TIMED_OUT')),
    constraint background_task_version_ck check (version >= 0)
);

create unique index background_task_idempotency_uq
    on background_task (task_type, idempotency_key)
    where idempotency_key is not null;
create index background_task_claim_idx
    on background_task (status, available_at, created_at, id)
    where status = 'PENDING';

create table background_task_attempt
(
    id                  uuid primary key default uuid_v7(),
    task_id             uuid not null,
    attempt_no          integer not null,
    status              varchar(32) not null,
    claimed_by          varchar(256),
    claimed_at          timestamptz,
    lease_expires_at    timestamptz,
    last_heartbeat_at   timestamptz,
    started_at          timestamptz,
    ended_at            timestamptz,
    error_classification varchar(128),
    error_summary       text,
    retryable           boolean,
    result_summary      jsonb,
    trace_id            varchar(256),
    created_at          timestamptz not null default current_timestamp,
    constraint background_task_attempt_uq unique (task_id, attempt_no),
    constraint background_task_attempt_no_ck check (attempt_no >= 1),
    constraint background_task_attempt_fk foreign key (task_id)
        references background_task (id) on delete restrict
);

create index background_task_attempt_task_idx on background_task_attempt (task_id, attempt_no);
