create table if not exists tag
(
    id              int8 not null auto_increment,
    episode_list_id int8 not null,
    episode_id      int8 not null,
    constraint tag_pkey primary key (id)
);