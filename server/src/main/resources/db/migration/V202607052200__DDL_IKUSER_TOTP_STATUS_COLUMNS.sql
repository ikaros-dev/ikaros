alter table if exists "ikuser_totp"
    add column if not exists "delete_status" boolean default false,
    add column if not exists "create_uid" uuid,
    add column if not exists "update_uid" uuid,
    add column if not exists "ol_version" bigint default 0;
