-- box
create sequence if not exists box_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists box
(
    id int8 NOT NULL default nextval('box_seq'),
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    "version" int8 NULL,
    name varchar(255) NOT NULL,
    parent_id int8 NOT NULL,
    CONSTRAINT box_pkey PRIMARY KEY (id)
);

-- file
create sequence if not exists file_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists file
(
    id int8 NOT NULL default nextval('file_seq'),
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    "version" int8 NULL,
    folder_id int8 NOT NULL,
    md5 varchar(255) NULL,
    name varchar(255) NOT NULL,
    original_name varchar(255) NULL,
    original_path varchar(255) NULL,
    place varchar(255) NULL,
    "size" int8 NULL,
    "type" varchar(255) NULL,
    url varchar(255) NOT NULL,
    CONSTRAINT file_pkey PRIMARY KEY (id)
);

-- folder
create sequence if not exists folder_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists folder
(
    id int8 NOT NULL default nextval('folder_seq'),
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    "version" int8 NULL,
    name varchar(255) NOT NULL,
    parent_id int8 NOT NULL,
    CONSTRAINT folder_pkey PRIMARY KEY (id)
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
    id int8 NOT NULL default nextval('ikuser_seq'),
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    "version" int8 NULL,
    avatar varchar(255) NULL,
    email varchar(255) NULL,
    "enable" bool NULL,
    introduce varchar(50000) NULL,
    last_login_ip varchar(255) NULL,
    last_login_time int8 NULL,
    nickname varchar(255) NULL,
    non_locked bool NULL,
    "password" varchar(255) NULL,
    site varchar(255) NULL,
    telephone varchar(255) NULL,
    username varchar(255) NULL,
    CONSTRAINT ikuser_pkey PRIMARY KEY (id)
);

-- metadata
create sequence if not exists metadata_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists metadata
(
    id int8 NOT NULL default nextval('metadata_seq'),
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    "version" int8 NULL,
    host_id int8 NOT NULL,
    meta_key varchar(255) NOT NULL,
    "type" varchar(255) NULL,
    meta_value varchar(50000) NULL,
    CONSTRAINT metadata_pkey PRIMARY KEY (id)
);

-- option
create sequence if not exists option_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists option
(
    id int8 NOT NULL default nextval('option_seq'),
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    "version" int8 NULL,
    name varchar(255) NOT NULL,
    CONSTRAINT option_pkey PRIMARY KEY (id)
);

-- subject
create sequence if not exists subject_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists subject
(
    id int8 NOT NULL default nextval('subject_seq'),
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    "version" int8 NULL,
    box_id int8 NULL,
    fid int8 NULL,
    "type" varchar(255) NULL,
    CONSTRAINT subject_pkey PRIMARY KEY (id)
);