package run.ikaros.verification;

/**
 * 验证挑战的不可逆终态和可验证状态。
 */
public enum VerificationChallengeStatus {
    ISSUED,
    VERIFIED,
    LOCKED,
    EXPIRED,
    CANCELLED
}
