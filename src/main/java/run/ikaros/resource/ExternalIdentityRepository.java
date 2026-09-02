package run.ikaros.resource;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 外部身份映射的数据库访问边界。
 */
public interface ExternalIdentityRepository extends ReactiveCrudRepository<ExternalIdentityEntity, UUID> {

    /**
     * 查询资源的外部身份列表。
     *
     * @param resourceId Resource 标识
     * @return 外部身份列表
     */
    Flux<ExternalIdentityEntity> findAllByResourceIdOrderByProviderAsc(UUID resourceId);

    Mono<ExternalIdentityEntity> findByIdAndResourceId(UUID id, UUID resourceId);
}
