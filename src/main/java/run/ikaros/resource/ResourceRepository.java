package run.ikaros.resource;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Resource 聚合根的数据库访问边界。
 */
public interface ResourceRepository extends ReactiveCrudRepository<ResourceEntity, UUID> {

    /**
     * 按拥有者读取单个资源。
     *
     * @param id Resource 标识
     * @param ownerId 当前拥有者标识
     * @return 可访问资源，未找到时为空
     */
    Mono<ResourceEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    /**
     * 按标题和类型分页查询活动资源。
     *
     * @param ownerId 资源拥有者
     * @param resourceType 类型过滤，空字符串表示不过滤
     * @param query 标题关键词，空字符串表示不过滤
     * @param offset 跳过的记录数
     * @param limit 返回的最大记录数
     * @return 当前页资源
     */
    @Query("""
        select distinct r.* from resource r
        join resource_title t on t.resource_id = r.id
        where r.owner_id = :ownerId
          and r.lifecycle = 'ACTIVE'
          and (:resourceType = '' or r.resource_type = :resourceType)
          and (:query = '' or t.title ilike '%' || :query || '%')
        order by r.updated_at desc
        offset :offset limit :limit
        """)
    Flux<ResourceEntity> search(UUID ownerId, String resourceType, String query, long offset, int limit);

    /**
     * 统计活动资源搜索结果总数。
     *
     * @param ownerId 资源拥有者
     * @param resourceType 类型过滤，空字符串表示不过滤
     * @param query 标题关键词，空字符串表示不过滤
     * @return 匹配资源总数
     */
    @Query("""
        select count(distinct r.id) from resource r
        join resource_title t on t.resource_id = r.id
        where r.owner_id = :ownerId
          and r.lifecycle = 'ACTIVE'
          and (:resourceType = '' or r.resource_type = :resourceType)
          and (:query = '' or t.title ilike '%' || :query || '%')
        """)
    Mono<Long> countSearch(UUID ownerId, String resourceType, String query);
}
