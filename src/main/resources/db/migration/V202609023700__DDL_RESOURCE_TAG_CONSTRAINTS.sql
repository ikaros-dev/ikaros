alter table resource_tag add constraint resource_tag_resource_fk
    foreign key (resource_id) references resource (id) on delete cascade;

alter table resource_tag add constraint resource_tag_name_ck
    check (length(trim(name)) > 0);
