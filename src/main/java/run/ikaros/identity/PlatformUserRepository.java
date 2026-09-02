package run.ikaros.identity;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 平台用户身份的响应式持久化入口。
 */
public interface PlatformUserRepository extends ReactiveCrudRepository<PlatformUserEntity, UUID> {
    /**
     * 按用户状态与用户名查找用户。
     *
     * @param status 可选的用户状态
     * @param query 可选的用户名关键词
     * @return 匹配的用户流
     */
    Flux<PlatformUserEntity> findAllByStatusAndUsernameContainingIgnoreCase(UserStatus status, String query);

    /**
     * 根据用户名查找用户。
     *
     * @param username 用户名
     * @return 对应用户
     */
    Mono<PlatformUserEntity> findByUsername(String username);
}
