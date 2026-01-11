create table if not exists "episode"
(
    "id"            uuid NOT NULL,
    "create_time"   timestamp(6),
    "create_uid"    uuid,
    "delete_status" boolean,
    "update_time"   timestamp(6),
    "update_uid"    uuid,
    "ol_version"    bigint,
    "subject_id"    uuid,
    "name"          varchar(255),
    "name_cn"       varchar(255),
    "description"   varchar(50000),
    "air_time"      timestamp(6),
    "ep_group"      varchar(50),
    "sequence"      real,
    CONSTRAINT "episode_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "subject_group_seq_name_uk" unique (subject_id, ep_group, sequence, name)
)
;