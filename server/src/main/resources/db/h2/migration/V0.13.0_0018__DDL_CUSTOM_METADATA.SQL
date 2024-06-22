create table if not exists `custom_metadata`
(
    id        int8         not null auto_increment,
    custom_id int8         not null,
    cm_key    varchar(255) not null,
    cm_value  blob,
    constraint custom_metadata_e_id_em_key unique (custom_id, cm_key),
    constraint custom_metadata_pkey primary key (id)
);