create table if not exists "episode_collection"
(
    "id"          uuid primary key DEFAULT uuid_generate_v7(),
    "user_id"     uuid,
    "subject_id"  uuid,
    "episode_id"  uuid,
    "finish"      boolean,
    "progress"    bigint,
    "duration"    bigint,
    "update_time" timestamp(6)
)
;