create sequence if not exists custom_metadata_seq
    increment 1
    start 1
    minvalue 1
    cache 1
    no cycle;

create table if not exists custom_metadata
(
    id
    int8
    not
    null
    default
    nextval
(
    'custom_metadata_seq'
),
    custom_id int8 not null,
    cm_key varchar
(
    255
) not null,
    cm_value bytea,
    constraint custom_metadata_custom_id_em_key unique
(
    custom_id,
    cm_key
),
    constraint custom_metadata_pkey primary key
(
    id
)
    );