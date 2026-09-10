CREATE TABLE reading_comic_import_entry (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    import_id UUID NOT NULL REFERENCES reading_comic_import(id) ON DELETE CASCADE,
    chapter_key VARCHAR(256) NOT NULL,
    entry_name VARCHAR(1024) NOT NULL,
    page_order INTEGER NOT NULL,
    page_role VARCHAR(24) NOT NULL DEFAULT 'NORMAL',
    version BIGINT NOT NULL DEFAULT 0,
    CHECK(page_order >= 0),
    UNIQUE(import_id, entry_name),
    UNIQUE(import_id, chapter_key, page_order)
);
