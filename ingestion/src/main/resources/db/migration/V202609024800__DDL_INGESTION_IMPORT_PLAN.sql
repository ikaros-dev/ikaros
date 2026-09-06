CREATE TABLE ingestion_import_plan (
 id UUID PRIMARY KEY DEFAULT uuid_v7(), scan_run_id UUID NOT NULL, owner_id UUID NOT NULL,
 dry_run BOOLEAN NOT NULL, status VARCHAR(24) NOT NULL, policy_snapshot_json TEXT NOT NULL DEFAULT '{}',
 generated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE ingestion_import_plan_item (
 id UUID PRIMARY KEY DEFAULT uuid_v7(), plan_id UUID NOT NULL, candidate_id UUID NOT NULL,
 action VARCHAR(48) NOT NULL, target_id UUID, reason VARCHAR(2000), confidence INTEGER NOT NULL,
 idempotency_key VARCHAR(256) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
 version BIGINT NOT NULL DEFAULT 0, CONSTRAINT ck_ingestion_plan_confidence CHECK (confidence BETWEEN 0 AND 100),
 CONSTRAINT uk_ingestion_plan_item_key UNIQUE (plan_id, idempotency_key)
);
CREATE INDEX idx_ingestion_plan_owner_generated ON ingestion_import_plan (owner_id, generated_at DESC);
CREATE INDEX idx_ingestion_plan_item_plan ON ingestion_import_plan_item (plan_id, created_at);
