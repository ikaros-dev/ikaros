create sequence if not exists ikuser_role_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists ikuser_role
(
    id      int8 not null default nextval('ikuser_role_seq'),
    user_id int8 not null,
    role_id int8 not null,
    constraint ikuser_role_pkey primary key (id)
);