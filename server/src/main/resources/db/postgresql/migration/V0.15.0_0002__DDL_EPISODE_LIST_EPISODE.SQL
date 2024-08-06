create sequence if not exists episode_list_episode_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists episode_list_episode
(
    id              int8 not null default nextval('episode_list_episode_seq'),
    episode_list_id int8 not null,
    episode_id      int8 not null,
    constraint episode_list_episode_pkey primary key (id)
);