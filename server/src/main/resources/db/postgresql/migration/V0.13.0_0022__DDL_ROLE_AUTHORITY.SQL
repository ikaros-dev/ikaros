create sequence if not exists role_authority_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists role_authority
(
    id           int8 not null default nextval('role_authority_seq'),
    role_id      int8 not null,
    authority_id int8 not null,
    constraint role_authority_id_uk unique (role_id, authority_id),
    constraint role_authority_pkey primary key (id)
);