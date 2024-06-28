create sequence if not exists role_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists role
(
    id            int8           not null default nextval('role_seq'),
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