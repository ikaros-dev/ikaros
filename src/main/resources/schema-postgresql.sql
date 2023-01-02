create table if not exists metadata
(
    name    varchar(255) not null,
    data    bytea,
    version bigint,
    primary key (name)
);
