-- add column 'comment' in table 'subject_collection', @see run.ikaros.server.store.entity.SubjectCollectionEntity
alter table subject_collection add column comment varchar(5000);

-- add column 'score' in table 'subject_collection', @see run.ikaros.server.store.entity.SubjectCollectionEntity
alter table subject_collection add column score int8;
