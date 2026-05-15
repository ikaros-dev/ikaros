create table if not exists "subject_relation"
(
    "id"                  uuid         NOT NULL,
    "create_time"         timestamp(6),
    "create_uid"          uuid,
    "delete_status"       boolean,
    "update_time"         timestamp(6),
    "update_uid"          uuid,
    "ol_version"          bigint,
    "subject_id"          uuid,
    "relation_type"       varchar(100) not null,
    "relation_subject_id" uuid,
    CONSTRAINT "subject_relation_pkey" PRIMARY KEY ("id")
)
;