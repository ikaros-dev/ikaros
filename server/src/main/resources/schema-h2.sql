-- attachment
create table if not exists attachment
(
    id          int8          not null auto_increment,
    parent_id   int8          null,
    type        varchar(255)  not null,
    url         varchar(5000) null,
    fs_path     varchar(5000) null,
    name        varchar(255)  not null,
    size        int8          null,
    update_time timestamp(6)  null,
    constraint type_parent_name_uk unique (type, parent_id, name),
    constraint attachment_pkey primary key (id)
);
-- Insert root directory
INSERT INTO attachment (id, parent_id, type, name, update_time)
SELECT 0, -1, 'Directory', '/', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1
                  FROM attachment
                  WHERE id = 0);
-- Insert cover directory
INSERT INTO attachment (id, parent_id, type, name, update_time)
SELECT 1, 0, 'Directory', 'Cover', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1
                  FROM attachment
                  WHERE id = 1);

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

-- subject_collection
create table if not exists subject_collection
(
    id               int8         not null auto_increment,
    user_id          int8         not null,
    subject_id       int8         not null,
    type             varchar(255) not null,
    main_ep_progress int8         not null,
    is_private       bool         not null,
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

-- episode_file
create table if not exists episode_file
(
    id         int8 not null auto_increment,
    episode_id int8 not null,
    file_id    int8 not null,
    constraint episode_file_pkey primary key (id)
);

-- file
create table if not exists file
(
    id          int8          not null auto_increment,
    folder_id   int8          null,
    md5         varchar(255)  null,
    aes_key     varchar(255)  null,
    name        varchar(1000) not null,
    fs_path     varchar(3000) null,
    size        int8          null,
    type        varchar(255)  null,
    url         varchar(3000) not null,
    can_read    bool          null,
    update_time timestamp(6)  null,
    constraint folder_name_uk unique (folder_id, name),
    constraint file_pkey primary key (id)
);

-- file_relation
create table if not exists file_relation
(
    id               int8         not null auto_increment,
    file_id          int8         not null,
    relation_type    varchar(255) not null,
    relation_file_id int8         not null,
    constraint file_relation_pkey primary key (id)
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
    id          int8         not null auto_increment,
    parent_id   int8         not null,
    name        varchar(255) not null,
    update_time timestamp(6) null,
    constraint name_parent_uk unique (name, parent_id),
    constraint folder_pkey primary key (id)
);

INSERT INTO folder (id, parent_id, name, update_time)
SELECT 0, -1, 'root', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1
                  FROM folder
                  WHERE name = 'root'
                    AND parent_id = -1);


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

-- video_subtitle
create table if not exists `video_subtitle`
(
    id               int8 not null auto_increment,
    video_file_id    int8 not null,
    subtitle_file_id int8 not null,
    constraint video_subtitle_file_id_uk unique (video_file_id, subtitle_file_id),
    constraint video_subtitle_pkey primary key (id)
);
