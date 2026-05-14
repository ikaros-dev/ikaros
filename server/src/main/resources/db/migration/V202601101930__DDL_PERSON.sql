create table if not exists "person"
(
    "id"            bigint primary key,
    "create_time"   timestamp(6),
    "create_uid"    bigint,
    "delete_status" boolean,
    "update_time"   timestamp(6),
    "update_uid"    bigint,
    "ol_version"    bigint,
    "name"          varchar(255),
    "infobox"       varchar(50000),
    "summary"       varchar(50000)
)
;