CREATE TABLE ingestion_import_conflict (
 id UUID PRIMARY KEY DEFAULT uuid_v7(), plan_id UUID NOT NULL, candidate_id UUID NOT NULL, owner_id UUID NOT NULL,
 reason VARCHAR(2000) NOT NULL, confidence INTEGER NOT NULL, status VARCHAR(24) NOT NULL DEFAULT 'OPEN', resolution VARCHAR(32),
 created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, resolved_at TIMESTAMPTZ, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT ck_ingestion_conflict_confidence CHECK (confidence BETWEEN 0 AND 100)
);
CREATE INDEX idx_ingestion_conflict_owner_status ON ingestion_import_conflict (owner_id, status, created_at);
