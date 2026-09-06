package run.ikaros.storage;

/**
 * Blob 在当前时刻对客户端呈现的可用状态。
 */
public enum BlobAvailability {
    AVAILABLE,
    REMOTE,
    PROCESSING,
    RESTORING,
    MISSING,
    CORRUPTED
}
