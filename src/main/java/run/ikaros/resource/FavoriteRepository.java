package run.ikaros.resource;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Resource 收藏关系的数据库访问边界。
 */
public interface FavoriteRepository extends ReactiveCrudRepository<FavoriteEntity, UUID> {

    /**
     * 查询用户对指定 Resource 的收藏。
     *
     * @param ownerId 用户标识
     * @param resourceId Resource 标识
     * @return 收藏关系，未收藏时为空
     */
    Mono<FavoriteEntity> findByOwnerIdAndResourceId(UUID ownerId, UUID resourceId);

    /**
     * 删除用户对指定 Resource 的收藏。
     *
     * @param ownerId 用户标识
     * @param resourceId Resource 标识
     * @return 删除完成信号
     */
    Mono<Void> deleteByOwnerIdAndResourceId(UUID ownerId, UUID resourceId);

    /**
     * 按收藏时间倒序查询用户的收藏关系。
     *
     * @param ownerId 用户标识
     * @return 收藏关系列表
     */
    Flux<FavoriteEntity> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}
