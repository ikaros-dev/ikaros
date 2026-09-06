package run.ikaros.relation;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * Resource 关系的响应式持久化入口。
 */
public interface ResourceRelationRepository extends ReactiveCrudRepository<ResourceRelationEntity, UUID> {
    /**
     * 按来源资源读取出向关系。
     *
     * @param sourceResourceId 来源 Resource 标识
     * @return 排序后的关系流
     */
    Flux<ResourceRelationEntity> findAllBySourceResourceIdOrderByRelationTypeAscPositionAsc(UUID sourceResourceId);
}
