create sequence if not exists custom_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists custom
(
    id        int8         not null default nextval('custom_seq'),
    c_group   varchar(255) not null,
    "version" varchar(255) not null,
    kind      varchar(255) not null,
    name      varchar(255) not null,
    constraint custom_gvkn unique (c_group, "version", kind, name),
    constraint custom_pkey primary key (id)
);