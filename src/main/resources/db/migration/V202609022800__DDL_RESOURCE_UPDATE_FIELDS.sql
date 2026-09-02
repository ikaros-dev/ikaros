alter table resource add column primary_title varchar(512);
alter table resource add column summary varchar(4000);
alter table resource add column data_classification varchar(32) not null default 'PRIVATE';

alter table resource add constraint resource_data_classification_ck
    check (data_classification in ('PUBLIC', 'SHARED', 'PRIVATE', 'SENSITIVE', 'SECURE'));
