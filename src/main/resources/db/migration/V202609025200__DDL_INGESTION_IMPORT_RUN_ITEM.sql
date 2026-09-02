CREATE TABLE ingestion_import_run_item (
 id UUID PRIMARY KEY DEFAULT uuid_v7(), run_id UUID NOT NULL, plan_item_id UUID NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'PENDING', attempt_count INTEGER NOT NULL DEFAULT 0,
 error_message VARCHAR(2000), idempotency_key VARCHAR(256) NOT NULL, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
 version BIGINT NOT NULL DEFAULT 0, CONSTRAINT ck_ingestion_run_item_attempt CHECK (attempt_count >= 0),
 CONSTRAINT uk_ingestion_run_item_key UNIQUE (run_id, idempotency_key)
);
CREATE INDEX idx_ingestion_run_item_run_updated ON ingestion_import_run_item (run_id, updated_at);
