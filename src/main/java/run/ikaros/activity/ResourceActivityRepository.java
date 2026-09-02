package run.ikaros.activity;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Resource Activity 的数据库访问边界。
 */
public interface ResourceActivityRepository extends ReactiveCrudRepository<ResourceActivityEntity, UUID> {
    /** 查询用户最近的活动记录。 */
    Flux<ResourceActivityEntity> findAllByOwnerIdOrderByOccurredAtDesc(UUID ownerId);

    /** 查询用户拥有的指定活动记录。 */
    Mono<ResourceActivityEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
