-- attachment
create table if not exists attachment
(
    id          int8          not null auto_increment,
    parent_id   int8          null,
    type        varchar(255)  not null,
    url         varchar(5000) null,
    path        varchar(5000) not null,
    fs_path     varchar(5000) null,
    name        varchar(255)  not null,
    size        int8          null,
    update_time timestamp(6)  null,
    deleted     bool          null,
    constraint type_parent_name_uk unique (type, parent_id, name),
    constraint attachment_pkey primary key (id)
);
-- Insert Root directory
INSERT INTO attachment (id, parent_id, type, path, name, update_time)
SELECT 0, -1, 'Directory', '/', '/', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1
                  FROM attachment
                  WHERE id = 0);
-- Insert Covers directory
INSERT INTO attachment (id, parent_id, type, path, name, update_time)
SELECT 1, 0, 'Directory', '/Covers', 'Covers', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1
                  FROM attachment
                  WHERE id = 1);
-- Insert Downloads directory
INSERT INTO attachment (id, parent_id, type, path, name, update_time)
SELECT 2, 0, 'Directory', '/Downloads', 'Downloads', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1
                  FROM attachment
                  WHERE id = 2);

-- attachment_relation
create table if not exists attachment_relation
(
    id                     int8         not null auto_increment,
    attachment_id          int8         not null,
    type                   varchar(255) not null,
    relation_attachment_id int8         not null,
    constraint attachment_relation_pkey primary key (id)
);

-- attachment_reference
create table if not exists attachment_reference
(
    id            int8         not null auto_increment,
    type          varchar(255) not null,
    attachment_id int8         not null,
    reference_id  int8         not null,
    constraint type_attachment_reference_uk unique (type, attachment_id, reference_id),
    constraint attachment_reference_pkey primary key (id)
);


-- authority
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

-- subject_collection
create table if not exists subject_collection
(
    id               int8         not null auto_increment,
    user_id          int8         not null,
    subject_id       int8         not null,
    type             varchar(255) not null,
    main_ep_progress int8         not null,
    is_private       bool         not null,
    comment          varchar(5000) null,
    score            int8         null,
    constraint user_subject_id_uk unique (user_id, subject_id),
    constraint subject_collection_pkey primary key (id)
);

-- episode_collection
create table if not exists episode_collection
(
    id          int8         not null auto_increment,
    user_id     int8         not null,
    subject_id  int8         not null,
    episode_id  int8         not null,
    finish      bool         not null,
    progress    int8         null,
    duration    int8         null,
    update_time timestamp(6) null,
    constraint user_episode_id_uk unique (user_id, episode_id),
    constraint episode_collection_pkey primary key (id)
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
    sequence      int8           null,
    ep_group      varchar(50)    not null,
    constraint subject_group_seq_uk unique (subject_id, ep_group, sequence),
    constraint episode_pkey primary key (id)
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
    cover         varchar(10000) null,
    infobox       varchar(50000) null,
    summary       varchar(50000) null,
    nsfw          bool           not null,
    air_time      timestamp(6)   null,
    score           DOUBLE          null,
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
    constraint platform_pid_ukey unique (platform, platform_id),
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
    avatar          varchar(255)        null,
    email           varchar(255)        null,
    enable          bool                null,
    introduce       varchar(50000)      null,
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

-- task
create table if not exists `task`
(
    id           int8          not null auto_increment,
    name         varchar(255)  not null,
    status       varchar(255)  not null,
    create_time  timestamp(6)  null,
    start_time   timestamp(6)  null,
    end_time     timestamp(6)  null,
    total        int8          null,
    index        int8          null,
    fail_message varchar(2000) null,
    constraint task_pkey primary key (id)
);


-- tag
create table if not exists tag
(
    id          int8         not null auto_increment,
    type        varchar(255) not null,
    master_id   int8         not null,
    name        varchar(255) not null,
    user_id     int8         not null,
    create_time timestamp(6) null,
    color varchar(200) null,
    constraint tag_pkey primary key (id)
);

-- ikuser_role
create table if not exists ikuser_role
(
    id      int8 not null auto_increment,
    user_id int8 not null,
    role_id int8 not null,
    constraint ikuser_role_pkey primary key (id)
);

-- role_authority
create table if not exists role_authority
(
    id           int8 not null auto_increment,
    role_id      int8 not null,
    authority_id int8 not null,
    constraint role_authority_pkey primary key (id)
);

-- attachment_driver
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