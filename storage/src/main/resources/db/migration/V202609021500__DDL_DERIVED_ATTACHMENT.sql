CREATE TABLE derived_attachment (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    source_attachment_id UUID NOT NULL,
    derived_attachment_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT uk_derived_attachment_target UNIQUE (derived_attachment_id),
    CONSTRAINT ck_derived_attachment_distinct CHECK (source_attachment_id <> derived_attachment_id)
);

CREATE INDEX idx_derived_attachment_source ON derived_attachment (source_attachment_id);
