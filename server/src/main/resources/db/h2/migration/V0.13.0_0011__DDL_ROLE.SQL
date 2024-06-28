create table if not exists role
(
    id            int8           not null auto_increment,
    create_time   timestamp(6)   null,
    create_uid    int8           null,
    delete_status bool           null,
    update_time   timestamp(6)   null,
    update_uid    int8           null,
    ol_version    int8           null,
    parent_id     int8           not null,
    name          varchar(255)   not null,
    description   varchar(50000) null,
    constraint role_pkey primary key (id)
);