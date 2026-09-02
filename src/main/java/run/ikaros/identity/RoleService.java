package run.ikaros.identity;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 平台角色与权限注册表的业务边界。
 */
public interface RoleService {
    /**
     * 创建自定义平台角色。
     *
     * @param actorId 执行创建的管理主体
     * @param request 角色资料
     * @return 新建角色视图
     */
    Mono<RoleView> create(UUID actorId, CreateRoleRequest request);

    /**
     * 列出全部平台角色及其权限。
     *
     * @return 角色视图流
     */
    Flux<RoleView> list();

    /**
     * 为角色授予已声明的平台权限。
     *
     * @param actorId 执行授权的管理主体
     * @param roleId 角色标识
     * @param permission 平台权限
     * @return 更新后的角色视图
     */
    Mono<RoleView> grantPermission(UUID actorId, UUID roleId, PlatformPermission permission);
}
