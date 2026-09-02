package run.ikaros.collection;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * Collection 成员关系的数据库访问边界。
 */
public interface CollectionResourceRepository extends ReactiveCrudRepository<CollectionResourceEntity, UUID> {

    /**
     * 查询集合成员关系。
     *
     * @param collectionId Collection 标识
     * @return 按位置排序的成员关系
     */
    Flux<CollectionResourceEntity> findAllByCollectionIdOrderByPositionAsc(UUID collectionId);
}
