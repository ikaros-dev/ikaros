create table if not exists "ikuser"
(
    "id"            uuid NOT NULL,
    "create_time"   timestamp(6),
    "create_uid"    uuid,
    "delete_status" boolean,
    "update_time"   timestamp(6),
    "update_uid"    uuid,
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
    CONSTRAINT "ikuser_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "ikuser_username_key" unique (username)
)
;