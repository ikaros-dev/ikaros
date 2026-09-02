package run.ikaros.metadata;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Resource 元数据的响应式持久化入口。 */
public interface ResourceMetadataRepository extends ReactiveCrudRepository<ResourceMetadataEntity, UUID> {
    Flux<ResourceMetadataEntity> findAllByResourceIdOrderByFieldKeyAsc(UUID resourceId);
    Mono<ResourceMetadataEntity> findByResourceIdAndFieldKey(UUID resourceId, String fieldKey);
}
