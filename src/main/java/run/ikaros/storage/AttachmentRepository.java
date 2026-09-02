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
    Flux<AttachmentEntity> findAllByResourceIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID resourceId);

    Mono<AttachmentEntity> findByIdAndResourceIdAndDeletedAtIsNull(UUID id, UUID resourceId);

    Mono<Long> countByBlobIdAndDeletedAtIsNull(UUID blobId);
}
