create sequence if not exists episode_list_collection_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists episode_list_collection
(
    id              int8         not null default nextval('episode_list_collection_seq'),
    user_id         int8         not null,
    episode_list_id int8         not null,
    update_time     timestamp(6) null,
    constraint episode_list_collection_pkey primary key (id)
);