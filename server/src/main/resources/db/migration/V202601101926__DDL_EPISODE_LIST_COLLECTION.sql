create table if not exists "episode_list_collection"
(
    "id"              uuid primary key DEFAULT uuidv7(),
    "user_id"         uuid,
    "episode_list_id" uuid,
    "update_time"     timestamp(6)
)
;
