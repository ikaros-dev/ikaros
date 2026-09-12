ALTER TABLE share_grant
  ADD COLUMN password_digest VARCHAR(128),
  ADD COLUMN allow_download BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN max_access_count INTEGER,
  ADD COLUMN access_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE share_grant
  ADD CONSTRAINT ck_share_access_count CHECK (access_count >= 0),
  ADD CONSTRAINT ck_share_max_access_count CHECK (max_access_count IS NULL OR max_access_count > 0);
