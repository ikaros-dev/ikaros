CREATE TABLE media_playback_session (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, resource_id UUID NOT NULL, release_id UUID NOT NULL,
    state VARCHAR(16) NOT NULL DEFAULT 'ACTIVE', started_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    ended_at TIMESTAMPTZ, last_position_seconds BIGINT NOT NULL DEFAULT 0, version BIGINT NOT NULL DEFAULT 0,
    CHECK (state IN ('ACTIVE','ENDED')), CHECK (last_position_seconds >= 0), CHECK (version >= 0),
    CHECK ((state = 'ACTIVE' AND ended_at IS NULL) OR (state = 'ENDED' AND ended_at IS NOT NULL)),
    FOREIGN KEY (resource_id) REFERENCES resource(id), FOREIGN KEY (release_id) REFERENCES media_release(id)
);
CREATE INDEX idx_media_playback_session_owner_started ON media_playback_session (owner_id, started_at DESC);

CREATE TABLE media_playback_history (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), owner_id UUID NOT NULL, resource_id UUID NOT NULL, session_id UUID NOT NULL,
    started_at TIMESTAMPTZ NOT NULL, ended_at TIMESTAMPTZ NOT NULL, watched_seconds BIGINT NOT NULL DEFAULT 0,
    CHECK (ended_at >= started_at), CHECK (watched_seconds >= 0),
    FOREIGN KEY (resource_id) REFERENCES resource(id), FOREIGN KEY (session_id) REFERENCES media_playback_session(id)
);
CREATE INDEX idx_media_playback_history_owner_ended ON media_playback_history (owner_id, ended_at DESC);
