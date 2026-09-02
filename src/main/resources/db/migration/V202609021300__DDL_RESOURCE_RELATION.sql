CREATE TABLE resource_relation (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    source_resource_id UUID NOT NULL,
    target_resource_id UUID NOT NULL,
    relation_type VARCHAR(48) NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT ck_resource_relation_distinct CHECK (source_resource_id <> target_resource_id),
    CONSTRAINT uk_resource_relation UNIQUE (source_resource_id, target_resource_id, relation_type)
);

CREATE INDEX idx_resource_relation_source ON resource_relation (source_resource_id, relation_type, position);
CREATE INDEX idx_resource_relation_target ON resource_relation (target_resource_id, relation_type);
