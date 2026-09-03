package run.ikaros.resource;

import java.util.UUID;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.JsonNode;
import run.ikaros.common.PageResponse;

/**
 * Resource 聚合的公开业务能力。所有调用均以拥有者作为最小权限边界。
 */
public interface ResourceService {

    /**
     * 创建 Resource 与其首个主标题。
     *
     * @param ownerId 当前拥有者标识
     * @param request 创建请求
     * @return 已创建的完整 Resource
     */
    Mono<ResourceView> create(UUID ownerId, CreateResourceRequest request);

    Mono<ResourceView> create(UUID ownerId, CreateResourceRequest request, String idempotencyKey);

    /**
     * 获取当前拥有者可见的 Resource。
     *
     * @param ownerId 当前拥有者标识
     * @param resourceId Resource 标识
     * @return 完整 Resource
     */
    Mono<ResourceView> get(UUID ownerId, UUID resourceId);

    Mono<ResourceView> update(UUID ownerId, UUID resourceId, UpdateResourceRequest request);
    Mono<ResourceView> update(UUID ownerId, UUID resourceId, UpdateResourceRequest request,
                              boolean primaryTitlePresent, boolean summaryPresent);

    /**
     * 按标题关键词与类型分页浏览活动 Resource。
     *
     * @param ownerId 当前拥有者标识
     * @param type 可选类型过滤
     * @param query 可选标题关键词
     * @param page 从零开始的页码
     * @param size 每页记录数
     * @return 分页结果
     */
    Mono<PageResponse<ResourceView>> list(UUID ownerId, ResourceType type, String query, int page, int size);

    Mono<ResourceView> findByExternalIdentity(UUID ownerId, String provider, String externalType,
                                               String externalId);

    /**
     * 将 Resource 移入回收站，不直接删除其 Attachment 或 Blob。
     *
     * @param ownerId 当前拥有者标识
     * @param resourceId Resource 标识
     * @return 完成信号
     */
    Mono<Void> trash(UUID ownerId, UUID resourceId);

    Mono<Void> trash(UUID ownerId, UUID resourceId, long expectedVersion);

    /** 将 Resource 显式归档。 */
    Mono<ResourceView> archive(UUID ownerId, UUID resourceId);

    Mono<ResourceView> archive(UUID ownerId, UUID resourceId, long expectedVersion);

    /**
     * 从回收站恢复 Resource。
     *
     * @param ownerId 当前拥有者标识
     * @param resourceId Resource 标识
     * @return 已恢复的 Resource
     */
    Mono<ResourceView> restore(UUID ownerId, UUID resourceId);

    Mono<ResourceView> restore(UUID ownerId, UUID resourceId, long expectedVersion);

    /**
     * 为 Resource 添加稳定外部身份映射。
     *
     * @param ownerId 当前拥有者标识
     * @param resourceId Resource 标识
     * @param request 外部身份数据
     * @return 已创建的外部身份
     */
    Mono<ExternalIdentityView> addExternalIdentity(UUID ownerId, UUID resourceId,
                                                   CreateExternalIdentityRequest request);

    Mono<Void> detachExternalIdentity(UUID ownerId, UUID resourceId, UUID identityId);
}
