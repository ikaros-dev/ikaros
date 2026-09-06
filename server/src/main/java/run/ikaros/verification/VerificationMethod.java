package run.ikaros.verification;

/**
 * 可插拔认证 Provider 对应的验证方式。
 */
public enum VerificationMethod {
    EMAIL_OTP,
    SMS_OTP,
    IDENTITY_DOCUMENT,
    FACE_IDENTITY
}
