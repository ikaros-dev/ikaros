create table if not exists storage_upload_session
(
    id              uuid primary key default uuid_v7(),
    owner_id        uuid not null,
    resource_id     uuid not null,
    provider        varchar(128) not null,
    object_key      varchar(1024) not null,
    expected_size   bigint not null,
    declared_sha256 varchar(64) not null,
    state           varchar(32) not null,
    expires_at      timestamptz not null,
    created_at      timestamptz not null default current_timestamp,
    updated_at      timestamptz not null default current_timestamp,
    version         bigint not null default 0,
    idempotency_key varchar(128),
    constraint storage_upload_session_size_ck check (expected_size >= 0),
    constraint storage_upload_session_sha256_ck check (declared_sha256 ~ '^[A-Fa-f0-9]{64}$'),
    constraint storage_upload_session_state_ck check (state in
        ('OPEN', 'RECEIVING', 'FINALIZING', 'COMPLETED', 'ABORTED', 'EXPIRED'))
);

create unique index if not exists storage_upload_session_idempotency_uq
    on storage_upload_session (owner_id, resource_id, idempotency_key)
    where idempotency_key is not null;

create index if not exists storage_upload_session_expiry_idx
    on storage_upload_session (state, expires_at);
