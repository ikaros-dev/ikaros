alter table collection_resource add constraint collection_resource_collection_fk
    foreign key (collection_id) references collection (id) on delete cascade;

alter table collection_resource add constraint collection_resource_resource_fk
    foreign key (resource_id) references resource (id) on delete restrict;

alter table collection_resource add constraint collection_resource_position_ck
    check (position >= 0);
