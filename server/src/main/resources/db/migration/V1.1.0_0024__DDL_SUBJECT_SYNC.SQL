create table if not exists "subject_sync"
(
    "id"            uuid NOT NULL,
    "create_time"   timestamp(6),
    "create_uid"    uuid,
    "delete_status" boolean,
    "update_time"   timestamp(6),
    "update_uid"    uuid,
    "ol_version"    bigint,
    "subject_id"    uuid,
    "platform"      varchar(255),
    "platform_id"   varchar(255),
    "sync_time"     timestamp(6),
    CONSTRAINT "subject_sync_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "platform_pid_ukey" UNIQUE (platform, platform_id)
)
;