create table if not exists "role"
(
    "id"            uuid primary key default uuidv7(),
    "create_time"   timestamp(6),
    "create_uid"    uuid,
    "delete_status" boolean,
    "update_time"   timestamp(6),
    "update_uid"    uuid,
    "ol_version"    bigint,
    "parent_id"     uuid,
    "name"          varchar(255),
    "description"   varchar(50000)
)
;