package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;

/**
 * Blob GC 扫描得到的候选摘要；候选只表示满足引用与保留期检查，不代表已经删除。
 */
public record BlobGcCandidateView(
    /** Blob 标识。 */
    UUID blobId,
    /** Blob 的内容摘要。 */
    String sha256,
    /** Blob 的字节大小。 */
    long sizeBytes,
    /** Blob 首次登记时间。 */
    Instant createdAt,
    /** 按本次扫描保留期计算出的最早清理时间。 */
    Instant eligibleAt
) {
}
