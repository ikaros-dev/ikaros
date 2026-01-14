create table if not exists "attachment"
(
    "id"          uuid NOT NULL,
    "parent_id"   uuid,
    "type"        varchar(255),
    "url"         varchar(5000),
    "path"        varchar(5000),
    "fs_path"     varchar(5000),
    "name"        varchar(255),
    "size"        bigint,
    "update_time" timestamp(6),
    "deleted"     boolean,
    "driver_id"   uuid,
    "sha1"        varchar(255),
    CONSTRAINT "attachment_pkey" PRIMARY KEY ("id")
)
;