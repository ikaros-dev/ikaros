create table if not exists role_authority
(
    id           int8 not null auto_increment,
    role_id      int8 not null,
    authority_id int8 not null,
    constraint role_authority_id_uk unique (role_id, authority_id),
    constraint role_authority_pkey primary key (id)
);