package run.ikaros.identity;

import java.util.UUID;
import reactor.core.publisher.Mono;
import run.ikaros.common.PageResponse;

/**
 * 平台用户目录的业务边界。
 */
public interface UserService {
    /**
     * 创建待激活的平台用户。
     *
     * @param actorId 执行创建的管理主体
     * @param request 用户资料
     * @return 新建用户视图
     */
    Mono<UserView> create(UUID actorId, CreateUserRequest request);

    /**
     * 分页查询平台用户。
     *
     * @param status 可选状态筛选
     * @param query 可选用户名关键词
     * @param page 从零开始的页码
     * @param size 每页数量
     * @return 用户分页视图
     */
    Mono<PageResponse<UserView>> list(UserStatus status, String query, int page, int size);

    /**
     * 变更用户可用状态。
     *
     * @param actorId 执行变更的管理主体
     * @param userId 用户标识
     * @param status 目标状态
     * @return 更新后的用户视图
     */
    Mono<UserView> changeStatus(UUID actorId, UUID userId, UserStatus status);

    /**
     * 为用户绑定平台角色。
     *
     * @param actorId 执行绑定的管理主体
     * @param userId 用户标识
     * @param roleId 角色标识
     * @return 完成信号
     */
    Mono<Void> assignRole(UUID actorId, UUID userId, UUID roleId);
}
