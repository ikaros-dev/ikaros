alter table resource_relation add constraint resource_relation_type_ck
    check (relation_type in ('CONTAINS', 'PART_OF', 'PREQUEL_TO', 'SEQUEL_TO',
        'ADAPTATION_OF', 'VERSION_OF', 'DERIVED_FROM', 'RELATED_TO'));

alter table resource_relation add constraint resource_relation_source_fk
    foreign key (source_resource_id) references resource (id) on delete restrict;

alter table resource_relation add constraint resource_relation_target_fk
    foreign key (target_resource_id) references resource (id) on delete restrict;
