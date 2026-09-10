ALTER TABLE photo_album ADD COLUMN cover_photo_id UUID;
ALTER TABLE photo_album ADD CONSTRAINT fk_photo_album_cover FOREIGN KEY (cover_photo_id) REFERENCES photo(id);
