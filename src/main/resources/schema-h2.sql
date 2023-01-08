-- file
create table if not exists file
(
    id            int8         not null auto_increment,
    create_time   timestamp(6) null,
    create_uid    int8         null,
    status        bool         null,
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
    status          bool           null,
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


-- setting
create table if not exists `setting`
(
    id            int8         not null auto_increment,
    create_time   timestamp(6) null,
    create_uid    int8         null,
    status        bool         null,
    update_time   timestamp(6) null,
    update_uid    int8         null,
    version       int8         null,
    category      varchar(255) not null,
    setting_key   varchar(255) not null,
    setting_value varchar(255) null,
    constraint setting_pkey primary key (id)
);


-- extension
create table if not exists `extension`
(
    id          int8         not null auto_increment,
    `group`     varchar(255) not null,
    version     varchar(255) not null,
    kind        varchar(255) not null,
    name        varchar(255) not null,
    constraint extension_gvkn unique (`group`, version, kind, name),
    constraint extension_pkey primary key (id)
);

-- extension_metadata
create table if not exists `extension_metadata`
(
    id       int8         not null auto_increment,
    e_id     int8         not null,
    em_key   varchar(255) not null,
    em_value blob,
    constraint extension_metadata_e_id_em_key unique (e_id, em_key),
    constraint extension_metadata_pkey primary key (id)
);
