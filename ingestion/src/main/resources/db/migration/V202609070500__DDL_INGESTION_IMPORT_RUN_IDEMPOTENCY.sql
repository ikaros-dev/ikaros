ALTER TABLE ingestion_import_run ADD COLUMN idempotency_key VARCHAR(256);
CREATE UNIQUE INDEX uk_ingestion_import_run_owner_key
    ON ingestion_import_run (owner_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
