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
