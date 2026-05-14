create table if not exists "episode_list"
(
    "id"            bigint primary key,
    "create_time"   timestamp(6),
    "create_uid"    bigint,
    "delete_status" boolean,
    "update_time"   timestamp(6),
    "update_uid"    bigint,
    "ol_version"    bigint,
    "name"          varchar(255),
    "name_cn"       varchar(255),
    "cover"         varchar(10000),
    "description"   varchar(50000),
    "nsfw"          boolean
)
;