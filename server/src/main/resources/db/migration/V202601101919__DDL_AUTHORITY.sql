create table if not exists "authority"
(
    "id"            uuid NOT NULL,
    "create_time"   timestamp(6),
    "create_uid"    uuid,
    "delete_status" boolean,
    "update_time"   timestamp(6),
    "update_uid"    uuid,
    "ol_version"    bigint,
    "allow"         boolean,
    "type"          varchar(100),
    "target"        varchar(255),
    "authority"     varchar(10000),
    CONSTRAINT "authority_pkey" PRIMARY KEY ("id")
)
;