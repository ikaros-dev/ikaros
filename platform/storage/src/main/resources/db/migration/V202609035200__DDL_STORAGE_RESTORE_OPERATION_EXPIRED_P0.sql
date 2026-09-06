alter table storage_restore_operation drop constraint if exists storage_restore_operation_status_check;

alter table storage_restore_operation
    add constraint storage_restore_operation_status_check
    check (status in ('REQUESTED', 'IN_PROGRESS', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'EXPIRED'));
