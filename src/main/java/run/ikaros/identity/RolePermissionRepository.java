package run.ikaros.identity;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 角色权限绑定的响应式持久化入口。
 */
public interface RolePermissionRepository extends ReactiveCrudRepository<RolePermissionEntity, UUID> {
    /**
     * 查询一个角色的全部权限绑定。
     *
     * @param roleId 角色标识
     * @return 权限绑定流
     */
    Flux<RolePermissionEntity> findAllByRoleId(UUID roleId);

    /**
     * 查询角色是否已拥有某项权限。
     *
     * @param roleId 角色标识
     * @param permissionKey 平台权限键
     * @return 可选的权限绑定
     */
    Mono<RolePermissionEntity> findByRoleIdAndPermissionKey(UUID roleId, String permissionKey);
}
