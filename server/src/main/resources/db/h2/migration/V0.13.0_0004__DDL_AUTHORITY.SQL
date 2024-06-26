create table if not exists authority
(
    id            int8           not null auto_increment,
    create_time   timestamp(6)   null,
    create_uid    int8           null,
    delete_status bool           null,
    update_time   timestamp(6)   null,
    update_uid    int8           null,
    ol_version    int8           null,
    allow         bool           not null,
    type          varchar(100)   not null,
    target        varchar(255)   not null,
    authority     varchar(10000) not null,
    constraint authority_pkey primary key (id)
);