create table if not exists ikuser_role
(
    id      int8 not null auto_increment,
    user_id int8 not null,
    role_id int8 not null,
    constraint ikuser_role_pkey primary key (id)
);