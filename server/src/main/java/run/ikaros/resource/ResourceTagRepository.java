package run.ikaros.resource;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Resource 用户标签的数据库访问边界。
 */
public interface ResourceTagRepository extends ReactiveCrudRepository<ResourceTagEntity, UUID> {
    /** 查询指定资源的用户标签。 */
    Flux<ResourceTagEntity> findAllByOwnerIdAndResourceIdOrderByNameAsc(UUID ownerId, UUID resourceId);

    Flux<ResourceTagEntity> findAllByOwnerIdOrderByNameAsc(UUID ownerId);

    /** 按名称查询指定资源标签，用于保证添加幂等。 */
    Mono<ResourceTagEntity> findByOwnerIdAndResourceIdAndName(UUID ownerId, UUID resourceId, String name);

    /** 查询当前用户拥有的指定标签。 */
    Mono<ResourceTagEntity> findByIdAndOwnerIdAndResourceId(UUID id, UUID ownerId, UUID resourceId);
}
