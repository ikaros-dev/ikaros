package run.ikaros.storage;

import java.util.List;
import java.util.UUID;

/**
 * Blob 的持久化 Placement 规划摘要，用于分层与副本策略判断。
 */
public record StoragePlacementPlanView(
    /** Blob 标识。 */
    UUID blobId,
    /** 本次规划优先考虑的存储层级。 */
    StorageTier preferredTier,
    /** 要求达到的最小 ACTIVE 副本数。 */
    int minimumReplicas,
    /** 当前 ACTIVE Placement 数量。 */
    int activeReplicaCount,
    /** 是否同时满足目标层级和最小副本数。 */
    boolean satisfied,
    /** Blob 当前所有 Placement。 */
    List<PlacementView> placements
) {
}
