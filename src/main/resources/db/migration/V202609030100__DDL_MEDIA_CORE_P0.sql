CREATE TABLE media_subject (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, resource_id UUID NOT NULL,
    kind VARCHAR(16) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, version BIGINT NOT NULL DEFAULT 0,
    CHECK (kind IN ('SERIES','MOVIE','VIDEO')), CHECK (version >= 0),
    FOREIGN KEY (resource_id) REFERENCES resource(id), UNIQUE (owner_id, resource_id)
);
CREATE INDEX idx_media_subject_owner ON media_subject (owner_id, updated_at DESC);

CREATE TABLE media_season (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, subject_id UUID NOT NULL,
    resource_id UUID NOT NULL, season_number INTEGER NOT NULL, name VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, CHECK (season_number >= 0), CHECK (version >= 0),
    FOREIGN KEY (subject_id) REFERENCES media_subject(id), FOREIGN KEY (resource_id) REFERENCES resource(id),
    UNIQUE (subject_id, season_number), UNIQUE (owner_id, resource_id)
);
CREATE INDEX idx_media_season_subject ON media_season (subject_id, season_number);

CREATE TABLE media_episode (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, subject_id UUID NOT NULL,
    season_id UUID, resource_id UUID NOT NULL, episode_number INTEGER NOT NULL, absolute_number INTEGER,
    air_date TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, version BIGINT NOT NULL DEFAULT 0,
    CHECK (episode_number >= 0), CHECK (absolute_number IS NULL OR absolute_number >= 0), CHECK (version >= 0),
    FOREIGN KEY (subject_id) REFERENCES media_subject(id), FOREIGN KEY (season_id) REFERENCES media_season(id),
    FOREIGN KEY (resource_id) REFERENCES resource(id), UNIQUE (season_id, episode_number), UNIQUE (owner_id, resource_id)
);
CREATE INDEX idx_media_episode_subject ON media_episode (subject_id, episode_number);

CREATE TABLE media_release (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, playable_resource_id UUID NOT NULL,
    attachment_id UUID NOT NULL, release_group VARCHAR(256), version_label VARCHAR(128),
    state VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE', content_fingerprint VARCHAR(256),
    created_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, updated_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    version BIGINT NOT NULL DEFAULT 0, CHECK (state IN ('AVAILABLE','MISSING','CORRUPTED','ARCHIVED')), CHECK (version >= 0),
    FOREIGN KEY (playable_resource_id) REFERENCES resource(id), FOREIGN KEY (attachment_id) REFERENCES attachment(id),
    UNIQUE (playable_resource_id, attachment_id)
);
CREATE INDEX idx_media_release_resource_state ON media_release (playable_resource_id, state, created_at DESC);
