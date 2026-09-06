package run.ikaros.identity;

import java.util.UUID;
import java.time.Instant;
import reactor.core.publisher.Mono;
import run.ikaros.authentication.api.SecurityVerificationLevel;

/**
 * 对单个受保护命令同时执行用户状态、RBAC 与安全验证等级校验。
 */
public interface AccessControlService {
    /**
     * 验证会话是否可以执行一个具有安全策略的命令。
     *
     * @param userId 当前用户标识
     * @param currentSvl 当前 Token / Verification Grant 的安全验证等级
     * @param verificationExpiresAt 当前验证保证到期时间
     * @param policy 命令安全策略
     * @return 校验通过时的完成信号，失败时返回拒绝异常
     */
    Mono<Void> require(UUID userId, SecurityVerificationLevel currentSvl,
                       Instant verificationExpiresAt, SecurityPolicy policy);
}
