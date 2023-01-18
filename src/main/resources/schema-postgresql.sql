-- file
create sequence if not exists file_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists file
(
    id            int8         not null default nextval('file_seq'),
    create_time   timestamp(6) null,
    create_uid    int8         null,
    delete_status bool         null,
    update_time   timestamp(6) null,
    update_uid    int8         null,
    "version"     int8         null,
    md5           varchar(255) null,
    name          varchar(255) not null,
    original_name varchar(255) null,
    original_path varchar(255) null,
    place         varchar(255) null,
    "size"        int8         null,
    "type"        varchar(255) null,
    url           varchar(255) not null,
    constraint file_pkey primary key (id)
);

-- ikuser
create sequence if not exists ikuser_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists ikuser
(
    id              int8           not null default nextval('ikuser_seq'),
    create_time     timestamp(6)   null,
    create_uid      int8           null,
    delete_status   bool           null,
    update_time     timestamp(6)   null,
    update_uid      int8           null,
    "version"       int8           null,
    avatar          varchar(255)   null,
    email           varchar(255)   null,
    "enable"        bool           null,
    introduce       varchar(50000) null,
    last_login_ip   varchar(255)   null,
    last_login_time int8           null,
    nickname        varchar(255)   null,
    non_locked      bool           null,
    "password"      varchar(255)   null,
    site            varchar(255)   null,
    telephone       varchar(255)   null,
    username        varchar(255)   null,
    constraint ikuser_pkey primary key (id)
);

-- custom
create sequence if not exists custom_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists custom
(
    id        int8         not null default nextval('custom_seq'),
    c_group   varchar(255) not null,
    "version" varchar(255) not null,
    kind      varchar(255) not null,
    name      varchar(255) not null,
    constraint custom_gvkn unique (c_group, "version", kind, name),
    constraint custom_pkey primary key (id)
);

-- custom_metadata
create sequence if not exists custom_metadata_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists custom_metadata
(
    id        int8         not null default nextval('custom_metadata_seq'),
    custom_id int8         not null,
    cm_key    varchar(255) not null,
    cm_value  bytea,
    constraint custom_metadata_custom_id_em_key unique (custom_id, cm_key),
    constraint custom_metadata_pkey primary key (id)
);

