create sequence if not exists episode_collection_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists episode_collection
(
    id          int8         not null default nextval('episode_collection_seq'),
    user_id     int8         not null,
    subject_id  int8         not null,
    episode_id  int8         not null,
    finish      bool         not null,
    progress    int8         null,
    duration    int8         null,
    update_time timestamp(6) null,
    constraint user_episode_id_uk unique (user_id, episode_id),
    constraint episode_collection_pkey primary key (id)
);