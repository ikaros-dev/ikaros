alter table storage_restore_request drop constraint if exists storage_restore_request_status_check;
alter table storage_restore_request drop constraint if exists ck_storage_restore_request_status;
alter table storage_restore_request
    add constraint ck_storage_restore_request_status
        check (status in ('QUEUED', 'REQUESTED', 'IN_PROGRESS', 'COMPLETED', 'PARTIAL_FAILURE', 'FAILED', 'CANCELLED'));
