package run.ikaros.storage;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;

/**
 * 默认 Placement 规划实现，只读取状态，不隐式创建、迁移或删除副本。
 */
@Service
public class DefaultStoragePlacementService implements StoragePlacementService {
    private final BlobRepository blobRepository;
    private final BlobPlacementRepository placementRepository;

    /**
     * 创建 Placement 规划服务。
     *
     * @param blobRepository Blob 仓储
     * @param placementRepository Placement 仓储
     */
    public DefaultStoragePlacementService(BlobRepository blobRepository,
                                          BlobPlacementRepository placementRepository) {
        this.blobRepository = blobRepository;
        this.placementRepository = placementRepository;
    }

    @Override
    public Mono<StoragePlacementPlanView> inspect(UUID blobId, StorageTier preferredTier, int minimumReplicas) {
        if (minimumReplicas < 1 || minimumReplicas > 32) {
            return Mono.error(new IllegalArgumentException("最小副本数必须介于 1 和 32 之间"));
        }
        return blobRepository.findById(blobId)
            .switchIfEmpty(Mono.error(new NotFoundException("Blob 不存在")))
            .thenMany(placementRepository.findAllByBlobIdOrderByCreatedAtAsc(blobId))
            .collectList()
            .map(placements -> toPlan(blobId, preferredTier, minimumReplicas, placements));
    }

    private StoragePlacementPlanView toPlan(UUID blobId, StorageTier preferredTier, int minimumReplicas,
                                            List<BlobPlacementEntity> placements) {
        int activeReplicaCount = (int) placements.stream()
            .filter(placement -> placement.placementState() == PlacementState.ACTIVE)
            .count();
        boolean hasPreferredTier = placements.stream()
            .anyMatch(placement -> placement.placementState() == PlacementState.ACTIVE
                && placement.storageTier() == preferredTier);
        List<PlacementView> views = placements.stream()
            .map(placement -> new PlacementView(placement.id(), placement.provider(), placement.storageTier(),
                placement.objectKey(), placement.placementState()))
            .toList();
        return new StoragePlacementPlanView(blobId, preferredTier, minimumReplicas, activeReplicaCount,
            hasPreferredTier && activeReplicaCount >= minimumReplicas, views);
    }
}
