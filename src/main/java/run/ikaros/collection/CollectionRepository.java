package run.ikaros.collection;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Collection 的数据库访问边界。
 */
public interface CollectionRepository extends ReactiveCrudRepository<CollectionEntity, UUID> {

    /**
     * 查询拥有者的所有集合。
     *
     * @param ownerId 当前拥有者标识
     * @return Collection 列表
     */
    Flux<CollectionEntity> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    /**
     * 根据拥有者读取单个集合。
     *
     * @param id Collection 标识
     * @param ownerId 当前拥有者标识
     * @return 集合，未命中时为空
     */
    Mono<CollectionEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
}
