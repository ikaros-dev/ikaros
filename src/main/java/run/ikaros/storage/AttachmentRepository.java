package run.ikaros.storage;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Attachment 业务引用的数据库访问边界。
 */
public interface AttachmentRepository extends ReactiveCrudRepository<AttachmentEntity, UUID> {

    /**
     * 按 Resource 所属者和可选 Resource 条件分页查询活动附件。
     *
     * @param ownerId 当前拥有者
     * @param resourceId 可选 Resource，null 表示不过滤 Resource
     * @param offset 跳过的记录数
     * @param limit 返回的最大记录数
     * @return 当前页附件
     */
    @Query("""
        select a.* from attachment a
        join resource r on r.id = a.resource_id
        where r.owner_id = :ownerId
          and a.archived_at is null
          and a.deleted_at is null
          and (:resourceId is null or a.resource_id = :resourceId)
        order by a.created_at asc, a.id asc
        offset :offset limit :limit
        """)
    Flux<AttachmentEntity> search(UUID ownerId, UUID resourceId, long offset, int limit);

    /** 统计按 Resource 所属者和可选 Resource 条件匹配的活动附件。 */
    @Query("""
        select count(*) from attachment a
        join resource r on r.id = a.resource_id
        where r.owner_id = :ownerId
          and a.archived_at is null
          and a.deleted_at is null
          and (:resourceId is null or a.resource_id = :resourceId)
        """)
    Mono<Long> countSearch(UUID ownerId, UUID resourceId);

    /**
     * 查询 Resource 的未删除附件。
     *
     * @param resourceId Resource 标识
     * @return 附件列表
     */
    Flux<AttachmentEntity> findAllByResourceIdAndArchivedAtIsNullAndDeletedAtIsNullOrderByCreatedAtAsc(UUID resourceId);

    Mono<AttachmentEntity> findByIdAndResourceIdAndArchivedAtIsNullAndDeletedAtIsNull(UUID id, UUID resourceId);

    Mono<AttachmentEntity> findByResourceIdAndIdempotencyKeyAndArchivedAtIsNullAndDeletedAtIsNull(UUID resourceId,
                                                                                 String idempotencyKey);

    Mono<Long> countByBlobIdAndArchivedAtIsNullAndDeletedAtIsNull(UUID blobId);

    Flux<AttachmentEntity> findAllByBlobIdAndArchivedAtIsNullAndDeletedAtIsNull(UUID blobId);

    Mono<AttachmentEntity> findFirstByBlobIdAndArchivedAtIsNullAndDeletedAtIsNullOrderByCreatedAtAsc(UUID blobId);
}
