create table if not exists "subject_collection"
(
    "id"               uuid NOT NULL,
    "user_id"          uuid,
    "subject_id"       uuid,
    "type"             varchar(255),
    "main_ep_progress" bigint,
    "is_private"       boolean,
    "comment"          varchar(5000),
    "score"            bigint,
    CONSTRAINT "subject_collection_pkey" PRIMARY KEY ("id")
)
;
