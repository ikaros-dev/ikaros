alter table resource_title add column title_kind varchar(16) not null default 'TITLE';

alter table resource_title add constraint resource_title_kind_ck
    check (title_kind in ('TITLE', 'ALIAS'));
