create table if not exists "custom_metadata"
(
    "id"        uuid primary key default uuidv7(),
    "custom_id" uuid,
    "cm_key"    varchar(255),
    "cm_value"  bytea,
    CONSTRAINT "custom_metadata_custom_id_em_key" unique (custom_id, cm_key)
)
;