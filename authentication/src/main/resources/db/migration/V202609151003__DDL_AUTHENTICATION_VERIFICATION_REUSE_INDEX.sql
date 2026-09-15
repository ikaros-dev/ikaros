CREATE INDEX idx_verification_challenge_user_reuse
    ON verification_challenge (user_id, purpose, status, consumed_at DESC);
