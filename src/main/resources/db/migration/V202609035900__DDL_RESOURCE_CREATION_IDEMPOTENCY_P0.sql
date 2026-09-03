create table resource_creation_idempotency
(
    id                 uuid primary key default uuid_v7(),
    owner_id           uuid not null,
    idempotency_key    varchar(512) not null,
    request_fingerprint varchar(128) not null,
    resource_id        uuid not null,
    created_at         timestamptz not null default current_timestamp,
    foreign key (resource_id) references resource(id) on delete cascade,
    unique (owner_id, idempotency_key)
);

create index idx_resource_creation_idempotency_resource
    on resource_creation_idempotency (resource_id);
