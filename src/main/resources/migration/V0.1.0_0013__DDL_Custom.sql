create table if not exists custom
(
    name    varchar(255) not null,
    data    oid NULL,
    version bigint,
    primary key (name)
);