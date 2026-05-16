create table if not exists "subject_collection"
(
    "id"               uuid primary key DEFAULT uuidv7(),
    "user_id"          uuid,
    "subject_id"       uuid,
    "type"             varchar(255),
    "main_ep_progress" uuid,
    "is_private"       boolean,
    "comment"          varchar(5000),
    "score"            bigint
)
;
