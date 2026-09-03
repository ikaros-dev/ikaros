package run.ikaros.storage;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Attachment 业务引用的数据库访问边界。
 */
public interface AttachmentRepository extends ReactiveCrudRepository<AttachmentEntity, UUID> {

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
