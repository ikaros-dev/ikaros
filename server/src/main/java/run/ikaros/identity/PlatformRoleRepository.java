package run.ikaros.identity;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * 平台角色的响应式持久化入口。
 */
public interface PlatformRoleRepository extends ReactiveCrudRepository<PlatformRoleEntity, UUID> {
    /**
     * 根据稳定角色编码查找角色。
     *
     * @param code 角色编码
     * @return 对应角色
     */
    Mono<PlatformRoleEntity> findByCode(String code);
}
