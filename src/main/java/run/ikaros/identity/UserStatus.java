package run.ikaros.identity;

/**
 * 平台用户可被认证和授权层识别的生命周期状态。
 */
public enum UserStatus {
    PENDING,
    ACTIVE,
    DISABLED,
    LOCKED,
    DEACTIVATED
}
