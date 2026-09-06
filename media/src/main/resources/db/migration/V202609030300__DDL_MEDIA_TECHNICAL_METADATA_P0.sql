CREATE TABLE media_probe (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), release_id UUID NOT NULL, container VARCHAR(64), duration_millis BIGINT,
    bitrate BIGINT, width INTEGER, height INTEGER, frame_rate VARCHAR(64), video_codec VARCHAR(64), audio_codec VARCHAR(64),
    probe_profile_version VARCHAR(128) NOT NULL, streams JSONB NOT NULL DEFAULT '[]'::jsonb,
    probed_at TIMESTAMPTZ NOT NULL DEFAULT current_timestamp, version BIGINT NOT NULL DEFAULT 0,
    CHECK (duration_millis IS NULL OR duration_millis >= 0), CHECK (bitrate IS NULL OR bitrate >= 0),
    CHECK (width IS NULL OR width > 0), CHECK (height IS NULL OR height > 0), CHECK (version >= 0),
    FOREIGN KEY (release_id) REFERENCES media_release(id), UNIQUE (release_id, probe_profile_version)
);
CREATE TABLE media_track (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), probe_id UUID NOT NULL, kind VARCHAR(24) NOT NULL, stable_key VARCHAR(256) NOT NULL,
    language VARCHAR(32), title VARCHAR(512), codec VARCHAR(64), width INTEGER, height INTEGER, channels INTEGER,
    sample_rate INTEGER, bitrate INTEGER, forced BOOLEAN NOT NULL DEFAULT FALSE, is_default BOOLEAN NOT NULL DEFAULT FALSE,
    hearing_impaired BOOLEAN NOT NULL DEFAULT FALSE, CHECK (kind IN ('VIDEO','AUDIO','EMBEDDED_SUBTITLE')),
    FOREIGN KEY (probe_id) REFERENCES media_probe(id) ON DELETE CASCADE, UNIQUE (probe_id, stable_key)
);
CREATE TABLE media_external_subtitle (
    id UUID PRIMARY KEY DEFAULT uuid_v7(), release_id UUID NOT NULL, attachment_id UUID NOT NULL, language VARCHAR(32) NOT NULL,
    title VARCHAR(512), format VARCHAR(32) NOT NULL, provider VARCHAR(256), offset_millis BIGINT NOT NULL DEFAULT 0,
    forced BOOLEAN NOT NULL DEFAULT FALSE, hearing_impaired BOOLEAN NOT NULL DEFAULT FALSE, version BIGINT NOT NULL DEFAULT 0,
    CHECK (version >= 0), FOREIGN KEY (release_id) REFERENCES media_release(id), FOREIGN KEY (attachment_id) REFERENCES attachment(id),
    UNIQUE (release_id, attachment_id)
);
