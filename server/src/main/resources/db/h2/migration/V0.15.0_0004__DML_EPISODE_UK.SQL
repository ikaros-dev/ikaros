alter table episode
    drop constraint subject_group_seq_uk;

ALTER TABLE episode
    ADD CONSTRAINT subject_group_seq_name_uk UNIQUE (subject_id, ep_group, sequence, name);