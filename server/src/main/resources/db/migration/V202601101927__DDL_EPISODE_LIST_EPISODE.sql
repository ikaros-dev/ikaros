create table if not exists "episode_list_episode"
(
    "id"              bigint primary key,
    "episode_list_id" bigint,
    "episode_id"      bigint
)
;
