create sequence if not exists person_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists person
(
    id            int8           not null default nextval('person_seq'),
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