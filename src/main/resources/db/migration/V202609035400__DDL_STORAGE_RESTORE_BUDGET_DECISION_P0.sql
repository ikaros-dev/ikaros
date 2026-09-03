alter table storage_restore_request
    add column budget_decision varchar(32) not null default 'ACCEPTED';

alter table storage_restore_request
    add constraint ck_storage_restore_request_budget_decision
        check (budget_decision in ('ACCEPTED', 'PARTIAL', 'CONFIRMED', 'QUEUED', 'REJECTED'));
