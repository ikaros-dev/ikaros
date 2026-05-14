create table if not exists "subject_collection"
(
    "id"               bigint primary key,
    "user_id"          bigint,
    "subject_id"       bigint,
    "type"             varchar(255),
    "main_ep_progress" bigint,
    "is_private"       boolean,
    "comment"          varchar(5000),
    "score"            bigint
)
;
