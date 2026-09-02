create extension if not exists pgcrypto;

create or replace function uuid_v7() returns uuid
    language sql volatile
as $$
    select (
        lpad(to_hex(floor(extract(epoch from clock_timestamp()) * 1000)::bigint), 12, '0')
        || '7'
        || substring(encode(gen_random_bytes(2), 'hex') from 1 for 3)
        || substring('89ab' from floor(random() * 4)::integer + 1 for 1)
        || substring(encode(gen_random_bytes(8), 'hex') from 1 for 15)
    )::uuid
$$;

create table resource
(
    id            uuid primary key default uuid_v7(),
    owner_id      uuid not null,
    resource_type varchar(64) not null,
    lifecycle     varchar(32) not null default 'ACTIVE',
    created_at    timestamptz not null default current_timestamp,
    updated_at    timestamptz not null default current_timestamp,
    deleted_at    timestamptz,
    version       bigint not null default 0
);

create index idx_resource_owner_lifecycle_updated
    on resource (owner_id, lifecycle, updated_at desc);

create table resource_title
(
    id          uuid primary key default uuid_v7(),
    resource_id uuid not null,
    locale      varchar(32) not null,
    title       varchar(512) not null,
    is_primary  boolean not null default false,
    created_at  timestamptz not null default current_timestamp,
    updated_at  timestamptz not null default current_timestamp,
    version     bigint not null default 0,
    unique (resource_id, locale)
);

create index idx_resource_title_search on resource_title using gin (to_tsvector('simple', title));

create table external_identity
(
    id            uuid primary key default uuid_v7(),
    resource_id   uuid not null,
    provider      varchar(128) not null,
    external_type varchar(128) not null,
    external_id   varchar(512) not null,
    created_at    timestamptz not null default current_timestamp,
    updated_at    timestamptz not null default current_timestamp,
    version       bigint not null default 0,
    unique (provider, external_type, external_id)
);

create table collection
(
    id          uuid primary key default uuid_v7(),
    owner_id    uuid not null,
    name        varchar(256) not null,
    description varchar(2000),
    created_at  timestamptz not null default current_timestamp,
    updated_at  timestamptz not null default current_timestamp,
    version     bigint not null default 0
);

create table collection_resource
(
    id            uuid primary key default uuid_v7(),
    collection_id uuid not null,
    resource_id   uuid not null,
    position      integer not null default 0,
    created_at    timestamptz not null default current_timestamp,
    version       bigint not null default 0,
    unique (collection_id, resource_id)
);

create table blob
(
    id           uuid primary key default uuid_v7(),
    sha256       varchar(64) not null unique,
    size_bytes   bigint not null,
    media_type   varchar(256),
    availability varchar(32) not null default 'AVAILABLE',
    created_at   timestamptz not null default current_timestamp,
    version      bigint not null default 0
);

create table attachment
(
    id              uuid primary key default uuid_v7(),
    resource_id     uuid not null,
    blob_id         uuid not null,
    file_name       varchar(512) not null,
    attachment_kind varchar(32) not null default 'ORIGINAL',
    created_at      timestamptz not null default current_timestamp,
    deleted_at      timestamptz,
    version         bigint not null default 0
);

create index idx_attachment_resource on attachment (resource_id) where deleted_at is null;

create table blob_placement
(
    id              uuid primary key default uuid_v7(),
    blob_id         uuid not null,
    provider        varchar(128) not null,
    storage_tier    varchar(32) not null,
    object_key      varchar(1024) not null,
    placement_state varchar(32) not null default 'ACTIVE',
    verified_at     timestamptz,
    created_at      timestamptz not null default current_timestamp,
    version         bigint not null default 0,
    unique (provider, object_key)
);

create index idx_blob_placement_blob on blob_placement (blob_id, placement_state);

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
