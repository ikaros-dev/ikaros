create table if not exists "episode_list_episode"
(
    "id"              uuid primary key DEFAULT uuidv7(),
    "episode_list_id" uuid,
    "episode_id"      uuid
)
;
