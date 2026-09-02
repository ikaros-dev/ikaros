package run.ikaros.storage;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Blob 内容身份的数据库访问边界。
 */
public interface BlobRepository extends ReactiveCrudRepository<BlobEntity, UUID> {

    /**
     * 根据完整 SHA-256 内容摘要查询 Blob。
     *
     * @param sha256 内容摘要
     * @return 已存在的 Blob，未命中时为空
     */
    Mono<BlobEntity> findBySha256(String sha256);

    /**
     * 查询当前无有效 Attachment 引用的 Blob，用于后续受策略保护的 GC。
     *
     * @return 候选 Blob
     */
    @Query("""
        select b.* from blob b
        where not exists (
            select 1 from attachment a where a.blob_id = b.id and a.deleted_at is null
        )
        order by b.created_at asc
        """)
    Flux<BlobEntity> findGarbageCollectionCandidates();
}
