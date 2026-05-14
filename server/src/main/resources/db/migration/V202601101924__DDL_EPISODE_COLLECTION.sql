create table if not exists "episode_collection"
(
    "id"          bigint primary key,
    "user_id"     bigint,
    "subject_id"  bigint,
    "episode_id"  bigint,
    "finish"      boolean,
    "progress"    bigint,
    "duration"    bigint,
    "update_time" timestamp(6)
)
;