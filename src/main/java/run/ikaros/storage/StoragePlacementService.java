package run.ikaros.storage;

import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * 面向策略层提供 Blob 多级存储 Placement 规划能力。
 */
public interface StoragePlacementService {

    /**
     * 检查 Blob 是否满足目标层级与最小副本数要求。
     *
     * @param blobId Blob 标识
     * @param preferredTier 优先存储层级
     * @param minimumReplicas 最小 ACTIVE 副本数
     * @return 当前 Placement 与策略满足情况
     */
    Mono<StoragePlacementPlanView> inspect(UUID blobId, StorageTier preferredTier, int minimumReplicas);
}
