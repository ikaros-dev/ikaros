create table if not exists "subject_relation"
(
    "id"                  bigint primary key,
    "create_time"         timestamp(6),
    "create_uid"          bigint,
    "delete_status"       boolean,
    "update_time"         timestamp(6),
    "update_uid"          bigint,
    "ol_version"          bigint,
    "subject_id"          bigint,
    "relation_type"       varchar(100) not null,
    "relation_subject_id" bigint
)
;