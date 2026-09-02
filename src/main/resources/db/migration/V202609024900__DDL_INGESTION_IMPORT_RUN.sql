CREATE TABLE ingestion_import_run (
 id UUID PRIMARY KEY DEFAULT uuid_v7(), plan_id UUID NOT NULL, owner_id UUID NOT NULL, actor_id UUID,
 status VARCHAR(32) NOT NULL DEFAULT 'PENDING', checkpoint VARCHAR(2048), completed_count BIGINT NOT NULL DEFAULT 0,
 failed_count BIGINT NOT NULL DEFAULT 0, skipped_count BIGINT NOT NULL DEFAULT 0, background_task_id UUID,
 started_at TIMESTAMPTZ, finished_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT ck_ingestion_run_counts CHECK (completed_count >= 0 AND failed_count >= 0 AND skipped_count >= 0)
);
CREATE INDEX idx_ingestion_run_owner_created ON ingestion_import_run (owner_id, created_at DESC);
