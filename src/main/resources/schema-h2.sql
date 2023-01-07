-- file
create table if not exists file
(
    id            int8         NOT NULL auto_increment,
    create_time   timestamp(6) NULL,
    create_uid    int8         NULL,
    status        bool         NULL,
    update_time   timestamp(6) NULL,
    update_uid    int8         NULL,
    version       int8         NULL,
    md5           varchar(255) NULL,
    name          varchar(255) NOT NULL,
    original_name varchar(255) NULL,
    original_path varchar(255) NULL,
    place         varchar(255) NULL,
    size          int8         NULL,
    type          varchar(255) NULL,
    url           varchar(255) NOT NULL,
    CONSTRAINT file_pkey PRIMARY KEY (id)
);

-- ikuser
create table if not exists ikuser
(
    id              int8           NOT NULL auto_increment,
    create_time     timestamp(6)   NULL,
    create_uid      int8           NULL,
    status          bool           NULL,
    update_time     timestamp(6)   NULL,
    update_uid      int8           NULL,
    version         int8           NULL,
    avatar          varchar(255)   NULL,
    email           varchar(255)   NULL,
    enable          bool           NULL,
    introduce       varchar(50000) NULL,
    last_login_ip   varchar(255)   NULL,
    last_login_time int8           NULL,
    nickname        varchar(255)   NULL,
    non_locked      bool           NULL,
    password        varchar(255)   NULL,
    site            varchar(255)   NULL,
    telephone       varchar(255)   NULL,
    username        varchar(255)   NULL,
    CONSTRAINT ikuser_pkey PRIMARY KEY (id)
);


-- setting
create table if not exists `setting`
(
    id          int8         NOT NULL auto_increment,
    create_time timestamp(6) NULL,
    create_uid  int8         NULL,
    status      bool         NULL,
    update_time timestamp(6) NULL,
    update_uid  int8         NULL,
    version     int8         NULL,
    name        varchar(255) NOT NULL,
    CONSTRAINT setting_pkey PRIMARY KEY (id)
);


-- custom
create table if not exists `custom`
(
    id          int8         NOT NULL auto_increment,
    create_time timestamp(6) NULL,
    create_uid  int8         NULL,
    status      bool         NULL,
    update_time timestamp(6) NULL,
    update_uid  int8         NULL,
    version     int8         NULL,
    name        varchar(255) NOT NULL,
    data        blob,
    CONSTRAINT custom_pkey PRIMARY KEY (id)
);
