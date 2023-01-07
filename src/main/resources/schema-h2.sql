-- box
create table if not exists box
(
    id int8 NOT NULL auto_increment,
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    version int8 NULL,
    name varchar(255) NOT NULL,
    parent_id int8 NOT NULL,
    CONSTRAINT box_pkey PRIMARY KEY (id)
);

-- file
create table if not exists file
(
    id int8 NOT NULL auto_increment,
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    version int8 NULL,
    md5 varchar(255) NULL,
    name varchar(255) NOT NULL,
    original_name varchar(255) NULL,
    original_path varchar(255) NULL,
    place varchar(255) NULL,
    size int8 NULL,
    type varchar(255) NULL,
    url varchar(255) NOT NULL,
    CONSTRAINT file_pkey PRIMARY KEY (id)
);

-- ikuser
create table if not exists ikuser
(
    id int8 NOT NULL auto_increment,
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    version int8 NULL,
    avatar varchar(255) NULL,
    email varchar(255) NULL,
    enable bool NULL,
    introduce varchar(50000) NULL,
    last_login_ip varchar(255) NULL,
    last_login_time int8 NULL,
    nickname varchar(255) NULL,
    non_locked bool NULL,
    password varchar(255) NULL,
    site varchar(255) NULL,
    telephone varchar(255) NULL,
    username varchar(255) NULL,
    CONSTRAINT ikuser_pkey PRIMARY KEY (id)
);

-- metadata
create table if not exists metadata
(
    id int8 NOT NULL auto_increment,
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    version int8 NULL,
    host_id int8 NOT NULL,
    meta_key varchar(255) NOT NULL,
    type varchar(255) NULL,
    meta_value varchar(50000) NULL,
    CONSTRAINT metadata_pkey PRIMARY KEY (id)
);

-- option
create table if not exists `option`
(
    id int8 NOT NULL auto_increment,
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    version int8 NULL,
    name varchar(255) NOT NULL,
    CONSTRAINT option_pkey PRIMARY KEY (id)
);

-- subject
create table if not exists subject
(
    id int8 NOT NULL auto_increment,
    create_time timestamp(6) NULL,
    create_uid int8 NULL,
    status bool NULL,
    update_time timestamp(6) NULL,
    update_uid int8 NULL,
    version int8 NULL,
    box_id int8 NULL,
    file_id int8 NULL,
    CONSTRAINT subject_pkey PRIMARY KEY (id)
);