package run.ikaros.verification;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * 验证挑战的响应式持久化入口。
 */
public interface VerificationChallengeRepository extends ReactiveCrudRepository<VerificationChallengeEntity, UUID> {
    /**
     * 统计用户在窗口内已发起的挑战数量。
     *
     * @param userId 用户标识
     * @param issuedAfter 统计窗口下界
     * @return 已发起挑战数
     */
    Mono<Long> countByUserIdAndIssuedAtAfter(UUID userId, Instant issuedAfter);
}
