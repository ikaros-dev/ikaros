create table if not exists "character"
(
    "id"            uuid NOT NULL,
    "create_time"   timestamp(6),
    "create_uid"    uuid,
    "delete_status" boolean,
    "update_time"   timestamp(6),
    "update_uid"    uuid,
    "ol_version"    bigint,
    "name"          varchar(255),
    "infobox"       varchar(50000),
    "summary"       varchar(50000),
    CONSTRAINT "character_pkey" PRIMARY KEY ("id")
)
;