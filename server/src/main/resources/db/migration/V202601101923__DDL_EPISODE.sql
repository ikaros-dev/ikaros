create table if not exists "episode"
(
    "id"            bigint primary key,
    "create_time"   timestamp(6),
    "create_uid"    bigint,
    "delete_status" boolean,
    "update_time"   timestamp(6),
    "update_uid"    bigint,
    "ol_version"    bigint,
    "subject_id"    bigint,
    "name"          varchar(255),
    "name_cn"       varchar(255),
    "description"   varchar(50000),
    "air_time"      timestamp(6),
    "ep_group"      varchar(50),
    "sequence"      real,
    CONSTRAINT "subject_group_seq_name_uk" unique (subject_id, ep_group, sequence, name)
)
;