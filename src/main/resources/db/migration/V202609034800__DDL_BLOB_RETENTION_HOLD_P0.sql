create table blob_retention_hold
(
    id           uuid primary key default uuid_v7(),
    blob_id      uuid not null,
    holder_type  varchar(128) not null,
    holder_id    varchar(256) not null,
    reason_code  varchar(128) not null,
    expires_at   timestamptz,
    created_by   uuid not null,
    created_at   timestamptz not null default current_timestamp,
    released_at  timestamptz,
    version      bigint not null default 0,
    foreign key (blob_id) references blob(id),
    foreign key (created_by) references platform_user(id),
    unique (blob_id, holder_type, holder_id, reason_code)
);
create index idx_blob_retention_hold_active on blob_retention_hold(blob_id, expires_at)
    where released_at is null;
