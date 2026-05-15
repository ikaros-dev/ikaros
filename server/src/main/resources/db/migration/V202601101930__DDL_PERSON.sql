create table if not exists "person"
(
    "id"            uuid primary key DEFAULT uuid_generate_v7(),
    "create_time"   timestamp(6),
    "create_uid"    uuid,
    "delete_status" boolean,
    "update_time"   timestamp(6),
    "update_uid"    uuid,
    "ol_version"    bigint,
    "name"          varchar(255),
    "infobox"       varchar(50000),
    "summary"       varchar(50000)
)
;