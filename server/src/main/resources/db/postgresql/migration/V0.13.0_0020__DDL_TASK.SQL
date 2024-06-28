create sequence if not exists task_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists task
(
    id           int8          not null default nextval('task_seq'),
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