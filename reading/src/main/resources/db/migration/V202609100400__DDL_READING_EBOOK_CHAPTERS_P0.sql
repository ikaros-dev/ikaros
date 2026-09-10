CREATE TABLE reading_ebook_chapter (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    import_id UUID NOT NULL REFERENCES reading_ebook_import(id) ON DELETE CASCADE,
    chapter_id UUID NOT NULL REFERENCES reading_chapter(id) ON DELETE CASCADE,
    href VARCHAR(2048) NOT NULL,
    title VARCHAR(512) NOT NULL,
    sort_order INTEGER NOT NULL CHECK (sort_order >= 0),
    UNIQUE(import_id, sort_order),
    UNIQUE(import_id, href)
);
