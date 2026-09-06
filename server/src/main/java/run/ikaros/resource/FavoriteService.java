package run.ikaros.resource;

import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Resource 收藏状态的业务能力。
 */
public interface FavoriteService {

    /**
     * 收藏一个 Resource；重复调用保持幂等。
     *
     * @param ownerId 当前用户标识
     * @param resourceId Resource 标识
     * @return 收藏后的状态
     */
    Mono<FavoriteView> add(UUID ownerId, UUID resourceId);

    /**
     * 取消收藏一个 Resource；重复调用保持幂等。
     *
     * @param ownerId 当前用户标识
     * @param resourceId Resource 标识
     * @return 操作完成信号
     */
    Mono<Void> remove(UUID ownerId, UUID resourceId);

    /**
     * 查询当前用户对 Resource 的收藏状态。
     *
     * @param ownerId 当前用户标识
     * @param resourceId Resource 标识
     * @return 收藏状态
     */
    Mono<FavoriteView> get(UUID ownerId, UUID resourceId);
}
