package run.ikaros.storage;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Blob Placement 的数据库访问边界。
 */
public interface BlobPlacementRepository extends ReactiveCrudRepository<BlobPlacementEntity, UUID> {

    /**
     * 查询 Blob 的全部 Placement。
     *
     * @param blobId Blob 标识
     * @return Placement 列表
     */
    Flux<BlobPlacementEntity> findAllByBlobIdOrderByCreatedAtAsc(UUID blobId);

    /**
     * 根据 Provider 与对象键定位物理位置。
     *
     * @param provider 存储提供者标识
     * @param objectKey 提供者内对象键
     * @return 已存在的 Placement，未命中时为空
     */
    Mono<BlobPlacementEntity> findByProviderAndObjectKey(String provider, String objectKey);

    Mono<Void> deleteByBlobId(UUID blobId);

    Mono<Long> countByProviderAndPlacementState(String provider, PlacementState placementState);

    Mono<Long> countByBlobIdAndPlacementState(UUID blobId, PlacementState placementState);
}
