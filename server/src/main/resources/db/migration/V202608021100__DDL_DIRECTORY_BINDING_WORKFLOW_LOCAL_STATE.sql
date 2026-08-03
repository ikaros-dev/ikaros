ALTER TABLE directory_binding_workflow
    ADD COLUMN IF NOT EXISTS local_mode varchar(32),
    ADD COLUMN IF NOT EXISTS local_scan_state text,
    ADD COLUMN IF NOT EXISTS version bigint DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS directory_binding_workflow_local_unique_idx
    ON directory_binding_workflow (directory_id, subject_id, local_mode);
