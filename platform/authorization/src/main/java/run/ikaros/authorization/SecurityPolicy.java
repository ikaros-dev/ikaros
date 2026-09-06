package run.ikaros.authorization;

import run.ikaros.authentication.api.SecurityVerificationLevel;
import run.ikaros.authorization.api.PlatformPermission;

/**
 * 高风险命令的权限与安全验证要求；调用方必须同时满足两类条件。
 */
public record SecurityPolicy(String action, PlatformPermission permission,
                             SecurityVerificationLevel minimumSvl, boolean requireFreshVerification) {
}
