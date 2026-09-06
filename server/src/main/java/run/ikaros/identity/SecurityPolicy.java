package run.ikaros.identity;

/**
 * 高风险命令的权限与安全验证要求；调用方必须同时满足两类条件。
 */
public record SecurityPolicy(String action, PlatformPermission permission,
                             SecurityVerificationLevel minimumSvl, boolean requireFreshVerification) {
}
