-- subject
create table if not exists subject
(
    id            int8         not null auto_increment,
    parent_id     int8         not null,
    version       int8         null,
    subject_group varchar(255) not null,
    api_version   varchar(255) not null,
    kind          varchar(255) not null,
    plural        varchar(255) null,
    singular      varchar(255) null,
    name          varchar(255) null,
    url           varchar(255) null,
    constraint gvk unique (subject_group, api_version, kind),
    constraint subject_pkey primary key (id)
);

create index if not exists subject_index_subject_group
    on subject (subject_group);
create index if not exists subject_index_api_version
    on subject (api_version);
create index if not exists subject_index_kind
    on subject (kind);
create index if not exists subject_index_name
    on subject (name);


-- metadata
create table if not exists metadata
(
    id         int8         not null auto_increment,
    version    int8         null,
    subject_id int8         not null,
    meta_key   varchar(255) not null,
    meta_value varchar(255) null,
    constraint metadata_subject_key unique (subject_id, meta_key),
    constraint metadata_pkey primary key (id)
);

create index if not exists metadata_index_subject_id
    on metadata (subject_id);
create index if not exists metadata_index_meta_key
    on metadata (meta_key);


-- specification
create table if not exists specification
(
    id         int8         not null auto_increment,
    version    int8         null,
    subject_id int8         not null,
    spec_key   varchar(255) not null,
    spec_value varchar(255) null,
    constraint specification_subject_key unique (subject_id, spec_key),
    constraint specification_pkey primary key (id)
);

create index if not exists specification_index_subject_id
    on specification (subject_id);
create index if not exists specification_index_spec_key
    on specification (spec_key);


-- status
create table if not exists status
(
    id           int8         not null auto_increment,
    version      int8         null,
    subject_id   int8         not null,
    status_key   varchar(255) not null,
    status_value varchar(255) null,
    constraint status_subject_key unique (subject_id, status_key),
    constraint status_pkey primary key (id)
);

create index if not exists status_index_subject_id
    on status (subject_id);
create index if not exists status_index_status_key
    on status (status_key);
