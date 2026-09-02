alter table collection add column parent_id uuid;

alter table collection add constraint collection_parent_not_self_ck
    check (parent_id is null or parent_id <> id);

alter table collection add constraint collection_parent_fk
    foreign key (parent_id) references collection (id) on delete restrict;

create index idx_collection_parent on collection (parent_id, updated_at desc);
