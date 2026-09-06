create table media_delivery_lease
(
    id              uuid primary key default uuid_v7(),
    attachment_id   uuid not null,
    blob_id         uuid not null,
    owner_id        uuid not null,
    grant_id        uuid,
    lease_expires_at timestamptz not null,
    released_at     timestamptz,
    last_heartbeat_at timestamptz not null,
    created_at      timestamptz not null default current_timestamp,
    version         bigint not null default 0,
    foreign key (attachment_id) references attachment(id),
    foreign key (blob_id) references blob(id),
    foreign key (owner_id) references platform_user(id),
    foreign key (grant_id) references media_delivery_grant(id)
);
create index idx_media_delivery_lease_blob_active on media_delivery_lease(blob_id, lease_expires_at)
    where released_at is null;
create index idx_media_delivery_lease_owner on media_delivery_lease(owner_id, lease_expires_at);
