package run.ikaros.identity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * 安全会话的响应式持久化入口。
 */
public interface SecuritySessionRepository extends ReactiveCrudRepository<SecuritySessionEntity, UUID> {
    /**
     * 查询用户尚未撤销且未过期的安全会话。
     *
     * @param userId 用户标识
     * @param now 当前时间
     * @return 活跃会话流
     */
    Flux<SecuritySessionEntity> findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfter(UUID userId, Instant now);
}
