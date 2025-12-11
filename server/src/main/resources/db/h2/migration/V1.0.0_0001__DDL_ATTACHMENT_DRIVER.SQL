create table if not exists attachment_driver
(
    id              int8           not null auto_increment,
    enable          bool           null,
    d_type          varchar(255)   not null,
    d_name          varchar(255)   null,
    mount_name      varchar(255)   null,
    remote_path     varchar(255)   null,
    d_order         int8           null,
    d_comment       varchar(255)   null,
    refresh_token   varchar(255)   null,
    access_token    varchar(255)   null,
    expire_time     timestamp(6)   null,
    list_page_size  int8           null,
    root_dir_id     varchar(255)   null,
    request_limit   int8           null,
    user_id         int8           null,
    user_name       varchar(255)   null,
    avatar          varchar(255)   null,
    space_total     int8           null,
    space_use       int8           null,
    constraint attachment_driver_pkey primary key (id)
);