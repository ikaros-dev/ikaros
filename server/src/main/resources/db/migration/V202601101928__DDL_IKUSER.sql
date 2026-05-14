create table if not exists "ikuser"
(
    "id"            bigint primary key,
    "create_time"   timestamp(6),
    "create_uid"    bigint,
    "delete_status" boolean,
    "update_time"   timestamp(6),
    "update_uid"    bigint,
    "ol_version"    bigint,
    "avatar"        varchar(255),
    "email"         varchar(255),
    "enable"        boolean,
    "introduce"     varchar(50000),
    "nickname"      varchar(255),
    "non_locked"    boolean,
    "password"      varchar(255),
    "site"          varchar(255),
    "telephone"     varchar(255),
    "username"      varchar(255),
    CONSTRAINT "ikuser_username_key" unique (username)
)
;