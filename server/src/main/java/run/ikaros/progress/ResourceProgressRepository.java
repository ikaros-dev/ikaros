package run.ikaros.progress;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Resource 消费进度的数据库访问边界。
 */
public interface ResourceProgressRepository extends ReactiveCrudRepository<ResourceProgressEntity, UUID> {
    /** 查询用户在资源上的指定类型进度。 */
    Mono<ResourceProgressEntity> findByOwnerIdAndResourceIdAndProgressType(UUID ownerId, UUID resourceId,
                                                                            ProgressType progressType);

    /** 查询用户最近更新的全部进度。 */
    Flux<ResourceProgressEntity> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
}
