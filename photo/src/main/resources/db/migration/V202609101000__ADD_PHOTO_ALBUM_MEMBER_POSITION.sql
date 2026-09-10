ALTER TABLE photo_album_member ADD COLUMN position INTEGER;
WITH ordered AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY album_id ORDER BY added_at, id) - 1 AS member_position
    FROM photo_album_member
)
UPDATE photo_album_member member SET position = ordered.member_position FROM ordered WHERE member.id = ordered.id;
ALTER TABLE photo_album_member ALTER COLUMN position SET NOT NULL;
CREATE INDEX idx_photo_album_member_order ON photo_album_member(album_id, position, added_at);
