create table if not exists "custom"
(
    "id"      uuid primary key default uuidv7(),
    "c_group" varchar(255),
    "version" varchar(255),
    "kind"    varchar(255),
    "name"    varchar(255),
    CONSTRAINT "custom_gvkn" unique (c_group, "version", kind, name)
)
;