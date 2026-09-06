package run.ikaros.storage;

/** Blob 内容完整性状态。 */
public enum BlobIntegrityStatus {
    UNKNOWN, VERIFIED, MISSING, CORRUPT
}
