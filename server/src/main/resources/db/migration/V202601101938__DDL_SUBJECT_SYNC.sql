create table if not exists "subject_sync"
(
    "id"            bigint primary key,
    "create_time"   timestamp(6),
    "create_uid"    bigint,
    "delete_status" boolean,
    "update_time"   timestamp(6),
    "update_uid"    bigint,
    "ol_version"    bigint,
    "subject_id"    bigint,
    "platform"      varchar(255),
    "platform_id"   varchar(255),
    "sync_time"     timestamp(6),
    CONSTRAINT "platform_pid_ukey" UNIQUE (platform, platform_id)
)
;