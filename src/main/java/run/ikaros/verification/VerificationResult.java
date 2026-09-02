package run.ikaros.verification;

import java.time.Instant;
import java.util.UUID;
import run.ikaros.identity.SecurityVerificationLevel;

/**
 * Provider 返回的标准验证结果；调用者仍需经过安全策略和权限检查。
 */
public record VerificationResult(UUID challengeId, VerificationMethod method, SecurityVerificationLevel achievedSvl,
                                 UUID subjectId, Instant verifiedAt, Instant expiresAt) {
}
