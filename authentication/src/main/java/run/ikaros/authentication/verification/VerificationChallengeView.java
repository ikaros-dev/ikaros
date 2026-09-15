package run.ikaros.authentication.verification;

import java.time.Instant;
import java.util.UUID;

/**
 * 可返回给调用方的挑战摘要，不包含 OTP、摘要或目标邮箱；命中复用窗口时可包含短期 Verification Grant。
 */
public record VerificationChallengeView(UUID id, VerificationMethod method, VerificationPurpose purpose,
                                        Instant expiresAt, VerificationChallengeStatus status,
                                        String verificationGrant) {
    public VerificationChallengeView(UUID id, VerificationMethod method, VerificationPurpose purpose,
                                      Instant expiresAt, VerificationChallengeStatus status) {
        this(id, method, purpose, expiresAt, status, null);
    }
}
