alter table background_task add column parent_task_id uuid;
alter table background_task add constraint background_task_parent_fk foreign key (parent_task_id)
    references background_task(id) on delete restrict;
create index background_task_parent_idx on background_task(parent_task_id);
