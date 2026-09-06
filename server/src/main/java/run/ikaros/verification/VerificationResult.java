package run.ikaros.verification;

import java.time.Instant;
import java.util.UUID;
import run.ikaros.authentication.api.SecurityVerificationLevel;

/**
 * Provider 返回的标准验证结果；调用者仍需经过安全策略和权限检查。
 */
public record VerificationResult(UUID challengeId, VerificationMethod method, SecurityVerificationLevel achievedSvl,
                                 UUID subjectId, Instant verifiedAt, Instant expiresAt, String verificationGrant) {
    public VerificationResult(UUID challengeId, VerificationMethod method, SecurityVerificationLevel achievedSvl,
                               UUID subjectId, Instant verifiedAt, Instant expiresAt) {
        this(challengeId, method, achievedSvl, subjectId, verifiedAt, expiresAt, null);
    }
}
