package run.ikaros.storage;

/**
 * Blob 副本在一个存储提供者中的状态。
 */
public enum PlacementState {
    ACTIVE,
    VERIFYING,
    UNAVAILABLE,
    DELETING
}
