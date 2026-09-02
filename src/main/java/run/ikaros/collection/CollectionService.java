package run.ikaros.collection;

import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Collection 的公开业务能力。
 */
public interface CollectionService {

    /**
     * 创建当前用户拥有的 Collection。
     *
     * @param ownerId 当前拥有者标识
     * @param request 集合名称与描述
     * @return 已创建集合
     */
    Mono<CollectionView> create(UUID ownerId, CreateCollectionRequest request);

    /**
     * 查询当前用户的 Collection。
     *
     * @param ownerId 当前拥有者标识
     * @return Collection 列表
     */
    Mono<List<CollectionView>> list(UUID ownerId);

    /**
     * 将当前用户拥有的 Resource 加入 Collection。
     *
     * @param ownerId 当前拥有者标识
     * @param collectionId Collection 标识
     * @param resourceId Resource 标识
     * @param position 集合内排序位置
     * @return 完成信号
     */
    Mono<Void> addResource(UUID ownerId, UUID collectionId, UUID resourceId, int position);

    /** 移动集合并拒绝自引用及任意深度祖先循环。 */
    Mono<CollectionView> move(UUID ownerId, UUID collectionId, UUID parentId);

    Mono<Void> removeResource(UUID ownerId, UUID collectionId, UUID resourceId);
}
