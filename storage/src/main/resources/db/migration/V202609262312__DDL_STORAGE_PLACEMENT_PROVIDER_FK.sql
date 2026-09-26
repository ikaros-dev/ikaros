alter table blob_placement
    alter column provider type varchar(256);

alter table blob_placement
    add constraint blob_placement_provider_fk
    foreign key (provider) references storage_provider(provider_key)
    on delete restrict not valid;
