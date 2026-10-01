package run.ikaros.resource;

import run.ikaros.resource.api.*;

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
     * 按资源库筛选条件分页查询资源。
     *
     * <p>Collection、标签与来源过滤使用 EXISTS 子查询，避免与标题 join 产生重复行；
     * 比较统一走文本，避免空字符串被 cast 成 uuid 时报错。</p>
     *
     * @param ownerId 资源拥有者
     * @param resourceType 类型过滤，空字符串表示不过滤
     * @param query 标题关键词，空字符串表示不过滤
     * @param lifecycle 生命周期
     * @param collectionId Collection 过滤，空字符串表示不过滤
     * @param tag 标签名精确过滤，空字符串表示不过滤
     * @param sourceProvider 外部身份 provider 过滤，空字符串表示不过滤
     * @param offset 跳过的记录数
     * @param limit 返回的最大记录数
     * @return 当前页资源
     */
    @Query("""
        select distinct r.* from resource r
        join resource_title t on t.resource_id = r.id
        where r.owner_id = :ownerId
          and r.lifecycle = :lifecycle
          and (:resourceType = '' or r.resource_type = :resourceType)
          and (:query = '' or t.title ilike '%' || :query || '%')
          and (:collectionId = '' or exists (
                select 1 from collection_resource cr
                 where cr.resource_id = r.id and cr.collection_id::text = :collectionId))
          and (:tag = '' or exists (
                select 1 from resource_tag rt
                 where rt.resource_id = r.id and rt.owner_id = :ownerId and rt.name = :tag))
          and (:sourceProvider = '' or exists (
                select 1 from external_identity ei
                 where ei.resource_id = r.id
                   and (ei.provider = :sourceProvider or ei.provider like :sourceProvider || ':%')))
        order by r.updated_at desc
        offset :offset limit :limit
        """)
    Flux<ResourceEntity> search(UUID ownerId, String resourceType, String query, String lifecycle,
                                String collectionId, String tag, String sourceProvider, long offset, int limit);

    /**
     * 统计资源库筛选结果总数。
     *
     * @param ownerId 资源拥有者
     * @param resourceType 类型过滤，空字符串表示不过滤
     * @param query 标题关键词，空字符串表示不过滤
     * @param lifecycle 生命周期
     * @param collectionId Collection 过滤，空字符串表示不过滤
     * @param tag 标签名精确过滤，空字符串表示不过滤
     * @param sourceProvider 外部身份 provider 过滤，空字符串表示不过滤
     * @return 匹配资源总数
     */
    @Query("""
        select count(distinct r.id) from resource r
        join resource_title t on t.resource_id = r.id
        where r.owner_id = :ownerId
          and r.lifecycle = :lifecycle
          and (:resourceType = '' or r.resource_type = :resourceType)
          and (:query = '' or t.title ilike '%' || :query || '%')
          and (:collectionId = '' or exists (
                select 1 from collection_resource cr
                 where cr.resource_id = r.id and cr.collection_id::text = :collectionId))
          and (:tag = '' or exists (
                select 1 from resource_tag rt
                 where rt.resource_id = r.id and rt.owner_id = :ownerId and rt.name = :tag))
          and (:sourceProvider = '' or exists (
                select 1 from external_identity ei
                 where ei.resource_id = r.id
                   and (ei.provider = :sourceProvider or ei.provider like :sourceProvider || ':%')))
        """)
    Mono<Long> countSearch(UUID ownerId, String resourceType, String query, String lifecycle,
                           String collectionId, String tag, String sourceProvider);
}
