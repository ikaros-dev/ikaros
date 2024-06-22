create table if not exists `custom`
(
    id      int8         not null auto_increment,
    c_group varchar(255) not null,
    version varchar(255) not null,
    kind    varchar(255) not null,
    name    varchar(255) not null,
    constraint custom_gvkn unique (c_group, version, kind, name),
    constraint custom_pkey primary key (id)
);