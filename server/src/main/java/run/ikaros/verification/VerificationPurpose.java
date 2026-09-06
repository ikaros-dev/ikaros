package run.ikaros.verification;

/**
 * 验证码的明确用途；认证凭据不能跨用途复用。
 */
public enum VerificationPurpose {
    LOGIN_STEP_UP,
    CHANGE_SECURITY_SETTING,
    RESET_SECURE_KEY,
    EXPORT_SECURE_VAULT,
    CHANGE_RECOVERY_POLICY
}
