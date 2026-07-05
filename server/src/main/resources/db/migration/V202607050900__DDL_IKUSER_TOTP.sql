create table if not exists "ikuser_totp"
(
    "id"           uuid primary key default uuidv7(),
    "user_id"      uuid not null references "ikuser"("id"),
    "secret"       varchar(64) not null,
    "enabled"      boolean default false,
    "create_time"  timestamp(6),
    "update_time"  timestamp(6),
    constraint "ikuser_totp_user_id_key" unique (user_id)
);
comment on table "ikuser_totp" is '用户二步验证TOTP配置';
comment on column "ikuser_totp"."secret" is 'TOTP密钥(Base32)';
comment on column "ikuser_totp"."enabled" is '是否启用二步验证';
