alter table attachment add constraint attachment_resource_fk
    foreign key (resource_id) references resource (id) on delete restrict;

alter table attachment add constraint attachment_blob_fk
    foreign key (blob_id) references blob (id) on delete cascade;

alter table blob_placement add constraint blob_placement_blob_fk
    foreign key (blob_id) references blob (id) on delete cascade;

alter table derived_attachment add constraint derived_attachment_source_fk
    foreign key (source_attachment_id) references attachment (id) on delete cascade;

alter table derived_attachment add constraint derived_attachment_target_fk
    foreign key (derived_attachment_id) references attachment (id) on delete cascade;
