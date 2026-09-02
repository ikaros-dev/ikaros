package run.ikaros.verification;

import java.time.Instant;
import java.util.UUID;

/**
 * 可返回给调用方的挑战摘要，不包含 OTP、摘要或目标邮箱。
 */
public record VerificationChallengeView(UUID id, VerificationMethod method, VerificationPurpose purpose,
                                        Instant expiresAt, VerificationChallengeStatus status) {
}
