package run.ikaros.identity;

import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 安全会话与 Step-up 验证状态的业务边界。
 */
public interface SecuritySessionService {
    /**
     * 为已经通过未来认证 Provider 验证的活跃用户建立普通会话。
     *
     * @param userId 用户标识
     * @param loginMethod 登录方式标识
     * @param expiresAt 会话到期时间
     * @return 新建安全会话视图
     */
    Mono<SessionView> open(UUID userId, String loginMethod, Instant expiresAt);

    /**
     * 更新会话的短期 Step-up 验证等级。
     *
     * @param userId 会话所属用户
     * @param userId 会话所属用户标识
     * @param sessionId 会话标识
     * @param level 已完成的验证等级
     * @param verificationExpiresAt 验证保证到期时间
     * @return 更新后的安全会话视图
     */
    Mono<SessionView> stepUp(UUID userId, UUID sessionId, SecurityVerificationLevel level,
                             Instant verificationExpiresAt);

    /**
     * 查询用户当前未撤销且未过期的会话。
     *
     * @param userId 用户标识
     * @return 活跃会话视图流
     */
    Flux<SessionView> listActive(UUID userId);

    /**
     * 撤销指定会话，使其不能继续代表用户执行受保护请求。
     *
     * @param actorId 执行撤销的主体
     * @param sessionId 会话标识
     * @return 完成信号
     */
    Mono<Void> revoke(UUID actorId, UUID userId, UUID sessionId);

    /** 撤销当前主体持有的会话，用于登出。 */
    Mono<Void> revokeCurrent(UUID actorId, UUID sessionId);

    /** 撤销用户的全部活跃会话，并提升用户安全版本。 */
    Mono<Void> revokeAll(UUID actorId, UUID userId);
}
