create table if not exists "role"
(
    "id"            bigint primary key,
    "create_time"   timestamp(6),
    "create_uid"    bigint,
    "delete_status" boolean,
    "update_time"   timestamp(6),
    "update_uid"    bigint,
    "ol_version"    bigint,
    "parent_id"     bigint,
    "name"          varchar(255),
    "description"   varchar(50000)
)
;