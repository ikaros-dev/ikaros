package run.ikaros.verification;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 一次性验证码挑战的持久化状态；仅保存慢哈希摘要，绝不保存 OTP 明文。
 */
@Table("verification_challenge")
public record VerificationChallengeEntity(
    @Id UUID id,
    @Column("user_id") UUID userId,
    @Column("verification_method") VerificationMethod method,
    VerificationPurpose purpose,
    @Column("target_reference") String targetReference,
    @Column("otp_digest") String otpDigest,
    @Column("issued_at") Instant issuedAt,
    @Column("expires_at") Instant expiresAt,
    @Column("attempt_count") int attemptCount,
    @Column("max_attempts") int maxAttempts,
    @Column("consumed_at") Instant consumedAt,
    VerificationChallengeStatus status,
    @Version Long version
) {
}
