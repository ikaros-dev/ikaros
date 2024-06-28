create sequence if not exists attachment_seq
    increment 1
    start 10
    minvalue 10
    cache 1
    no cycle;

create table if not exists attachment
(
    id          int8          not null default nextval('attachment_seq'),
    parent_id   int8          null,
    type        varchar(255)  not null,
    url         varchar(5000) null,
    path        varchar(5000) not null,
    fs_path     varchar(5000) null,
    name        varchar(255)  not null,
    size        int8          null,
    update_time timestamp(6)  null,
    constraint type_parent_name_uk unique (type, parent_id, name),
    constraint attachment_pkey primary key (id)
);