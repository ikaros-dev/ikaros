create table if not exists metadata
(
    name    varchar(255) not null,
    data    longblob,
    version bigint,
    primary key (name)
);
