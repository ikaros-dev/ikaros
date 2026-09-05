alter table background_task
    add column if not exists attempt integer not null default 0;

alter table background_task
    add constraint background_task_attempt_ck check (attempt >= 0);
