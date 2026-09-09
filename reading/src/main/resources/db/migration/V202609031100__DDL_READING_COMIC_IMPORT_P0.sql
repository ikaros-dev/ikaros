CREATE TABLE reading_comic_import (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    owner_id UUID NOT NULL,
    source_attachment_id UUID NOT NULL REFERENCES attachment(id),
    work_id UUID NOT NULL REFERENCES reading_work(id),
    edition_id UUID NOT NULL REFERENCES reading_edition(id),
    status VARCHAR(24) NOT NULL,
    error_code VARCHAR(64),
    error_message VARCHAR(512),
    idempotency_key VARCHAR(256) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(owner_id, idempotency_key)
);
