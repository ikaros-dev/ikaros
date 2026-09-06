CREATE TABLE verification_challenge (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    user_id UUID NOT NULL,
    verification_method VARCHAR(32) NOT NULL,
    purpose VARCHAR(64) NOT NULL,
    target_reference VARCHAR(255),
    otp_digest VARCHAR(512) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    consumed_at TIMESTAMPTZ,
    status VARCHAR(24) NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT ck_verification_challenge_attempts CHECK (attempt_count >= 0 AND max_attempts > 0)
);

CREATE INDEX idx_verification_challenge_user_issued ON verification_challenge (user_id, issued_at DESC);
CREATE INDEX idx_verification_challenge_user_status ON verification_challenge (user_id, status, expires_at);
