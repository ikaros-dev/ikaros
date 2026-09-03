create table media_delivery_grant
(
    id                  uuid primary key default uuid_v7(),
    attachment_id       uuid not null,
    owner_id            uuid not null,
    token_hash          varchar(128) not null,
    method              varchar(16) not null default 'GET',
    range_start         bigint,
    range_end           bigint,
    expires_at          timestamptz not null,
    revocation_level    varchar(32) not null default 'IMMEDIATE',
    revoked_at          timestamptz,
    created_at          timestamptz not null default current_timestamp,
    version             bigint not null default 0,
    check (method = 'GET'),
    check (range_start is null or range_start >= 0),
    check (range_end is null or range_end >= 0),
    check (range_start is null or range_end is null or range_start <= range_end),
    check (revocation_level in ('IMMEDIATE', 'KEY_VERSION_BOUND', 'TTL_BOUNDED', 'NOT_REVOCABLE_BEFORE_EXPIRY')),
    foreign key (attachment_id) references attachment(id),
    foreign key (owner_id) references platform_user(id)
);
create unique index uq_media_delivery_grant_token_hash on media_delivery_grant(token_hash);
create index idx_media_delivery_grant_attachment on media_delivery_grant(attachment_id, expires_at);
