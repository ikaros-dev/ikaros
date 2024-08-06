create table if not exists episode_list
(
    id            int8           not null auto_increment,
    create_time   timestamp(6)   null,
    create_uid    int8           null,
    delete_status bool           null,
    update_time   timestamp(6)   null,
    update_uid    int8           null,
    ol_version    int8           null,
    name          varchar(255)   not null,
    name_cn       varchar(255)   null,
    cover         varchar(10000) null,
    description   varchar(50000) null,
    nsfw          bool           not null,
    constraint episode_list_pkey primary key (id)
);