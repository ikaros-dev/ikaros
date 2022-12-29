create table if not exists custom
(
    name    varchar(255) not null,
    data    bytea,
    version bigint,
    primary key (name)
);