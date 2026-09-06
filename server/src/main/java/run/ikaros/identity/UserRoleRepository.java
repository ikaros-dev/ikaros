package run.ikaros.identity;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 用户角色绑定的响应式持久化入口。
 */
public interface UserRoleRepository extends ReactiveCrudRepository<UserRoleEntity, UUID> {
    /**
     * 查询用户的全部角色绑定。
     *
     * @param userId 用户标识
     * @return 角色绑定流
     */
    Flux<UserRoleEntity> findAllByUserId(UUID userId);

    /**
     * 查询指定用户与角色间的绑定。
     *
     * @param userId 用户标识
     * @param roleId 角色标识
     * @return 可选的角色绑定
     */
    Mono<UserRoleEntity> findByUserIdAndRoleId(UUID userId, UUID roleId);

    Mono<Void> deleteByUserIdAndRoleId(UUID userId, UUID roleId);
}
