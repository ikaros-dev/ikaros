package run.ikaros.relation;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Resource 关系的业务边界。
 */
public interface ResourceRelationService {
    /**
     * 建立当前用户两个资源间的有向关系。
     *
     * @param ownerId 当前用户标识
     * @param sourceResourceId 来源 Resource 标识
     * @param request 目标与关系类型
     * @return 新建关系视图
     */
    Mono<ResourceRelationView> create(UUID ownerId, UUID sourceResourceId, CreateResourceRelationRequest request);

    /**
     * 查询当前用户资源的出向关系。
     *
     * @param ownerId 当前用户标识
     * @param sourceResourceId 来源 Resource 标识
     * @return 关系视图流
     */
    Flux<ResourceRelationView> list(UUID ownerId, UUID sourceResourceId);

    /**
     * 删除当前用户资源的一条出向关系。
     *
     * @param ownerId 当前用户标识
     * @param sourceResourceId 来源 Resource 标识
     * @param relationId 关系标识
     * @return 完成信号
     */
    Mono<Void> remove(UUID ownerId, UUID sourceResourceId, UUID relationId);
}
