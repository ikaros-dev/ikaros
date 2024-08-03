create table if not exists episode_list_collection
(
    id              int8         not null auto_increment,
    user_id         int8         not null,
    episode_list_id int8         not null,
    update_time     timestamp(6) null,
    constraint episode_list_collection_pkey primary key (id)
);