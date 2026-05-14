create table if not exists "episode_list_collection"
(
    "id"              bigint primary key,
    "user_id"         bigint,
    "episode_list_id" bigint,
    "update_time"     timestamp(6)
)
;
