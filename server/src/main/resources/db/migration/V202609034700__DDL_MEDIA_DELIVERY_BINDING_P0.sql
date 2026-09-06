create table media_delivery_binding
(
    id                       uuid primary key default uuid_v7(),
    storage_provider_id      uuid not null,
    delivery_provider_key    varchar(128) not null,
    priority                 integer not null default 100,
    enabled                  boolean not null default true,
    cache_key_policy         varchar(32) not null default 'CONTENT_IDENTITY',
    range_policy             varchar(32) not null default 'PASSTHROUGH',
    fallback_participation   boolean not null default true,
    created_at               timestamptz not null default current_timestamp,
    updated_at               timestamptz not null default current_timestamp,
    version                  bigint not null default 0,
    check (cache_key_policy in ('CONTENT_IDENTITY', 'FULL_REQUEST', 'NO_CACHE')),
    check (range_policy in ('PASSTHROUGH', 'FIXED_CHUNK', 'UNSUPPORTED')),
    foreign key (storage_provider_id) references storage_provider(id),
    unique (storage_provider_id, delivery_provider_key)
);
create index idx_media_delivery_binding_resolution on media_delivery_binding(storage_provider_id, enabled, priority);
