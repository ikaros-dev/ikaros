create table if not exists "authority"
(
    "id"            bigint primary key,
    "create_time"   timestamp(6),
    "create_uid"    bigint,
    "delete_status" boolean,
    "update_time"   timestamp(6),
    "update_uid"    bigint,
    "ol_version"    bigint,
    "allow"         boolean,
    "type"          varchar(100),
    "target"        varchar(255),
    "authority"     varchar(10000)
)
;