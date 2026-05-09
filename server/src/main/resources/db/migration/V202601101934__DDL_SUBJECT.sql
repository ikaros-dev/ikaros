create table if not exists "subject"
(
    "id"            uuid NOT NULL,
    "create_time"   timestamp(6),
    "create_uid"    uuid,
    "delete_status" boolean,
    "update_time"   timestamp(6),
    "update_uid"    uuid,
    "ol_version"    bigint,
    "type"          varchar(255),
    "name"          varchar(255),
    "name_cn"       varchar(255),
    "cover"         varchar(10000),
    "infobox"       varchar(50000),
    "summary"       varchar(50000),
    "nsfw"          boolean,
    "air_time"      timestamp(6),
    "score"         double precision,
    CONSTRAINT "subject_pkey" PRIMARY KEY ("id")
)
;