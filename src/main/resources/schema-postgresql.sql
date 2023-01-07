-- subject
create sequence if not exists subject_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists subject
(
    id            int8         not null default nextval('subject_seq'),
    parent_id     int8         not null,
    version       int8         null,
    subject_group varchar(255) not null,
    api_version   varchar(255) not null,
    kind          varchar(255) not null,
    plural        varchar(255) null,
    singular      varchar(255) null,
    name          varchar(255) null,
    url           varchar(255) null,
    constraint subject_gvk unique (subject_group, api_version, kind),
    constraint subject_pkey primary key (id)
);

create index if not exists subject_index_subject_group
    on subject
        using btree (subject_group);
create index if not exists subject_index_api_version
    on subject
        using btree (api_version);
create index if not exists subject_index_kind
    on subject
        using btree (kind);
create index if not exists subject_index_name
    on subject
        using btree (name);


-- metadata
create sequence if not exists metadata_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists metadata
(
    id         int8         not null default nextval('metadata_seq'),
    version    int8         null,
    subject_id int8         not null,
    meta_key   varchar(255) not null,
    meta_value varchar(255) null,
    constraint metadata_pkey primary key (id)
);

create index if not exists metadata_index_subject_id
    on metadata
        using btree (subject_id);
create index if not exists metadata_index_meta_key
    on metadata
        using btree (meta_key);


-- specification
create sequence if not exists specification_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists specification
(
    id         int8         not null default nextval('specification_seq'),
    version    int8         null,
    subject_id int8         not null,
    spec_key   varchar(255) not null,
    spec_value varchar(255) null,
    constraint specification_pkey primary key (id)
);

create index if not exists specification_index_subject_id
    on specification
        using btree (subject_id);
create index if not exists specification_index_spec_key
    on specification
        using btree (spec_key);


-- status
create sequence if not exists status_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists status
(
    id           int8         not null default nextval('status_seq'),
    version      int8         null,
    subject_id   int8         not null,
    status_key   varchar(255) not null,
    status_value varchar(255) null,
    constraint status_pkey primary key (id)
);

create index if not exists status_index_subject_id
    on status
        using btree (subject_id);
create index if not exists status_index_status_key
    on status
        using btree (status_key);