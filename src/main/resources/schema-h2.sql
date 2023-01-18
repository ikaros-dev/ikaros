-- file
create table if not exists file
(
    id            int8         not null auto_increment,
    create_time   timestamp(6) null,
    create_uid    int8         null,
    delete_status bool         null,
    update_time   timestamp(6) null,
    update_uid    int8         null,
    version       int8         null,
    md5           varchar(255) null,
    name          varchar(255) not null,
    original_name varchar(255) null,
    original_path varchar(255) null,
    place         varchar(255) null,
    size          int8         null,
    type          varchar(255) null,
    url           varchar(255) not null,
    constraint file_pkey primary key (id)
);

-- ikuser
create table if not exists ikuser
(
    id              int8           not null auto_increment,
    create_time     timestamp(6)   null,
    create_uid      int8           null,
    delete_status   bool           null,
    update_time     timestamp(6)   null,
    update_uid      int8           null,
    version         int8           null,
    avatar          varchar(255)   null,
    email           varchar(255)   null,
    enable          bool           null,
    introduce       varchar(50000) null,
    last_login_ip   varchar(255)   null,
    last_login_time int8           null,
    nickname        varchar(255)   null,
    non_locked      bool           null,
    password        varchar(255)   null,
    site            varchar(255)   null,
    telephone       varchar(255)   null,
    username        varchar(255)   null,
    constraint ikuser_pkey primary key (id)
);


-- custom
create table if not exists `custom`
(
    id      int8         not null auto_increment,
    c_group varchar(255) not null,
    version varchar(255) not null,
    kind    varchar(255) not null,
    name    varchar(255) not null,
    constraint custom_gvkn unique (c_group, version, kind, name),
    constraint custom_pkey primary key (id)
);

-- custom_metadata
create table if not exists `custom_metadata`
(
    id        int8         not null auto_increment,
    custom_id int8         not null,
    cm_key    varchar(255) not null,
    cm_value  blob,
    constraint custom_metadata_e_id_em_key unique (custom_id, cm_key),
    constraint custom_metadata_pkey primary key (id)
);
