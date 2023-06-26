-- authority
create table if not exists authority
(
    id            int8         not null auto_increment,
    create_time   timestamp(6) null,
    create_uid    int8         null,
    delete_status bool         null,
    update_time   timestamp(6) null,
    update_uid    int8         null,
    ol_version    int8         null,
    role_id       int8         not null,
    method        varchar(255) not null,
    allow         bool         not null,
    url           varchar(255) not null,
    constraint authority_pkey primary key (id)
);

-- character
create table if not exists character
(
    id            int8           not null auto_increment,
    create_time   timestamp(6)   null,
    create_uid    int8           null,
    delete_status bool           null,
    update_time   timestamp(6)   null,
    update_uid    int8           null,
    ol_version    int8           null,
    name          varchar(255)   not null,
    infobox       varchar(50000) null,
    summary       varchar(50000) null,
    constraint character_pkey primary key (id)
);

-- collection
create table if not exists collection
(
    id            int8         not null auto_increment,
    create_time   timestamp(6) null,
    create_uid    int8         null,
    delete_status bool         null,
    update_time   timestamp(6) null,
    update_uid    int8         null,
    ol_version    int8         null,
    user_id       int8         not null,
    subject_id    int8         not null,
    status        int8         not null,
    constraint collection_pkey primary key (id)
);

-- episode
create table if not exists episode
(
    id            int8           not null auto_increment,
    create_time   timestamp(6)   null,
    create_uid    int8           null,
    delete_status bool           null,
    update_time   timestamp(6)   null,
    update_uid    int8           null,
    ol_version    int8           null,
    subject_id    int8           not null,
    name          varchar(255)   not null,
    name_cn       varchar(255)   null,
    description   varchar(50000) null,
    air_time      timestamp(6)   null,
    sequence      double         null,
    constraint episode_pkey primary key (id)
);

-- episode_file
create table if not exists episode_file
(
    id            int8         not null auto_increment,
    create_time   timestamp(6) null,
    create_uid    int8         null,
    delete_status bool         null,
    update_time   timestamp(6) null,
    update_uid    int8         null,
    ol_version    int8         null,
    episode_id    int8         not null,
    file_id       int8         not null,
    constraint episode_file_pkey primary key (id)
);

-- file
create table if not exists file
(
    id            int8          not null auto_increment,
    create_time   timestamp(6)  null,
    create_uid    int8          null,
    delete_status bool          null,
    update_time   timestamp(6)  null,
    update_uid    int8          null,
    ol_version    int8          null,
    folder_id     int8          null,
    md5           varchar(255)  null,
    aes_key       varchar(255)  null,
    name          varchar(1000) not null,
    original_name varchar(1000) null,
    original_path varchar(3000) null,
    size          int8          null,
    type          varchar(255)  null,
    url           varchar(3000) not null,
    can_read      bool          null,
    constraint file_pkey primary key (id)
);

-- file_remote
create table if not exists file_remote
(
    id            int8         not null auto_increment,
    create_time   timestamp(6) null,
    create_uid    int8         null,
    delete_status bool         null,
    update_time   timestamp(6) null,
    update_uid    int8         null,
    ol_version    int8         null,
    file_id       int8         not null,
    remote_id     varchar(100) not null,
    remote        varchar(100) not null,
    md5           varchar(300) null,
    file_name     varchar(300) null,
    path          varchar(600) null,
    file_size     int8         null,
    constraint file_remote_pkey primary key (id)
);

-- folder
create table if not exists folder
(
    id            int8         not null auto_increment,
    create_time   timestamp(6) null,
    create_uid    int8         null,
    delete_status bool         null,
    update_time   timestamp(6) null,
    update_uid    int8         null,
    ol_version    int8         null,
    parent_id     int8         not null,
    name          varchar(255) not null,
    constraint folder_pkey primary key (id)
);

-- person_character
create table if not exists person_character
(
    id            int8         not null auto_increment,
    create_time   timestamp(6) null,
    create_uid    int8         null,
    delete_status bool         null,
    update_time   timestamp(6) null,
    update_uid    int8         null,
    ol_version    int8         null,
    person_id     int8         not null,
    character_id  int8         not null,
    constraint person_character_pkey primary key (id)
);

-- person
create table if not exists person
(
    id            int8           not null auto_increment,
    create_time   timestamp(6)   null,
    create_uid    int8           null,
    delete_status bool           null,
    update_time   timestamp(6)   null,
    update_uid    int8           null,
    ol_version    int8           null,
    name          varchar(255)   not null,
    infobox       varchar(50000) null,
    summary       varchar(50000) null,
    constraint person_pkey primary key (id)
);


-- role
create table if not exists role
(
    id            int8         not null auto_increment,
    create_time   timestamp(6) null,
    create_uid    int8         null,
    delete_status bool         null,
    update_time   timestamp(6) null,
    update_uid    int8         null,
    ol_version    int8         null,
    name          varchar(255) not null,
    constraint role_pkey primary key (id)
);

-- subject_character
create table if not exists subject_character
(
    id            int8         not null auto_increment,
    create_time   timestamp(6) null,
    create_uid    int8         null,
    delete_status bool         null,
    update_time   timestamp(6) null,
    update_uid    int8         null,
    ol_version    int8         null,
    subject_id    int8         not null,
    character_id  int8         not null,
    constraint subject_character_pkey primary key (id)
);

-- subject
create table if not exists subject
(
    id            int8           not null auto_increment,
    create_time   timestamp(6)   null,
    create_uid    int8           null,
    delete_status bool           null,
    update_time   timestamp(6)   null,
    update_uid    int8           null,
    ol_version    int8           null,
    type          varchar(255)   not null,
    name          varchar(255)   not null,
    name_cn       varchar(255)   null,
    cover         varchar(255)   null,
    infobox       varchar(50000) null,
    summary       varchar(50000) null,
    nsfw          bool           not null,
    air_time      timestamp(6)   null,
    constraint subject_pkey primary key (id)
);

-- subject_person
create table if not exists subject_person
(
    id            int8         not null auto_increment,
    create_time   timestamp(6) null,
    create_uid    int8         null,
    delete_status bool         null,
    update_time   timestamp(6) null,
    update_uid    int8         null,
    ol_version    int8         null,
    subject_id    int8         not null,
    person_id     int8         not null,
    constraint subject_person_pkey primary key (id)
);

-- subject_relation
create table if not exists subject_relation
(
    id                  int8         not null auto_increment,
    create_time         timestamp(6) null,
    create_uid          int8         null,
    delete_status       bool         null,
    update_time         timestamp(6) null,
    update_uid          int8         null,
    ol_version          int8         null,
    subject_id          int8         not null,
    relation_type       varchar(255) not null,
    relation_subject_id int8         not null,
    constraint subject_relation_pkey primary key (id)
);

-- subject_sync
create table if not exists subject_sync
(
    id            int8         not null auto_increment,
    create_time   timestamp(6) null,
    create_uid    int8         null,
    delete_status bool         null,
    update_time   timestamp(6) null,
    update_uid    int8         null,
    ol_version    int8         null,
    subject_id    int8         not null,
    platform      varchar(255) not null,
    platform_id   varchar(255) not null,
    sync_time     timestamp(6) null,
    constraint subject_sync_pkey primary key (id)
);

-- ikuser
create table if not exists ikuser
(
    id              int8                not null auto_increment,
    create_time     timestamp(6)        null,
    create_uid      int8                null,
    delete_status   bool                null,
    update_time     timestamp(6)        null,
    update_uid      int8                null,
    ol_version      int8                null,
    role_id         int8                null,
    avatar          varchar(255)        null,
    email           varchar(255)        null,
    enable          bool                null,
    introduce       varchar(50000)      null,
    last_login_ip   varchar(255)        null,
    last_login_time timestamp(6)        null,
    nickname        varchar(255)        null,
    non_locked      bool                null,
    password        varchar(255)        null,
    site            varchar(255)        null,
    telephone       varchar(255)        null,
    username        varchar(255) unique null,
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
